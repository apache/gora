/*
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
package org.apache.gora.pig.udf;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.Tuple;

/**
 * Merge maps, with right parameters overwriting the left ones, and returns a new map.
 * 
 * Example:
 * 
 *   MergeMaps(map1, map2, map3, map4)
 *
 */
public class MapMerge extends EvalFunc<Map<String,Object>> {

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Object> exec(Tuple input) throws IOException {
    Map<String, Object> resultMap = new HashMap<String,Object>() ;
    
    for (int i=0 ; i<input.size() ; i++ ) {
      Map<String,Object> elementMap = (Map<String,Object>) input.get(i) ;
      resultMap.putAll(elementMap) ;
    }
    
    return resultMap;
  }

}
