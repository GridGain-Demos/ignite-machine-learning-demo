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

package org.gridgain.demo.interactive;

import java.io.*;
import java.util.*;



import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;

import java.text.NumberFormat;
import java.text.ParseException;


import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.math.primitives.vector.impl.DenseVector;





public class ConfigPipeLineSettings {
    /**
     * public static values can also be set directly from other ML pipeline orchestration processes
     * also some of these can be set in Environment Variables or in the config/MLPLProperties.txt file
     */
	
	public static final boolean TEST_MODE = false;   // set to true to see vector assembly from model.predict()
	
	
	public static String PROPERTIESFROMFILE = "";
	public static String FILEPREFIX = "200608-TESTS";
	
	
	static FileWriter statusFileWriter = null;

	static File statusOutputFile = null;
	
	public static IgniteCache<Integer, Vector> TRAINING_DATASET_CACHE = null;   // training dataset
	public static IgniteCache<Integer, Vector> XACTION_CACHE = null;	// "new xactions to be predicted
	
	public static Ignite IGNITE_ML_PIPELINE = null;  // ignite handle used up to training
	public static Ignite IGNITE_TRANSACTIONS = null; // ignite handle used in separate sessions after training 

	public static  String 	TRAINING_CACHE_NAME = "TrainingDataSet";		// set default prior to DatasetGenerator call
	public static 	String 	XACTIONS_CACHE_NAME = "OperationalDataSet"; // net default new transactions 
	public static int 		TRAININGSET_ROW_COUNT = 0; 			// set prior
	public static int 		TRAININGSET_MAX_KEY = 0;  // use for size, to add new xactions during inferencing
	
    public  static float 	DATA_SPLIT = 0.0F; 		// set prior...
	
    public static double MAE_MAX = 800; // used to trigger model update
    
    public static Properties MLPIPELINEPROPS = null;
    
	  public static void main(String args[]) throws IOException {
		  
		  
		 
		  MLPIPELINEPROPS = readPropertiesFile("config/MLPLProperties.txt");
		  
		  PROPERTIESFROMFILE = MLPIPELINEPROPS.toString();
		  
	      System.out.println("Properties File Contents = " + PROPERTIESFROMFILE);
	    
	    
	      
	      if (statusFileWriterInit() > 0)
	      {
	    		System.out.println("can't open Pipeline progress file -- quitting");
	    		throw new EmptyStackException();
	      }
	      
	      statusFileWriteMsg("Properties File contents: " +
  				ConfigPipeLineSettings.PROPERTIESFROMFILE);
	      statusFileWriter.flush();  
  		

	   }
	   public static Properties readPropertiesFile(String fileName) throws IOException {
	      FileInputStream fis = null;
	      Properties prop = null;
	      try {
	         fis = new FileInputStream(fileName);
	         prop = new Properties();
	         prop.load(fis);
	      } 
	      catch(FileNotFoundException fnfe) {
	         fnfe.printStackTrace();
	      } 
	      catch(IOException ioe) {
	         ioe.printStackTrace();
	      } 
	      finally {
	         fis.close();
	      }
	      return prop;
	   }
		
    public static void checkSettings () {
    	
    		checkOrSetRowCountSettings();
    		checkDataSplitSettings();
    }
    
  
   
   public static void checkOrSetRowCountSettings() {
   	
   	int source = 0;
   	String status = "";
   	int  envRows = 0;
   	
   	
   	try {
   		
    	envRows = // Integer.parseInt(System.getenv("ROWS"));
    			Integer.valueOf(MLPIPELINEPROPS.getProperty("ROWS"));
    
   		source = 1;
    	
   		status = "(from Properties File)";
   	}
   	catch(Exception e)
   	{
   		source = 3;
   		status  = "no ROWS property provided";
   	}
   	
   	
      
   	
   	if (source == 1)
   		TRAININGSET_ROW_COUNT = envRows;
   	else if (TRAININGSET_ROW_COUNT != 0)
   	{
   		source = 2;   //  has already been set by parent ML workflow process
   		status = "(using hard coded value)";
   	}	
   	
   	
   	if (source > 2) {
   		TRAININGSET_ROW_COUNT = 1000;    // default; if you set to 40,000 then takes 8 minutes or so
   		statusFileWriteMsg(status + ":  use default ROW_COUNT: " + TRAININGSET_ROW_COUNT);
   	}
   	else
   		statusFileWriteMsg("ROW_COUNT set to: " + TRAININGSET_ROW_COUNT + " " + status);
   	
   }
   
