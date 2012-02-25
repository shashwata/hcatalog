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

package org.apache.hcatalog.utils;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hcatalog.common.HCatConstants;
import org.apache.hcatalog.data.HCatRecord;
import org.apache.hcatalog.mapreduce.HCatInputFormat;
import org.apache.hcatalog.mapreduce.InputJobInfo;

/**
 * This is a map reduce test for testing hcat which goes against the "numbers"
 * table. It performs a group by on the first column and a SUM operation on the
 * other columns. This is to simulate a typical operation in a map reduce program
 * to test that hcat hands the right data to the map reduce program
 * 
 * Usage: hadoop jar sumnumbers <serveruri> <output dir> <-libjars hive-hcat jar>
            The <tab|ctrla> argument controls the output delimiter
            The hcat jar location should be specified as file://<full path to jar>
 */
public class SimpleRead extends Configured implements Tool {

    private static final String TABLE_NAME = "studenttab10k";
    private static final String TAB = "\t";
    
  public static class Map
       extends Mapper<WritableComparable, HCatRecord, Text, IntWritable>{
      
      String name;
      int age;
      double gpa;
      
    @Override
  protected void map(WritableComparable key, HCatRecord value, 
          org.apache.hadoop.mapreduce.Mapper<WritableComparable,HCatRecord,
          Text,IntWritable>.Context context) 
    throws IOException ,InterruptedException {
        name = (String) value.get(0);
System.out.println(name);
        age = (Integer) value.get(1);
        gpa = (Double) value.get(2);
        context.write(new Text(name), new IntWritable(age));

    }
  }
  
   public int run(String[] args) throws Exception {
    Configuration conf = getConf();
    args = new GenericOptionsParser(conf, args).getRemainingArgs();

    String serverUri = args[0];
    String tableName = args[1];
    String outputDir = args[2];
    String dbName = null;
    
    String principalID = System.getProperty(HCatConstants.HCAT_METASTORE_PRINCIPAL);
    if(principalID != null)
    conf.set(HCatConstants.HCAT_METASTORE_PRINCIPAL, principalID);
    Job job = new Job(conf, "SimpleRead");
    HCatInputFormat.setInput(job, InputJobInfo.create(
    		dbName, tableName, null, serverUri, principalID));
    // initialize HCatOutputFormat
    
    job.setInputFormatClass(HCatInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);
    job.setJarByClass(SimpleRead.class);
    job.setMapperClass(Map.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    FileOutputFormat.setOutputPath(job, new Path(outputDir));
    return (job.waitForCompletion(true) ? 0 : 1);
  }
   
   public static void main(String[] args) throws Exception {
       int exitCode = ToolRunner.run(new SimpleRead(), args);
       System.exit(exitCode);
   }
}
