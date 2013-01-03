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

package org.apache.hcatalog.hbase.snapshot;

import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;

public class RevisionManagerConfiguration {



  public static Configuration addResources(Configuration conf) {
    conf.addDefaultResource("revision-manager-default.xml");
    conf.addResource("revision-manager-site.xml");
    return conf;
  }

  /**
   * Creates a Configuration with Revision Manager resources
   * @return a Configuration with Revision Manager resources
   */
  public static Configuration create() {
    Configuration conf = new Configuration();
    return addResources(conf);
  }

  /**
   * Creates a clone of passed configuration.
   * @param that Configuration to clone.
   * @return a Configuration created with the revision-manager-*.xml files plus
   * the given configuration.
   */
  public static Configuration create(final Configuration that) {
    Configuration conf = create();
    //we need to merge things instead of doing new Configuration(that)
    //because of a bug in Configuration wherein the config
    //set on the MR fronted will get loaded on the backend as resouce called job.xml
    //hence adding resources on the backed could potentially overwrite properties
    //set on the frontend which we shouldn't be doing here
    
    //[shashwat] see @{link #merge()}
    // HBaseConfiguration.merge(conf, that);
    merge(conf, that);
    return conf;
  }
  
  //[shashwat] copied from hbase 0.92 org.apache.hadoop.hbase.HBaseConfiguration.java
  static void merge(Configuration destConf, Configuration srcConf) {
	for (Entry<String, String> e : srcConf) {
		destConf.set(e.getKey(), e.getValue());
		}
	}
}