   public static void checkDataSplitSettings() {
   	
   	int source = 0;
   	String exceptionStr = "";
   	Float envdatasplit = null;
   	
   	
   	
   	try {  //  override if provided by Environment variable  
   	
   		envdatasplit = Float.parseFloat(MLPIPELINEPROPS.getProperty("DATA_SPLIT"));
   		source = 1;

   	}
   	catch(Exception e)
   	{
   		source = 3;
   		exceptionStr = e.getMessage();
   	}
   	
   	
   	if (source == 1)
   		DATA_SPLIT = envdatasplit;
   	else if (DATA_SPLIT != 0.0F)
   		source = 2;   //  has already been set by parent ML workflow process
   		
   		
   	if (DATA_SPLIT > 1.0F || DATA_SPLIT < 0.0F) {
   		DATA_SPLIT = 0.75f;
   		statusFileWriteMsg("DATA_SPLIT provided out of range, so use default: " + DATA_SPLIT);
   	}
   			
   	
   	if (source > 2)
   	{
   		DATA_SPLIT = 0.75f;
   		statusFileWriteMsg("DATA_SPLIT Property not provided; default used, ");
   	}
   	
	statusFileWriteMsg("DATA_SPLIT set to: " + DATA_SPLIT);

   }
   

public	static int statusFileWriterInit() {
		
    	
	
		
		String outputDir = "";
		outputDir = 
				MLPIPELINEPROPS.getProperty("OUTPUT_DIR").toString();
		
		String fileprefix = MLPIPELINEPROPS.getProperty("FILEPREFIX").toString();
		
		
		if (!fileprefix.isBlank())  // if not blank set FILEPREFIX to this field, otherwise leave FILEPREFIX as is
			FILEPREFIX = fileprefix + "-";

    	if (outputDir == null)    // not set if running in test mode
    		outputDir = "";
    	
    	String filepath =  outputDir + FILEPREFIX + "interactive-status.txt";   // default name with timestamp
    	
		int   fail = 0;
		try {
    		statusOutputFile  = new File(filepath);
    		System.out.println("Output file = {" + statusOutputFile.getAbsolutePath() + "}");
    		statusFileWriter = new FileWriter(statusOutputFile, true);
    		
    		
    		
    	}
    	catch (Exception e) {
    		System.out.println("Error creating file:" + statusOutputFile.getAbsolutePath() 
    			+ ", exception=" + e.getMessage());
    		fail = 1;
    	}
    
		
		return(fail);
	}
	
	public static void statusFileWriteMsg (String contents)
	{
		System.out.println(contents);
	   	
	   	try {
	   		statusFileWriter.write("\n" + contents );
	   	}
	   	catch (Exception e) {
	   		System.out.println("can't write to the output file: " + statusOutputFile.getName() +
	   				", exception=" + e.getMessage());
	   	}
	   		
	}
	
	public static void statusFileWriterFlush () {
		
		try {
			statusFileWriter.flush();
		}
		catch (Exception e) {
	   		System.out.println("can't Flush buffer in the output file: " + statusOutputFile.getName() +
	   				", exception=" + e.getMessage());
	   	}
		
	}

	
	public static void statusFileWriterFlushAndClose () {
		
		try {
			statusFileWriter.flush();
			statusFileWriter.close();
		}
		catch (Exception e) {
	   		System.out.println("can't Flush/Close the output file: " + statusOutputFile.getName() +
	   				", exception=" + e.getMessage());
	   	}
		
	}

	static boolean trainingdone = false;
	
	 static public void setPredictiveModelReady()
	{
		trainingdone = true;
	}
	
	 static public boolean predictiveModelIsReady()
	{
		return trainingdone;
	}
   
	 public static int  checkTestServerFlag(String [] args) {
	    	
	    	int success = 0;
	    	
	    	String testServerNode = ConfigPipeLineSettings.MLPIPELINEPROPS.getProperty("TEST_SERVER");
			if (testServerNode.startsWith("1")) {
				Step0RunTestCacheNode.main(args);
				testServerNode += " as value for TEST_SERVER; starting one test Cache server node";
			}
			else
				testServerNode += "..No value for TEST_SERVER provided; so assuming existing Cache server cluster provided";
				
			ConfigPipeLineSettings.statusFileWriteMsg(testServerNode);
	    	
			return success;
	    }

    
}
