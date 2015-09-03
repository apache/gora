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
package org.apache.gora.compiler.utils;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Utility class which specifies a collection of license headers which can be 
 * used within the GoraCompiler for generating alternative license headers for
 * Java interfaces and classes generated from protocols and schemas. 
 */
public class LicenseHeaders {

  private static final Logger LOG = LoggerFactory.getLogger(LicenseHeaders.class);

  /**
   * Chosen license to be included within the generated classes
   */
  private String licenseName;
  
  /**
   * Licenses supported by Gora Compilers
   */
  private static final String supportedLicenses[] = {"ASLv2", "AGPLv3", "CDDLv1", "FDLv13", "GPLv1", "GPLv2", "GPLv3", "LGPLv21", "LGPLv3"}; 
  
  /**
   * HashMap containing supported licenses' names and their corresponding text.
   */
  private HashMap<String, String> relatedLicenses;

  // ASLv2 license header
  @SuppressWarnings("unused")
  private static final String ASLv2 = 
    "/**\n" +
    " *Licensed to the Apache Software Foundation (ASF) under one\n" +
    " *or more contributor license agreements.  See the NOTICE file\n" +
    " *distributed with this work for additional information\n" +
    " *regarding copyright ownership.  The ASF licenses this file\n" +
    " *to you under the Apache License, Version 2.0 (the\"\n" +
    " *License\"); you may not use this file except in compliance\n" +
    " *with the License.  You may obtain a copy of the License at\n" +
    " *\n " +
    " * http://www.apache.org/licenses/LICENSE-2.0\n" +
    " * \n" +
    " *Unless required by applicable law or agreed to in writing, software\n" +
    " *distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
    " *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
    " *See the License for the specific language governing permissions and\n" +
    " *limitations under the License.\n" +
    " */\n";
  
  // AGPLv3 license header
  @SuppressWarnings("unused")
  private static final String AGPLv3 =
    "/**\n" +
    " * This program is free software: you can redistribute it and/or modify\n" +
    " * it under the terms of the GNU Affero General Public License as published by\n" +
    " * the Free Software Foundation, either version 3 of the License, or\n" +
    " * (at your option) any later version.\n" +
    " *\n " +
    " * This program is distributed in the hope that it will be useful,\n" +
    " * but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
    " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
    " * GNU General Public License for more details.\n" +
    " */\n";
    
  // CDDLv1 license header
  @SuppressWarnings("unused")
  private static final String CDDLv1 =
    "/**\n" +
    " * COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0\n" +
    " *\n " +
    " * This program is free software: you can redistribute it and/or modify\n" +
    " * it under the terms of the Common Development and Distrubtion License as\n" +
    " * published by the Sun Microsystems, either version 1.0 of the\n" +
    " * License, or (at your option) any later version.\n" +
    " *\n " +
    " * This program is distributed in the hope that it will be useful,\n" +
    " * but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
    " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
    " * GNU General Lesser Public License for more details.\n" +
    " *\n " +
    " * You should have received a copy of the Common Development and Distrubtion\n" +
    " * License along with this program.  If not, see\n" +
    " * <http://www.gnu.org/licenses/gpl-1.0.html>.\n" +
    " */\n";
    
  // FDLv1.3 license header
  @SuppressWarnings("unused")
  private static final String FDLv13 =
    "/**\n" +
    " * This program is free software: you can redistribute it and/or modify\n" +
    " * it under the terms of the GNU Free Documentation License as published by\n" +
    " * the Free Software Foundation, either version 1.3 of the License, or\n" +
    " * (at your option) any later version.\n" +
    " *\n " +
    " * This program is distributed in the hope that it will be useful,\n" +
    " * but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
    " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
    " * GNU General Public License for more details.\n" +
    " *\n " +
    " * You should have received a copy of the GNU Free Documentation License\n" +
    " * along with this program.  If not, see <http://www.gnu.org/licenses/>.\n" +
    " */\n";

  // GPLv1 license header
  @SuppressWarnings("unused")
  private static final String GPLv1 =
    "/**\n" +
    " * This program is free software: you can redistribute it and/or modify\n" +
    " * it under the terms of the GNU General Public License as\n" +
    " * published by the Free Software Foundation, either version 1 of the\n" +
    " * License, or (at your option) any later version.\n" +
    " *\n " +
    " * This program is distributed in the hope that it will be useful,\n" +
    " * but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
    " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
    " * GNU General Public License for more details.\n" +
    " *\n " +
    " * You should have received a copy of the GNU General Public\n" +
    " * License along with this program.  If not, see\n" +
    " * <http://www.gnu.org/licenses/gpl-1.0.html>.\n" +
    " */\n";

