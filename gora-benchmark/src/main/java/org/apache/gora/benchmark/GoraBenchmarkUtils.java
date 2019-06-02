package org.apache.gora.benchmark;
import java.util.HashMap;

import org.fluttercode.datafactory.impl.DataFactory;

import com.yahoo.ycsb.ByteIterator;

import generated.Person;

public class GoraBenchmarkUtils {
	
	private static DataFactory df;
	
	public static Person createPerson(String key) {
		df = new DataFactory();
		
		
		Person person = new Person();
		
		person.setUserId(key);
		person.setFirstName(df.getFirstName());
		person.setLastName(df.getLastName());
		person.setBirthDate(df.getNumberBetween(1990, 2015));
		person.setCompany(df.getBusinessName());
		person.setCity(df.getCity());
		person.setAddress(df.getAddress());
		person.setEmail(df.getEmailAddress());
		person.setStreetname(df.getStreetName());
		person.setStreetsuffix(df.getStreetSuffix());
		person.setPersonalnumber(df.getNumberBetween(0, Integer.MAX_VALUE));
		
		return person;
	}
	
	public static boolean isFieldUpdatable(String field, HashMap<String, ByteIterator> values) {
	  if(values.get(field) == null) {
	    return false;
	  }
	  return true;
	}

}
