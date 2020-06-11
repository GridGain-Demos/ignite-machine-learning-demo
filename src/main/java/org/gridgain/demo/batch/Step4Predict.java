package org.gridgain.demo.batch;

import static org.gridgain.demo.batch.ConfigPipeLineSettings.*;
import static org.gridgain.demo.batch.DatasetBuilder.*;

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

public class Step4Predict {
	
	public static int BATCH_SIZE = 0;
	public static 	Map<Integer, Double> PREDICTIONS = new HashMap<>();
	public static 	Map<Integer, Double> ACTUALS = new HashMap<>();


	
			// simulate new xactions to be predicted  

	public static void main(String[] args) {
 
    	ConfigPipeLineSettings.statusFileWriteMsg("\n------- start Predictions with live transactions: ");
    	
        try { 
        	

        	IgniteConfiguration cfg2 = ConfigNodeClient.getIgniteClientConfiguration("Ops");
        	ConfigPipeLineSettings.IGNITE_TRANSACTIONS 
        		= Ignition.start(cfg2.setIgniteInstanceName("TransactionsDriver"));

        	
        	
        	IgniteCluster cluster = IGNITE_TRANSACTIONS.cluster();
        	
        	IgniteCache<Integer, Vector> xactioncache = createNewTransactionsCache(IGNITE_TRANSACTIONS);
        	int onlineCacheSize = xactioncache.size();

       
        	
            ClusterGroup onlineCacheNodes = cluster.forCacheNodes(xactioncache.getName());
            ConfigPipeLineSettings.statusFileWriteMsg("Operational Cache: {" + xactioncache.getName() +  
            "} is on these host nodes: " + onlineCacheNodes.hostNames());
  
        	
        	
        	ConfigPipeLineSettings.statusFileWriteMsg("\n >> Size of Operational OLTP  dataset {" 
        			+ xactioncache.getName() + "} = number of records: "
        			+ onlineCacheSize );
        	
        	
        	if (BATCH_SIZE == 0)   // if not set from caller program use default
        		BATCH_SIZE = 10;
        	
 
        	
        	
        	
        	
       		ConfigPipeLineSettings.statusFileWriteMsg("Size of Online Cache before predicting/inserting new transactions: " + onlineCacheSize);

        	Serializable [] newSourceXaction = null;
        	
    
        	
        	// generate some simulated transactions
        	for(int key = onlineCacheSize; key < BATCH_SIZE + onlineCacheSize; key++) {
        		
        		// simulate a new transaction coming in from simulated OLTP application ..
        		// using training data generator  so ignore output label value in this case
        		newSourceXaction = DatasetBuilder.newTransactionEvent(key);
        		
        		Vector predictedBeforeInsert = new DenseVector(newSourceXaction);
        		
        		double predicted = Step2PreProcess.buildPredictionInputVector(predictedBeforeInsert);
        		
        		// in production would update the transaction here with predicted instead of using generated value..
        		PREDICTIONS.put(key, predicted);
        		
        		// now get what actual target value entered from  application
        		ACTUALS.put(key,(double) newSourceXaction[DatasetBuilder.TARGET_COLUMN_INDEX]);
        		
        
        		// now enter into operations cache
        		ConfigPipeLineSettings.XACTION_CACHE.put(key, predictedBeforeInsert);
        		
        		
 
        	
        		           		
           		ConfigPipeLineSettings.statusFileWriteMsg("Key: " + key + ", Actual/Predicted: {" 
           				+ ACTUALS.get(key) + " / " + PREDICTIONS.get(key)  + "}");
           		
         
       		
        	}
  
        }
        catch (Exception e)
        {
        	ConfigPipeLineSettings.statusFileWriteMsg("Exception during Prediction process: " + e.getMessage());
        	
        }
        finally {
            System.out.flush();
        }
    }
    
   

}
