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

package org.apache.gora.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

/**
 * An utility class for {@link Writable} related functionality.
 */
public class WritableUtils {
  private WritableUtils() {
    // prevents instantiation
  }
  
  
  public static final void writeProperties(DataOutput out, Properties props) throws IOException {
    MapWritable propsWritable = new MapWritable();
    for (Entry<Object, Object> prop : props.entrySet()) {
      Writable key = new Text(prop.getKey().toString());
      Writable value = new Text(prop.getValue().toString());
      propsWritable.put(key,value);
    }
    propsWritable.write(out);
  }
  
  public static final Properties readProperties(DataInput in) throws IOException {
    Properties props = new Properties();
    MapWritable propsWritable = new MapWritable();
    propsWritable.readFields(in);
    for (Entry<Writable, Writable> prop : propsWritable.entrySet()) {
      String key = prop.getKey().toString();
      String value = prop.getValue().toString();
      props.put(key,value);
    }
    return props;
  }

}
