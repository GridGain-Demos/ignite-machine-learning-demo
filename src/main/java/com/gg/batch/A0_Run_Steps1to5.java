/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gg.batch;

import java.io.*;

import com.gg.batch.ConfigPipeLineSettings.*;

/**
 * 
 */
public class A0_Run_Steps1to5 {
    /** Run examples with default settings. */
    public static void main(String[] args) {
    	
    	if (doPipeLindConfigSettings(args) == 1)
    		System.exit(1);
    	
    	// don't start server in step0, assume cache server cluster already running
    	// checkTestServerFlag(args); // check to see if need a local test server or not
    	  	
    	long startJob = System.currentTimeMillis();
    	
    	// synthetic data generator to train the model 
    	// ConfigPipeLineSettings.TRAINING_CACHE_NAME = "ClaimsTrainingDataset";
    	// ConfigPipeLineSettings.TRAININGSET_ROW_COUNT = 1200;
    	
    	boolean useGeneratedData = true; // use either synthetic data or actual CSV file
        
    	if(useGeneratedData)
    		Step1GenerateTrainingDatasets.main(args);  // data generator
    	else 
    		; //	Step1LoadTrainingDatasetFromCSV.main(args); // not implemented yet
        
        Step2PreProcess.main(args);

        // perform training on the synthetic data
        ConfigPipeLineSettings.DATA_SPLIT = 0.79F;   // set the split value
        Step3TrainRF.main(args);
        
        // perform  prediction using simulated new data + the model created
        Step4Predict.BATCH_SIZE = 14;
        
        Step4Predict.main(args);
            
           
            
            // retraining when data / concept drift detected in accuracy
       ConfigPipeLineSettings.MAE_MAX = 600;
            
       Step5MeasureAndReTrain.main(args);
            
            
       long endJob = System.currentTimeMillis();
            
       ConfigPipeLineSettings.statusFileWriteMsg("Job Completed, elapsed time is " 
            		+ (endJob - startJob) + " milliseconds");

       
        
        ConfigPipeLineSettings.statusFileWriterFlushAndClose();
       
    }
    
    private static int  doPipeLindConfigSettings(String [] args) {
    	int success = 0;
    	try {
    		ConfigPipeLineSettings.main(args);
    		
    	}
    	catch(Exception e) {
    		System.out.println("Config properties file can't be processed");
    		success = 1;
    	}	
		return success;
    }

    private static int  checkTestServerFlag(String [] args) {
    	
    	int success = 0;
    	
    	String testServerNode = ConfigPipeLineSettings.MLPIPELINEPROPS.getProperty("TEST_SERVER");
		if (testServerNode.startsWith("1")) {
			Step0RunTestCacheNode.main(args);
			testServerNode += " as value for TEST_SERVER; starting one test server node";
		}
		else
			testServerNode += "..No value for TEST_SERVER provided; no test server started";
			
		ConfigPipeLineSettings.statusFileWriteMsg(testServerNode);
    	
		return success;
    }


}


