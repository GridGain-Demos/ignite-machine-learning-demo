package com.gg.interactive;


import java.util.Scanner;  // Import the Scanner class


import com.gg.interactive.sg.DataGeneratorInterface;
import com.gg.interactive.sg.DatasetBuilder;
import com.gg.interactive.sg.RFModelInterface;

import java.io.*;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.HashMap;
import java.util.Map;

import javax.cache.Cache;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCluster;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;

import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.configuration.IgniteConfiguration;


import java.io.FileNotFoundException;
import java.io.Serializable;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Scanner;
import java.util.UUID;
 
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.util.IgniteUtils;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.math.primitives.vector.impl.DenseVector;




import org.apache.ignite.ml.dataset.feature.extractor.Vectorizer;
import org.apache.ignite.ml.dataset.feature.extractor.impl.DummyVectorizer;
import org.apache.ignite.ml.math.primitives.vector.VectorUtils;

public class A1_Perform_Predictions {
	
	public static int BATCH_SIZE = 0;
	public static 	Map<Integer, Double> PREDICTIONS = new HashMap<>();
	public static 	Map<Integer, Double> ACTUALS = new HashMap<>();

	public static double MSE = 0;
	public static double MAE = 0;
	
   	static DataGeneratorInterface dgService = null; // to get simulated data from OLTP cache s 
	static RFModelInterface mdlService = null;  // model prediction API

	static String oltpCachename = "";
	static IgniteCache<Integer, Vector> xactioncache  = null;
	static int onlineCacheSize = -1;
	
			// simulate new xactions to be predicted  

