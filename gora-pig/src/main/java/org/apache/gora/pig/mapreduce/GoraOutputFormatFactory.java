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
package org.apache.gora.pig.mapreduce;

import org.apache.gora.mapreduce.GoraOutputFormat;
import org.apache.gora.persistency.Persistent;
import org.apache.gora.util.ClassLoadingUtils;

/**
 * Class with static methods to create instances of GoraInputFormat with
 * variable generics parameters.
 * 
 */
public class GoraOutputFormatFactory {

  /**
   * Creates an instance of GoraInputFormat from two Class types (key, value)
   * 
   * @param keyClass Key class 
   * @param valueClass Subclass of Persistent for values
   * @return A new GoraOutputFormat instance
   */
  public static <K, V extends Persistent>
  GoraOutputFormat<K,V> createInstance(Class<K> keyClass, Class<V> valueClass) {
    return new GoraOutputFormat<K,V>() ;
  }
  
  /**
   * Creates an instance of a subclass of GoraOutputFormat from the three
   * classes corresponding to the subclass, key and value.
   * 
   * @param GoraOutputFormatSubclass GoraOutputFormat subclass
   * @param keyClass Key class
   * @param valueClass Subclass of Persistent for values
   * @return A new instance subclass of GoraOutputFormat
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  public static <R extends GoraOutputFormat<K,V>, K, V extends Persistent>
  R createInstance(Class<R> GoraOutputFormatSubclass,
                   Class<K> keyClass,
                   Class<V> valueClass) throws InstantiationException,
                                               IllegalAccessException {
    return GoraOutputFormatSubclass.newInstance() ;
  }

  /**
   * Creates and instance of GoraOutputFormat from class names for key and
   * persistent value.
   * 
   * @param keyName Key class name
   * @param persistentName Subclass of Persistent class name
   * @return A new instance of GoraOutputFormat
   * @throws ClassNotFoundException
   * @throws ClassClastException When 'persistentName' parameter is not actually
   *         subclass of Persistent 
   */
  public static GoraOutputFormat<?,? extends Persistent> createInstance (
                                                String keyName,
                                                String persistentName)
                                                throws ClassNotFoundException,
                                                       ClassCastException {
    Class<?> keyClass = ClassLoadingUtils.loadClass(keyName) ;
    @SuppressWarnings("unchecked")
    Class<? extends Persistent> valueClass = (Class<? extends Persistent>)ClassLoadingUtils.loadClass(persistentName) ;
    if (!Persistent.class.isAssignableFrom(valueClass)) {
      throw new ClassCastException("Error casting from "+ persistentName + " to Persistent") ;
    }
    return createInstance(keyClass, valueClass) ;
  }
  
}