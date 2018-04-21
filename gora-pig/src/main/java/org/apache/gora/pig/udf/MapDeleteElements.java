package org.apache.gora.pig.udf;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;

/**
 * Deletes elements from a map. The first parameter must be a map, and the others
 * can be strings, tuples(with keys), bags(with tuples with keys -the 1st
 * element of the tuple-) and maps(the keys of the map will the ones deleted)
 * 
 * Example:
 * 
 *   MapDeleteElements(map, key1, key2, key3,...)
 *   MapDeleteElements(map, tuple_keys, key1, bag_keys, map_keys ...)
 *
 */
public class MapDeleteElements extends EvalFunc<Map<String,Object>> {

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Object> exec(Tuple input) throws IOException {
    Map<String, Object> resultMap = new HashMap<String, Object>() ;
    
    resultMap.putAll((Map<String,Object>) input.get(0)) ;
    
    for (int i=0 ; i<input.size() ; i++ ) {
      switch (input.getType(i)) {
        case DataType.BAG :
          DataBag databag = (DataBag) input.get(i) ;
          Iterator<Tuple> bagIt = databag.iterator() ;
          while (bagIt.hasNext()) {
            Tuple t = bagIt.next() ;
            resultMap.remove((String)t.get(0)) ;
          }
          break ;
        case DataType.CHARARRAY:
          resultMap.remove((String)input.get(i)) ;
          break ;
        case DataType.TUPLE:
          Tuple t = (Tuple) input.get(i) ;
          for (Object e : t.getAll()) {
            resultMap.remove((String)e) ;
          }
          break ;
        case DataType.MAP:
          Map<String,Object> deleteKeys = (Map<String,Object>) input.get(i) ;
          for (String key: deleteKeys.keySet()) {
            resultMap.remove(key) ;
          }
        case DataType.LONG:
          break ;
        default:
          this.getLogger().warn("Received an unmanaged parameter of type " + DataType.findTypeName(input.getType(i))) ;
      }
    }
    
    return resultMap;
  }

}
