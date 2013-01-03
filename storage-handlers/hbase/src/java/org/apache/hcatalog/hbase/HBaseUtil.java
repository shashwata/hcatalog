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

package org.apache.hcatalog.hbase;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;

//import org.apache.hadoop.hbase.security.User;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hive.hbase.HBaseSerDe;
import org.apache.hadoop.mapred.JobConf;

// [shashwat] for Methods class copied from hbase 0.95
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class HBaseUtil {

    private HBaseUtil(){
    }

    /**
     * Parses the HBase columns mapping to identify the column families, qualifiers
     * and also caches the byte arrays corresponding to them. One of the HCat table
     * columns maps to the HBase row key, by default the first column.
     *
     * @param columnMapping - the column mapping specification to be parsed
     * @param colFamilies - the list of HBase column family names
     * @param colFamiliesBytes - the corresponding byte array
     * @param colQualifiers - the list of HBase column qualifier names
     * @param colQualifiersBytes - the corresponding byte array
     * @return the row key index in the column names list
     * @throws IOException
     */
    static int parseColumnMapping(
        String columnMapping,
        List<String> colFamilies,
        List<byte []> colFamiliesBytes,
        List<String> colQualifiers,
        List<byte []> colQualifiersBytes) throws IOException {

      int rowKeyIndex = -1;

      if (colFamilies == null || colQualifiers == null) {
        throw new IllegalArgumentException("Error: caller must pass in lists for the column families " +
            "and qualifiers.");
      }

      colFamilies.clear();
      colQualifiers.clear();

      if (columnMapping == null) {
        throw new IllegalArgumentException("Error: hbase.columns.mapping missing for this HBase table.");
      }

      if (columnMapping.equals("") || columnMapping.equals(HBaseSerDe.HBASE_KEY_COL)) {
        throw new IllegalArgumentException("Error: hbase.columns.mapping specifies only the HBase table"
            + " row key. A valid Hive-HBase table must specify at least one additional column.");
      }

      String [] mapping = columnMapping.split(",");

      for (int i = 0; i < mapping.length; i++) {
        String elem = mapping[i];
        int idxFirst = elem.indexOf(":");
        int idxLast = elem.lastIndexOf(":");

        if (idxFirst < 0 || !(idxFirst == idxLast)) {
          throw new IllegalArgumentException("Error: the HBase columns mapping contains a badly formed " +
              "column family, column qualifier specification.");
        }

        if (elem.equals(HBaseSerDe.HBASE_KEY_COL)) {
          rowKeyIndex = i;
          colFamilies.add(elem);
          colQualifiers.add(null);
        } else {
          String [] parts = elem.split(":");
          assert(parts.length > 0 && parts.length <= 2);
          colFamilies.add(parts[0]);

          if (parts.length == 2) {
            colQualifiers.add(parts[1]);
          } else {
            colQualifiers.add(null);
          }
        }
      }

      if (rowKeyIndex == -1) {
        colFamilies.add(0, HBaseSerDe.HBASE_KEY_COL);
        colQualifiers.add(0, null);
        rowKeyIndex = 0;
      }

      if (colFamilies.size() != colQualifiers.size()) {
        throw new IOException("Error in parsing the hbase columns mapping.");
      }

      // populate the corresponding byte [] if the client has passed in a non-null list
      if (colFamiliesBytes != null) {
        colFamiliesBytes.clear();

        for (String fam : colFamilies) {
          colFamiliesBytes.add(Bytes.toBytes(fam));
        }
      }

      if (colQualifiersBytes != null) {
        colQualifiersBytes.clear();

        for (String qual : colQualifiers) {
          if (qual == null) {
            colQualifiersBytes.add(null);
          } else {
            colQualifiersBytes.add(Bytes.toBytes(qual));
          }
        }
      }

      if (colFamiliesBytes != null && colQualifiersBytes != null) {
        if (colFamiliesBytes.size() != colQualifiersBytes.size()) {
          throw new IOException("Error in caching the bytes for the hbase column families " +
              "and qualifiers.");
        }
      }

      return rowKeyIndex;
    }

    /**
     * Get delegation token from hbase and add it to JobConf
     * @param job
     * @throws IOException
     */
    static void addHBaseDelegationToken(JobConf job) throws IOException {
        if (isHBaseSecurityEnabled(job)) {
            try {
                obtainAuthTokenForJob(job);
            } catch (InterruptedException e) {
                throw new IOException("Error while obtaining hbase delegation token", e);
            }
        }
    }

    //[shashwat] Auth api is only available from hbase version 0.92 org.apache.hadoop.hbase.security.User.isHBaseSecurityEnabled
    static boolean isHBaseSecurityEnabled(Configuration conf) {
    	//[shashwat] Code copied from hbase trunk (0.95 SNAPSHOT)
    	final String HBASE_SECURITY_CONF_KEY = "hbase.security.authentication";
    	return "kerberos".equalsIgnoreCase(conf.get(HBASE_SECURITY_CONF_KEY)) && "kerberos".equalsIgnoreCase(
			conf.get(CommonConfigurationKeys.HADOOP_SECURITY_AUTHENTICATION));
    }
    
	//[shashwat] copied from hbase 0.95-SNAPSHOT org.apache.hadoop.hbase.security.User.obtainAuthTokenForJob
	static void obtainAuthTokenForJob(JobConf job) throws IOException, InterruptedException {
		try {
			Class c = Class.forName("org.apache.hadoop.hbase.security.token.TokenUtil");
			Methods.call(c, null, "obtainTokenForJob", new Class[]{JobConf.class, UserGroupInformation.class},
					new Object[]{job, UserGroupInformation.getCurrentUser()});
		} catch (ClassNotFoundException cnfe) {
			throw new RuntimeException("Failure loading TokenUtil class, is secure RPC available?", cnfe);
		} catch (IOException ioe) {
			throw ioe;
		} catch (InterruptedException ie) {
			throw ie;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new UndeclaredThrowableException(e, "Unexpected error calling TokenUtil.obtainAndCacheToken()");
		}
	}
	

	//[shashwat] copied from hbase 0.95-SNAPSHOT org.apache.hadoop.hbase.util.Methods
	static class Methods {
	  private static Log LOG = LogFactory.getLog(Methods.class);

	  public static <T> Object call(Class<T> clazz, T instance, String methodName,
	      Class[] types, Object[] args) throws Exception {
	    try {
	      Method m = clazz.getMethod(methodName, types);
	      return m.invoke(instance, args);
	    } catch (IllegalArgumentException arge) {
	      LOG.fatal("Constructed invalid call. class="+clazz.getName()+
	          " method=" + methodName + " types=" + stringify(types), arge);
	      throw arge;
	    } catch (NoSuchMethodException nsme) {
	      throw new IllegalArgumentException(
	          "Can't find method "+methodName+" in "+clazz.getName()+"!", nsme);
	    } catch (InvocationTargetException ite) {
	      // unwrap the underlying exception and rethrow
	      if (ite.getTargetException() != null) {
	        if (ite.getTargetException() instanceof Exception) {
	          throw (Exception)ite.getTargetException();
	        } else if (ite.getTargetException() instanceof Error) {
	          throw (Error)ite.getTargetException();
	        }
	      }
	      throw new UndeclaredThrowableException(ite,
	          "Unknown exception invoking "+clazz.getName()+"."+methodName+"()");
	    } catch (IllegalAccessException iae) {
	      throw new IllegalArgumentException(
	          "Denied access calling "+clazz.getName()+"."+methodName+"()", iae);
	    } catch (SecurityException se) {
	      LOG.fatal("SecurityException calling method. class="+clazz.getName()+
	          " method=" + methodName + " types=" + stringify(types), se);
	      throw se;
	    }
	  }
	  
	//[shashwat] copied from hbase 0.95-SNAPSHOT org.apache.hadoop.hbase.util.Classes.stringify
	  public static String stringify(Class[] classes) {
		    StringBuilder buf = new StringBuilder();
		    if (classes != null) {
		      for (Class c : classes) {
		        if (buf.length() > 0) {
		          buf.append(",");
		        }
		        buf.append(c.getName());
		      }
		    } else {
		      buf.append("NULL");
		    }
		    return buf.toString();
	  	}	  
	}
}
