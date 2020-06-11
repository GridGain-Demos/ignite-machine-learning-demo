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


import java.io.Serializable;
import java.util.Random;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.math.primitives.vector.impl.DenseVector;
import org.gridgain.demo.batch.ConfigPipeLineSettings;

import it.unimi.dsi.fastutil.Arrays;

/**
 * 1. Generate a Regression Dataset for training. 
 * 2. create new transactions for prediction and comparison using Model module
 */
public class DatasetBuilder {
    public static final int COL_COUNT = 16;

    public static final int CATEGORICAL_FEATURE_ID = 13;
    
   public static int [] CATEGORICAL_FEATURES = {1,2,3};   // category1, category2, category3 

    public static final int TARGET_COLUMN_INDEX = 15;

    
	public static String COLUMNHEADER = "";
	
	public static String SEPARATOR = ",";
	
	public static String [] defaultColNames = null;
	
   

    private static Random rnd1 = new Random(1L);
    private static Random rnd2 = new Random(2L);
    private static Random rnd3 = new Random(3L);
    private static Random rnd4 = new Random(4L);
    private static Random rnd5 = new Random(5L);
    private static Random rnd6 = new Random(6L);
    private static Random rnd7 = new Random(7L);
    private static Random rnd8 = new Random(8L);
    private static Random rnd9 = new Random(9L);
    private static Random rnd10 = new Random(10L);
    private static Random rnd11 = new Random(11L);
    private static Random rnd12 = new Random(12L);
    private static Random rnd13 = new Random(13L);
    private static Random rnd14 = new Random(14L);
    private static Random rnd15 = new Random(15L);
    private static Random rnd16 = new Random(16L);

    public static IgniteCache<Integer, Vector> generateTrainingDataset(Ignite ignite, boolean csvFile) {
    	
        IgniteCache<Integer, Vector> cache = getTrainingDataCache(ignite);
        cache.clear();

        ConfigPipeLineSettings.checkSettings();  // if not set by caller ENV then set to defaults
        
        double avgRegVal = 0.0;
        double maxRegVal = 0.0;
        double minRegVal = 0.0;
        double currRegVal = 0.0;
        
        String csvline = ""; // for output to CSV file for standalone data generation
        
        ConfigPipeLineSettings.statusFileWriteMsg("\n----\nRegression Dataset generation: CacheName={" 
        		+ ConfigPipeLineSettings.TRAINING_CACHE_NAME  
        		+ "}, Rows # = {" 
        		+ ConfigPipeLineSettings.TRAININGSET_ROW_COUNT 
        		+ "}");

        
        ConfigPipeLineSettings.TRAININGSET_MAX_KEY = ConfigPipeLineSettings.TRAININGSET_ROW_COUNT - 1;
        
        // Get the data streamer reference and stream data.
        try (IgniteDataStreamer<Integer, Vector> vectorizedData = 
        		ignite.dataStreamer(ConfigPipeLineSettings.TRAINING_CACHE_NAME)) {
            vectorizedData.allowOverwrite(true);
            // Stream entries.
            for (int key = 0; key < ConfigPipeLineSettings.TRAININGSET_ROW_COUNT; key++) {
                Serializable[] sourceData = genSourceFeatureValues(key); // generate values for feature fields...
                
          
                if(csvFile == true) {
                // in data gen phase, write features line into dataset csv file 
                	csvline = "";
                	for (int j=0; j < sourceData.length; j++) 
                		csvline += sourceData[j].toString() + (j < sourceData.length-1 ? "," : "");
                	Step1GenerateTrainingDatasets.writeCSVrecord(csvline);
                }
                
                
                // this source data with non-doubles needs to be packed into dense vector format first, prior to preprocessing...
                vectorizedData.addData(key, new DenseVector(sourceData));
                
                // collect some metrics 
                currRegVal = (double)sourceData[TARGET_COLUMN_INDEX];
                avgRegVal += currRegVal;
                maxRegVal =  Double.max(currRegVal, maxRegVal);
                minRegVal = (key==0)? currRegVal : Double.min(currRegVal, minRegVal);
            }
        }

        ConfigPipeLineSettings.statusFileWriteMsg("The Training data cache {" + cache.getName() + "} size is " + cache.size() +
        		", Last row value is: " + cache.get(ConfigPipeLineSettings.TRAININGSET_ROW_COUNT -1).toString());
        ConfigPipeLineSettings.statusFileWriteMsg("Regression (Target) Value [Avg/Max/Min/Mean] is: [" + (avgRegVal/ConfigPipeLineSettings.TRAININGSET_ROW_COUNT) + " / " +
        		maxRegVal + " / " + minRegVal + " / " + (maxRegVal-minRegVal)/2 + "]");
        
        return cache;
    }

    
    public static Serializable []  newTransactionEvent(int key) {  // public method to other modules

              
        Serializable [] newSourceXaction = null;

        try
        {
        	newSourceXaction = genSourceFeatureValues(key); 
        	
        
        }
        catch (Exception e)
        {
        
        	ConfigPipeLineSettings.statusFileWriteMsg("Exception during new transaction event: " + e.getMessage());
        }
        
        ConfigPipeLineSettings.statusFileWriteMsg("New Transaction Event, key =  " + key);
        

        // note that output target value is ignored since this is a generated value
        
        return   newSourceXaction ;
    }

