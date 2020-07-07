import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class InvertedIndex {
    // final static String INPUT_PATH = "hdfs://hadoop0:9000/index_in";
    // final static String OUTPUT_PATH = "hdfs://hadoop0:9000/index_out";

    public static class InvertedIndexMapper extends 
        Mapper<Object,Text,Object,Text>{
        private Text keyInfo = new Text();//store the combination of word and URI
        private  Text valueInfo = new Text();//store the word frequency
        private FileSplit split;//store the split target
        
        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            //there are 3 input files：1.txt("MapReduce is Simple")
            //2.txt("MapReduce is powerful is Simple")
            //3.txt("Hello MapReduce bye MapReduce")
          
            split = (FileSplit)context.getInputSplit();
             //get the FileSplit tatget of <key,value>
             
             //"value" saves one line of message in the txt file
             //"key" is the offset of the initial and the head address of txt file
             
            StringTokenizer itr = new StringTokenizer(value.toString());
             //StringTokenizer splits each row into words combination and output <word, 1> as the result of mapping
          
            while(itr.hasMoreTokens()){
                //key is the combination of words and URI
                keyInfo.set(itr.nextToken()+":"+split.getPath().toString());
                valueInfo.set("1");
                context.write(keyInfo, valueInfo);
                 //output：<key,value>---<"MapReduce:1.txt",1>
            }
        }
    }

    public static class InvertedIndexCombiner 
        extends Reducer<Text, Text, Text, Text>{
        private Text info = new Text();
        
        protected void reduce(Text key, Iterable<Text> values,Context context)
                throws IOException, InterruptedException {
            //input：<key,value>---<"MapReduce:1.txt",list(1,1,1,1)>
            //key="MapReduce:1.txt",value=list(1,1,1,1);
            int sum = 0;
            for(Text value : values){
                sum += Integer.parseInt(value.toString());
            }
            
            int splitIndex = key.toString().indexOf(":");
            info.set(key.toString().substring(splitIndex+1)+":"+sum);
            key.set(key.toString().substring(0,splitIndex));
            context.write(key, info);
             //output:<key,value>----<"Mapreduce","0.txt:2">
        }   
    }

    public static class InvertedIndexReducer 
        extends Reducer<Text, Text, Text, Text>{
        
        private Text result = new Text();
        
        protected void reduce(Text key, Iterable<Text> values,Context context)
                throws IOException, InterruptedException {
            //input：<"MapReduce",list("0.txt:1","1.txt:1","2.txt:1")>
            //output：<"MapReduce","0.txt:1,1.txt:1,2.txt:1">
            String fileList = new String();
            for(Text value : values){//value="0.txt:1"
                fileList += value.toString()+";";
            }
            result.set(fileList);
            context.write(key, result);
             //output：<"MapReduce","0.txt:1,1.txt:1,2.txt:1">
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
          System.err.println("Usage: InvertedIndex <in> <out>");
          System.exit(2);
        }
        Job job = new Job(conf, "InvertedIndex");
        job.setJarByClass(InvertedIndex.class);
        //use InvertedIndexMapper class to finish Mapping
        job.setMapperClass(InvertedIndexMapper.class);
        //ues InvertedIndexCombiner class to finish Combination
        job.setCombinerClass(InvertedIndexCombiner.class);
        //use InvertedIndexReducer class to finish Reduction
        job.setReducerClass(InvertedIndexReducer.class);
        //set the output format of Mapping and Reduction, key's is Text
        job.setOutputKeyClass(Text.class);
        //set the input format of Mapping and Reduction, value's is Text；
        job.setOutputValueClass(Text.class);
        //set the input address of mission data
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        //set the storage address of mission data
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        //call job.waitForCompletion(true) to finish the misson
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}