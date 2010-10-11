
package org.gora.mapreduce;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.gora.util.StringUtils;

/**
 * MapReduce related utilities for Gora
 */
public class GoraMapReduceUtils {

  public static class HelperInputFormat<K,V> extends FileInputFormat<K, V> {
    @Override
    public RecordReader<K, V> createRecordReader(InputSplit arg0,
        TaskAttemptContext arg1) throws IOException, InterruptedException {
      return null;
    }
  }
  
  public static void setIOSerializations(Configuration conf, boolean reuseObjects) {
    String serializationClass =
      PersistentSerialization.class.getCanonicalName();
    if (!reuseObjects) {
      serializationClass =
        PersistentNonReusingSerialization.class.getCanonicalName();
    }
    String[] serializations = StringUtils.joinStringArrays(
        conf.getStrings("io.serializations"), 
        "org.apache.hadoop.io.serializer.WritableSerialization",
        StringSerialization.class.getCanonicalName(),
        serializationClass); 
    conf.setStrings("io.serializations", serializations);
  }  
  
  public static List<InputSplit> getSplits(Configuration conf, String inputPath) 
    throws IOException {
    JobContext context = createJobContext(conf, inputPath);
    
    HelperInputFormat<?,?> inputFormat = new HelperInputFormat<Object,Object>();
    return inputFormat.getSplits(context);
  }
  
  public static JobContext createJobContext(Configuration conf, String inputPath) 
    throws IOException {
    
    if(inputPath != null) {
      Job job = new Job(conf);
      FileInputFormat.addInputPath(job, new Path(inputPath));
      return new JobContext(job.getConfiguration(), null);
    } 
    
    return new JobContext(conf, null);
  }
}