    /**
     * Fills cache with data and returns it.
     *
     * @param ignite Ignite instance.
     * @return Filled Ignite Cache.
     */
    private static IgniteCache<Integer, Vector> getTrainingDataCache(Ignite ignite) {

        CacheConfiguration<Integer, Vector> cacheConfiguration = new CacheConfiguration<>();
        cacheConfiguration.setName(ConfigPipeLineSettings.TRAINING_CACHE_NAME);
        cacheConfiguration.setAffinity(new RendezvousAffinityFunction(false, 10));

        return ignite.getOrCreateCache(cacheConfiguration);
    }
    
    public  static IgniteCache<Integer, Vector> createNewTransactionsCache(Ignite ignite) { // public to modules

        CacheConfiguration<Integer, Vector> cacheConfiguration = new CacheConfiguration<>();
        cacheConfiguration.setName(ConfigPipeLineSettings.XACTIONS_CACHE_NAME);
        cacheConfiguration.setAffinity(new RendezvousAffinityFunction(false, 10));

        ConfigPipeLineSettings.XACTION_CACHE = ignite.getOrCreateCache(cacheConfiguration);
        return ConfigPipeLineSettings.XACTION_CACHE;
        
    }
    
    
    
    public static Serializable[] generateNewXaction(int key)  // public called by prediction to simulate new transaction
    {
     	return genSourceFeatureValues(key);
    }
    
    

    private static Serializable[] genSourceFeatureValues(int key) {
        Serializable[] data = new Serializable[COL_COUNT];
        

        data[0] =  (double)rnd1.nextInt(1000) + 1; //  
        data[1] = rnd2.nextDouble() * 2030; // 
        data[2] = rnd3.nextDouble(); // 
        data[3] = rnd4.nextDouble(); // 
        data[4] = rnd5.nextBoolean() ? 1.0 : 0.0; //  
        data[5] = rnd6.nextDouble(); //  
        data[6] = rnd7.nextDouble(); //  
        data[7] = rnd8.nextDouble() * 495 * 2; //  
        data[8] =  (double)rnd9.nextInt(74) + 1; //  
        data[9] = rnd10.nextDouble() * 0.91; //  
        data[10] = rnd11.nextDouble(); // 
        data[11] =  (double)rnd12.nextInt(24) + 1; //   //  
        data[12] = (double)rnd13.nextInt(100);//  
        data[CATEGORICAL_FEATURE_ID]  = (double) rnd14.nextInt(4);   // category values 1, 2, 3
        data[14] = rnd15.nextDouble();//  
        data[TARGET_COLUMN_INDEX]  = getTargetValueBasedOnFeatures(data); // 

        
        return data;
    }

    private static Serializable getTargetValueBasedOnFeatures(Serializable[] data) {
        double regVal = rnd16.nextDouble() * 1000 * 2;

        if ((double)data[0] < 100.0) regVal += 1000;
        if ((double)data[1] < 10) regVal += 1000;
        if ((double)data[2] > 0.9) regVal += 1000;
        if ((double)data[3] > 0.95) regVal += 10000;
        if ((double)data[4] == 0.0) regVal+=500;
        if ((double)data[5] < 0.001) regVal+= 10000;
        if ((double)data[6] > 0.98) regVal+= 500;
        if ((double)data[7] > 300) regVal+= 500;
        if ((double)data[8] > 73) regVal+= 1000;
        if ((double)data[9] < 0.3) regVal+= 100;
        if ((double)data[10] > 0.1) regVal += 100;
        if ((double)data[11] > 23) regVal += 1000;
        if ((double)data[12]> 99) regVal+= 10000;
        if ((double) data[CATEGORICAL_FEATURE_ID] == 1)  regVal+= 1000;
        if ((double) data[CATEGORICAL_FEATURE_ID] == 2)  regVal+= 100;
        if ((double) data[CATEGORICAL_FEATURE_ID] == 3)  regVal+= 10;
        if ((double)data[14] > 0.8) regVal+= 100;

        return regVal;
    }
    
   
   
    public static String  getColumnNames (Integer columnCount) {
    	
        
    	defaultColNames = new String [columnCount];
    	
    	String csvline = "";
    	
    	String inputstr = "col";  // colNames[j] for default names in generated .CSV file....
    	
    	for (int j=0; j < columnCount; j++)
    	{
			csvline += inputstr  + Integer.toString(j+1) + (j < columnCount-1 ? SEPARATOR : ""); 
			defaultColNames[j] = inputstr + Integer.toString(j);
			
    	}
    	
    	return COLUMNHEADER = csvline;
    	
    }
    

}
