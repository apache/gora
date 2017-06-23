package org.apache.gora.cassandra.test.nativeSerialization;

import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.extras.codecs.MappingCodec;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Sample Class for Custom Codec
 * {@link com.datastax.driver.extras.codecs.MappingCodec}
 */
public class DateAsStringCodec extends MappingCodec<String, Date> {
  public DateAsStringCodec() {
    super(TypeCodec.timestamp(), String.class);
  }

  @Override
  protected Date serialize(String value) {
    try {
      return new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).parse(value);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected String deserialize(Date value) {
    return String.valueOf(value);
  }
}
