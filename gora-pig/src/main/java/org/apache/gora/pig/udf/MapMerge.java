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