  // GPLv2 license header
  @SuppressWarnings("unused")
  private static final String GPLv2 =
    "/**\n" +
    " * This program is free software: you can redistribute it and/or modify\n" +
    " * it under the terms of the GNU General Public License as\n" +
    " * published by the Free Software Foundation, either version 2 of the\n" + 
    " * License, or (at your option) any later version.\n" +
    " *\n " +
    " * This program is distributed in the hope that it will be useful,\n" +
    " * but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
    " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
    " * GNU General Public License for more details.\n" +
    " *\n " +
    " * You should have received a copy of the GNU General Public\n" + 
    " * License along with this program.  If not, see\n" +
    " * <http://www.gnu.org/licenses/gpl-2.0.html>.\n" +
    " */\n";    

  // GPLv3 license header
  @SuppressWarnings("unused")
  private static final String GPLv3 =
    "/**\n" +
    " * This program is free software: you can redistribute it and/or modify\n" +
    " * it under the terms of the GNU General Public License as\n" +
    " * published by the Free Software Foundation, either version 3 of the\n" + 
    " * License, or (at your option) any later version.\n" +
    " *\n " +
    " * This program is distributed in the hope that it will be useful,\n" +
    " * but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
    " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
    " * GNU General Public License for more details.\n" +
    " *\n " +
    " * You should have received a copy of the GNU General Public\n" +
    " * License along with this program.  If not, see\n" +
    " * <http://www.gnu.org/licenses/gpl-3.0.html>.\n" +
    " */\n";  

  // LGPLv21 license header
  @SuppressWarnings("unused")
  private static final String LGPLv21 =
    "/**\n" +
    " * This program is free software: you can redistribute it and/or modify\n" +
    " * it under the terms of the GNU Lesser General Public License as\n" +
    " * published by the Free Software Foundation, either version 2.1 of the\n" + 
    " * License, or (at your option) any later version.\n" +
    " *\n " +
    " * This program is distributed in the hope that it will be useful,\n" +
    " * but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
    " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
    " * GNU General Public License for more details.\n" +
    " *\n " +
    " * You should have received a copy of the GNU Lesser General Public\n" + 
    " * License along with this program.  If not, see\n" +
    " * <http://www.gnu.org/licenses/lgpl-2.1.html>.\n" +
    " */\n";  

  // LGPLv3 license header
  @SuppressWarnings("unused")
  private static final String LGPLv3 =
    "/**\n" +
    " * This program is free software: you can redistribute it and/or modify\n" +
    " * it under the terms of the GNU Lesser General Public License as\n" +
    " * published by the Free Software Foundation, either version 3 of the\n" +
    " * License, or (at your option) any later version.\n" +
    " *\n " +
    " * This program is distributed in the hope that it will be useful,\n" +
    " * but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
    " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
    " * GNU General Public License for more details.\n" +
    " *\n " +
    " * You should have received a copy of the GNU Lesser General Public\n" + 
    " * License along with this program.  If not, see\n" +
    " * <http://www.gnu.org/licenses/lgpl-3.0.html>.\n" +
    " */\n"; 
  
  /**
   * @param license 
   */
  public LicenseHeaders(String pLicenseName) {
    this.initializeRelations();
    this.setLicenseName(pLicenseName);
  }

  /**
   * Initializes relations between supported licenses and license text
   */
  public void initializeRelations(){
    relatedLicenses = new HashMap<String, String>();
    try {
        for (String licenseValue : supportedLicenses) {
          String var = (String) this.getClass().getDeclaredField(licenseValue).get(licenseValue);
          relatedLicenses.put(licenseValue,var);
        }
    } catch (SecurityException e) {
      LOG.error(e.getMessage());
      throw new RuntimeException(e);
    } catch (NoSuchFieldException e) {
      LOG.error(e.getMessage());
      throw new RuntimeException(e);
    } catch (IllegalArgumentException e) {
      LOG.error(e.getMessage());
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      LOG.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   *Set the license header for the LicenseHeader object.
   *
   */  
  public void setLicenseName(String pLicenseName) {
    this.licenseName = pLicenseName;
  }
  
  /**
   * Get the license header for the LicenseHeader object.
   * @return
   */
  public String getLicense() {
    return relatedLicenses.get(licenseName)!=null?relatedLicenses.get(licenseName):"";
  }
  
  /**
   * Get the license name for the LicenseHeader object.
   * @return
   */
  public String getLicenseName(){
    return licenseName;
  }
}
