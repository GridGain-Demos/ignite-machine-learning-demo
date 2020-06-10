package com.gg.batch;

import static com.gg.batch.ConfigPipeLineSettings.IGNITE_ML_PIPELINE;
import static com.gg.batch.ConfigPipeLineSettings.XACTIONS_CACHE_NAME;
import static com.gg.batch.DatasetBuilder.*;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCluster;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;

import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.internal.util.IgniteUtils;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.math.primitives.vector.impl.DenseVector;
import org.apache.ignite.ml.tree.randomforest.RandomForestRegressionTrainer;
import org.apache.ignite.ml.dataset.feature.extractor.Vectorizer;
import org.apache.ignite.ml.dataset.feature.extractor.impl.DummyVectorizer;
import org.apache.ignite.ml.math.primitives.vector.VectorUtils;


public class Step5MeasureAndReTrain {
	
	public static double MSE = 0;
	public static double MAE = 0;
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		

		ConfigPipeLineSettings.statusFileWriteMsg("\n---- Retraining phase: ");
		try {     	
			
			// get cache that holds "new" online entries, post training 
        	IgniteCache<Integer, Vector> xactioncache = ConfigPipeLineSettings.IGNITE_TRANSACTIONS.getOrCreateCache(XACTIONS_CACHE_NAME);
        	
        	int opscachesize = xactioncache.size();
        	
			ConfigPipeLineSettings.statusFileWriteMsg("\n >> Size of Online / Operational Cache {" 
        	
        			+ xactioncache.getName() + "} = number of records: "
        			+ xactioncache.size() + " \nOnline Predictions Model used is: {" 
        			+	Step3TrainRF.RF_MODELS[Step3TrainRF.MODEL_CURRENT_VERSION_NUMBER].toString() + "}");
        	
			
			
            double predicted, actual = 0;
            int totalAmount = 0;

            int key = opscachesize - Step4Predict.BATCH_SIZE;
			for ( ; key < opscachesize; key++)
			{
				actual = (double) Step4Predict.ACTUALS.get(key);
			
			
				predicted = (double) Step4Predict.PREDICTIONS.get(key);
				
				
				
           		MSE += Math.pow(predicted - actual, 2.0);
                MAE += Math.abs(predicted - actual);
           		ConfigPipeLineSettings.statusFileWriteMsg("Key: " + key + ", Actual/Predicted: {" 
        				+ actual + " / " + predicted + "}");
                totalAmount++;
			}
			
			
			MSE /= totalAmount;
	        MAE /= totalAmount;

	       
	        
	        
	        ConfigPipeLineSettings.statusFileWriteMsg("\n>>> # Predictions compared: " + totalAmount +
	        			", Mean squared error (MSE): " + MSE + ", Mean absolute error (MAE):  " + MAE);
		    
	        
	        String retrainstatus = retrain();

	        ConfigPipeLineSettings.statusFileWriteMsg(retrainstatus);
	        
	        
		}
		catch (Exception e)
		{
			ConfigPipeLineSettings.statusFileWriteMsg("Exception during Measure / ReTraining process" + e.getMessage());
		        	
		}
		finally {
		            System.out.flush();
		}
		
	}

	private static String  retrain() {
	
		String status = "";
		
		if (MAE <= ConfigPipeLineSettings.MAE_MAX)
		
			status = "MAE within margin: {" 
					+ ConfigPipeLineSettings.MAE_MAX 
					+ "}, Model Update not required";
		
		
		else {
			status = Step3TrainRF.updateModel();
		}
			 
		return status;
		
		
		
		
	}
	
	
}
