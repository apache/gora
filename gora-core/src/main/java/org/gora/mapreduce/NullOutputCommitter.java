package org.gora.mapreduce;

import java.io.IOException;

import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * An OutputCommitter that does nothing.
 */
public class NullOutputCommitter extends OutputCommitter {

  @Override
  public void abortTask(TaskAttemptContext arg0) throws IOException {
  }

  @Override
  public void cleanupJob(JobContext arg0) throws IOException {
  }

  @Override
  public void commitTask(TaskAttemptContext arg0) throws IOException {
  }

  @Override
  public boolean needsTaskCommit(TaskAttemptContext arg0) throws IOException {
    return false;
  }

  @Override
  public void setupJob(JobContext arg0) throws IOException {
  }

  @Override
  public void setupTask(TaskAttemptContext arg0) throws IOException {
  }
}
