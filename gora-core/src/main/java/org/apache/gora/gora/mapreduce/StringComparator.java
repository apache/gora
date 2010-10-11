package org.gora.mapreduce;

import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.Text;

public class StringComparator implements RawComparator<String> {

  @Override
  public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
    return Text.Comparator.compareBytes(b1, s1, l1, b2, s2, l2);
  }

  @Override
  public int compare(String o1, String o2) {
    return o1.compareTo(o2);
  }

}
