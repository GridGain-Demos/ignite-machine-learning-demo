/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gridgain.demo.batch;

import static org.gridgain.demo.batch.ConfigPipeLineSettings.*;
import static org.gridgain.demo.batch.DatasetBuilder.*;

import java.io.*;


import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCluster;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Scanner;
import java.util.UUID;
 
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.util.IgniteUtils;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.math.primitives.vector.impl.DenseVector;




public class Step1GenerateTrainingDatasets {
   
	
	public static String csvColumnHeader = null;
	public static FileWriter csvFileWriter = null;

	public static File csvOutputFile = null;
	
	public static String csvfilename = "";
	
	
    public static void main(String[] args) {
    	
    	
  
       
    	
    	if (csvFileInit() > 0)
    	{
    		System.out.println("can't open CSV dataset  file, quitting");
    		System.exit(1);
    	}
    	

    	ConfigPipeLineSettings.statusFileWriteMsg("\n-------------\nStart of Dataset Generation");
    
    	ConfigPipeLineSettings.checkOrSetRowCountSettings();   // set default ROW_COUNT if not specified in Env
    	
    	
    	
        try {
        
        	IgniteConfiguration cfg1 = ConfigNodeClient.getIgniteClientConfiguration("ML");
        	
        	IGNITE_ML_PIPELINE = Ignition.start(cfg1.setIgniteInstanceName("ML Processing"));
        	
        	 

        	IgniteCluster cluster = IGNITE_ML_PIPELINE.cluster();
       
        	// true to write output CSV file
        	
            TRAINING_DATASET_CACHE = DatasetBuilder.generateTrainingDataset(IGNITE_ML_PIPELINE, true);

            ClusterGroup dataCacheNodes = cluster.forCacheNodes(TRAINING_DATASET_CACHE.getName());
            
            ConfigPipeLineSettings.statusFileWriteMsg("dataset generator run, training data cache name: " 
            		+ TRAINING_DATASET_CACHE.getName() + ", total size = " 
            		+ TRAINING_DATASET_CACHE.sizeLong());
            ConfigPipeLineSettings.statusFileWriteMsg(", Cache is on these host nodes: " + dataCacheNodes.hostNames());
            
            csvFileFlushAndClose();   // save dataset.csv file.... {sampled} 

  
        }
        catch (Exception e)
        {
        	System.out.println("Generate Training Dataset exception" + e.getMessage());
        	
        }
        finally {
            System.out.flush();
        }
    }
    
	static int csvFileInit() {
		
		csvfilename = ConfigPipeLineSettings.FILEPREFIX + "dataset.csv";
    	
		String outputDir = "";
		outputDir = System.getenv("OUTPUT_DIR");
    	if (outputDir == null)
    		outputDir = "";
    	
    	String csvfilepath = outputDir + csvfilename;
    	
		int   fail = 0;
		try {
    		csvOutputFile  = new File(csvfilepath);
    		System.out.println("Output file = {" + csvOutputFile.getAbsolutePath() + "}");
    		csvFileWriter = new FileWriter(csvOutputFile, false); // append=false
    	}
    	catch (Exception e) {
    		System.out.println("Error creating CSV  file:" + csvOutputFile.getAbsolutePath() 
    			+ ", exception=" + e.getMessage());
    		fail = 1;
    	}
    
		
		return(fail);
	}
	
	static void writeCSVrecord (String contents)
	{
 	   	
	   	try {
	   		if (csvColumnHeader == null)   // column names for readability
	   		{
	   			 csvColumnHeader = DatasetBuilder.getColumnNames(DatasetBuilder.COL_COUNT);	
	   			 csvFileWriter.write(csvColumnHeader + "\n");
	   		}
	   		csvFileWriter.write(contents + "\n");
	   	}
	   	catch (Exception e) {
	   		System.out.println("can't write to the output file: " + csvOutputFile.getName() +
	   				", exception=" + e.getMessage());
	   	}
	   		
	}
	
	static void csvFileFlushAndClose () {
		
		try {
			csvFileWriter.flush();
			csvFileWriter.close();
		}
		catch (Exception e) {
	   		System.out.println("can't Flush/Close the output file: " + csvOutputFile.getName() +
	   				", exception=" + e.getMessage());
	   	}
		
	}
	
   
}
    
 
