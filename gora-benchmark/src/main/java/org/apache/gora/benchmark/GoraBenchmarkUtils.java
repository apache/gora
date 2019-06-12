package org.apache.gora.benchmark;
import java.util.HashMap;

import org.fluttercode.datafactory.impl.DataFactory;

import com.yahoo.ycsb.ByteIterator;

import generated.Person;

public class GoraBenchmarkUtils {
	
	public static boolean isFieldUpdatable(String field, HashMap<String, ByteIterator> values) {
	  if(values.get(field) == null) {
	    return false;
	  }
	  return true;
	}

}
