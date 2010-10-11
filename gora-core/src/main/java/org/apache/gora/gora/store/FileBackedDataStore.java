
package org.gora.store;

import java.io.InputStream;
import java.io.OutputStream;

import org.gora.persistency.Persistent;

/** FileBackedDataStore supplies necessary interfaces to set input 
 * and output paths for data stored which are file based.   
 */
public interface FileBackedDataStore<K, T extends Persistent> extends DataStore<K, T> {

  public void setInputPath(String inputPath);
  
  public void setOutputPath(String outputPath);
  
  public String getInputPath();
  
  public String getOutputPath();
  
  public void setInputStream(InputStream inputStream);
  
  public void setOutputStream(OutputStream outputStream);

  public InputStream getInputStream();
  
  public OutputStream getOutputStream();
  
}