	public static void main(String[] args) {
 

    	
        try { 
        	
    		ConfigPipeLineSettings.main(args);
        	ConfigPipeLineSettings.statusFileWriteMsg("Startup process to create Predictions with live transactions: " + java.time.LocalTime.now());


        	IgniteConfiguration cfg2 = ConfigNodeClient.getIgniteClientConfiguration("Do predictions");
        	
        	Ignite ignite = Ignition.start(cfg2.setIgniteInstanceName(A1_Perform_Predictions.class.getName()));

 
        	
			try {
				
					dgService = 
							
					ignite.services(ignite.cluster().forAttribute("nodelabel", "dataprovider")).serviceProxy(DataGeneratorInterface.class.getName(),
									DataGeneratorInterface.class,
									true);

					oltpCachename = dgService.getOperationalCacheName(); 
					xactioncache = ignite.getOrCreateCache(oltpCachename);
					onlineCacheSize = xactioncache.size();
		            ClusterGroup onlineCacheNodes = ignite.cluster().forCacheNodes(oltpCachename);

		            ConfigPipeLineSettings.statusFileWriteMsg("Operational Cache: {" 
		            		+ oltpCachename +  "} is on these host nodes: " 
		            		+ onlineCacheNodes.hostNames() + "number of records: "
		        			+ onlineCacheSize );
		        	
			}
			catch(Exception e) {
				ConfigPipeLineSettings.statusFileWriteMsg("Exception trying to connect Data Generator service" + e.getMessage());
			}
			
	       	try {
        		
					mdlService = 
							ignite.services(ignite.cluster().forAttribute("nodelabel", "rfmodel")).serviceProxy(RFModelInterface.class.getName(),
													RFModelInterface.class,
													true);
					ConfigPipeLineSettings.statusFileWriteMsg("Fetched proxy to Model service: " + RFModelInterface.class.getName());
	               
	        }
	        catch (Exception e)
	        {
	        	ConfigPipeLineSettings.statusFileWriteMsg("Exception trying to connect to RF Model  service" + e.getMessage());
	        }
			
	        try { 
	        	
	
	        	
	        	
	        	if (BATCH_SIZE == 0)   // if not set from caller program use default
	        		BATCH_SIZE = 10;
	        	
	 
	        	
	        	
	        	
	        	
	       		ConfigPipeLineSettings.statusFileWriteMsg("Size of Online Cache before predicting/inserting new transactions: " + onlineCacheSize);

	        	Serializable [] newSourceXaction = null;
	        	

	        	Scanner myObj = new Scanner(System.in);  // Create a Scanner object
	        	String userinput = ""; 
	        	int batchsize = 0;
	        	int dummykey = -1;   // inputs to new transaction creates and predicts don't need actual key value
	        	do  {	        	  
	        	  onlineCacheSize = xactioncache.size();	        		
	        	  System.out.print(">>>> Online Cache size = {" + onlineCacheSize + "}; Enter # new Transactions to add [0 for no more, or [CR for default=" + BATCH_SIZE + "]: ");
	        	  userinput = myObj.nextLine();
	        	  batchsize = userinput.isBlank() ? BATCH_SIZE : Integer.parseInt(userinput);	
	        	  if (batchsize <= 0)
	        			break;	        		
	        	  System.out.println("generating {" + batchsize + "} transactions with predictions");	            
	        	// generate some simulated transactions
	        	  for(int key = onlineCacheSize; key < batchsize + onlineCacheSize; key++) {	        		
	        		// simulate a new transaction coming in from simulated OLTP application ..10
	        		// using training data generator  so ignore output label value in this case
	        		newSourceXaction = DatasetBuilder.newTransactionEvent(dummykey);
	        		double predicted = mdlService.predictOnSerializable(dummykey, newSourceXaction);	        		
	        		// in production would update the transaction here with predicted instead of using generated value..
	        		PREDICTIONS.put(key, predicted);	        		
	        		// now get what actual target value entered from  application
	        		ACTUALS.put(key,(double) newSourceXaction[DatasetBuilder.TARGET_COLUMN_INDEX]);	        			        	        		
	        		
	        		xactioncache.put(key, new DenseVector(newSourceXaction));
	        	
	        		           		
	           	if (ConfigPipeLineSettings.TEST_MODE)
	           			ConfigPipeLineSettings.statusFileWriteMsg("Key: " + key + ", Actual/Predicted: {" 
	           				+ ACTUALS.get(key) + " / " + PREDICTIONS.get(key)  + "}");
	        	  }
	        	  
	        	} while (batchsize > 0);
	        }
	        catch (Exception e)
	        {
	        	ConfigPipeLineSettings.statusFileWriteMsg("Exception Predictions/actuals measurement process: " + A1_Perform_Predictions.class.getName());	        	
	        }
	    
	        boolean retrainNeeded =  calculatePredictiveAccuracy();
	        if (retrainNeeded)
	        {
	        	String status = mdlService.updateModel(oltpCachename);
	        	ConfigPipeLineSettings.statusFileWriteMsg(status);
	        }

	        ConfigPipeLineSettings.statusFileWriteMsg("----\nPREDICTIONS and MEASUREMENTS done, close and flush status file\n-----\n time completed: "
	        		+ java.time.LocalTime.now());
	        
        
        } 
        catch (Exception e)
        {
        	ConfigPipeLineSettings.statusFileWriteMsg("Exception during Perform Prediction process: " + e.getMessage());
        	
        }
        
    	ConfigPipeLineSettings.statusFileWriterFlushAndClose();

	}
	
private static boolean   calculatePredictiveAccuracy() {
		
		double predicted = 0;
		double actual = 0;
        int totalAmount = 0;

        
        for (Map.Entry<Integer, Double> entry : ACTUALS.entrySet()) {
		    
        	actual = (double) entry.getValue();
		    predicted = (double) PREDICTIONS.get(entry.getKey());
		    
     		MSE += Math.pow(predicted - actual, 2.0);
            MAE += Math.abs(predicted - actual);
       		ConfigPipeLineSettings.statusFileWriteMsg("Key: " + entry.getKey() + ", Actual/Predicted: {" 
    				+ actual + " / " + predicted + "}");
            totalAmount++;
        }

        
		
		
		MSE /= totalAmount;
        MAE /= totalAmount;

       
        
        
        ConfigPipeLineSettings.statusFileWriteMsg("\n>>> # Predictions compared: " + totalAmount +
        			", Mean squared error (MSE): " + MSE + ", Mean absolute error (MAE):  " + MAE);
	    
        
		String status = "";
		
		boolean retrain = false;
		
		if (MAE <= ConfigPipeLineSettings.MAE_MAX)
		
			status = "MAE within margin: {" 
					+ ConfigPipeLineSettings.MAE_MAX 
					+ "}, Model Update not required";
		
		
		else {
			status = "MAE Exceeds MAE_MAX {" 
					+ ConfigPipeLineSettings.MAE_MAX 
					+ "}:  Model Update Will be Performed With New Actuals used as Training Data...";

			retrain = true;
		}
			 

        ConfigPipeLineSettings.statusFileWriteMsg(status);
        
        return retrain;
        
		
	}
	
	
	

}