/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.avro.mapreduce;

import java.io.Closeable;
import java.io.IOException;

import org.apache.avro.file.SeekableInput;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;

/*
 * Copied from Avro trunk, when 1.4 is released and Gora switches to it
 * remove this file
 */

/** Adapt an {@link FSDataInputStream} to {@link SeekableInput}. */
public class FsInput implements Closeable, SeekableInput {
  private final FSDataInputStream stream;
  private final long len;

  /** 
   * Construct given a path and a configuration.
   * @param path
   *      a path on HDFS to construct
   * @param conf
   *      a Hadoop {@link org.apache.hadoop.conf.Configuration} object
   * @throws IOException
   *      if there is an error opening path, or obtaining a file status
   *      from the file system
   */
  public FsInput(Path path, Configuration conf) throws IOException {
    this.stream = path.getFileSystem(conf).open(path);
    this.len = path.getFileSystem(conf).getFileStatus(path).getLen();
  }

  public long length() {
    return len;
  }

  public int read(byte[] b, int off, int len) throws IOException {
    return stream.read(b, off, len);
  }

  public void seek(long p) throws IOException {
    stream.seek(p);
  }

  public long tell() throws IOException {
    return stream.getPos();
  }

  public void close() throws IOException {
    stream.close();
  }
}
