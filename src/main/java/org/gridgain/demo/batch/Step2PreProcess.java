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

import static org.gridgain.demo.batch.DatasetBuilder.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
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
import org.apache.ignite.ml.composition.ModelsComposition;
import org.apache.ignite.ml.dataset.feature.FeatureMeta;
import org.apache.ignite.ml.dataset.feature.extractor.Vectorizer;
import org.apache.ignite.ml.dataset.feature.extractor.impl.DummyVectorizer;
import org.apache.ignite.ml.environment.LearningEnvironmentBuilder;
import org.apache.ignite.ml.environment.logging.ConsoleLogger;
import org.apache.ignite.ml.environment.parallelism.ParallelismStrategy;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.preprocessing.Preprocessor;
import org.apache.ignite.ml.preprocessing.encoding.EncoderTrainer;
import org.apache.ignite.ml.preprocessing.encoding.EncoderType;
import org.apache.ignite.ml.selection.scoring.evaluator.Evaluator;
import org.apache.ignite.ml.selection.scoring.metric.classification.Accuracy;
// import org.apache.ignite.ml.selection.scoring.metric.regression.RegressionMetricValues;
import org.apache.ignite.ml.selection.split.TrainTestDatasetSplitter;
import org.apache.ignite.ml.selection.split.TrainTestSplit;
import org.apache.ignite.ml.tree.randomforest.RandomForestRegressionTrainer;
import org.apache.ignite.ml.tree.randomforest.data.FeaturesCountSelectionStrategies;
import org.gridgain.demo.batch.ConfigPipeLineSettings.*;
import org.gridgain.demo.batch.Step3TrainRF.*;
import org.h2.store.Data;
// import org.omg.CORBA.DATA_CONVERSION;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.math.primitives.vector.impl.DenseVector;

public class Step2PreProcess {
    /**;
     * Run example.
     */
	

	
	public static  Preprocessor<Integer, Vector> ONEHOT_ENCODER_PREPROCESSOR = null;
	
	public static List<FeatureMeta> FEATURES_META_DATA = null;
	
    public static void main(String[] args) {
    	
    	

    	ConfigPipeLineSettings.statusFileWriteMsg("\n-------PreProcessing: " );
    	
        	
     	
    	
        try  {
        	
            IgniteCluster cluster = ConfigPipeLineSettings.IGNITE_ML_PIPELINE.cluster();
            

            ClusterGroup dataCacheNodes = 
            		cluster.forCacheNodes(ConfigPipeLineSettings.TRAINING_DATASET_CACHE.getName());
            
            ConfigPipeLineSettings.statusFileWriteMsg("Cache nodes in this Cluster used to PreProcess this data set {" +
            ConfigPipeLineSettings.TRAINING_DATASET_CACHE.getName() + "} : "+ dataCacheNodes.hostNames());

            final Vectorizer<Integer, Vector, Integer, Double> vectorizer = new DummyVectorizer<Integer>()
                .labeled(Vectorizer.LabelCoordinate.LAST);
            
            ConfigPipeLineSettings.statusFileWriteMsg("Vectorizer instanced: {" + vectorizer.toString() + "}");

            long startPreProcessing = System.currentTimeMillis();


            ONEHOT_ENCODER_PREPROCESSOR = new EncoderTrainer<Integer, Vector>()
                .withEncoderType(EncoderType.ONE_HOT_ENCODER)
                .withEncodedFeature(CATEGORICAL_FEATURE_ID)
                .fit(ConfigPipeLineSettings.IGNITE_ML_PIPELINE,
                		ConfigPipeLineSettings.TRAINING_DATASET_CACHE,
                    vectorizer
                );

            
            ConfigPipeLineSettings.statusFileWriteMsg("PreProcessor instanced: {" 
            			+ ONEHOT_ENCODER_PREPROCESSOR.toString() + "}");

            AtomicInteger idx = new AtomicInteger(0);
            int amountOfFeatures = DatasetBuilder.COL_COUNT + DatasetBuilder.CATEGORICAL_FEATURES.length; 
            
           
            // basic features + amount of one-hot-encoded columns
            FEATURES_META_DATA = IntStream.range(0, amountOfFeatures - 1)
                .mapToObj(x -> new FeatureMeta("PreProcessorFeature"  , idx.getAndIncrement(), false)).collect(Collectors.toList());


            
            ConfigPipeLineSettings.statusFileWriteMsg("Preprocess dataset Meta features built: " + FEATURES_META_DATA.toString() );
            		
           

            ConfigPipeLineSettings.statusFileWriteMsg("PreProcessing elapsed time is "
            			+ (System.currentTimeMillis() - startPreProcessing) + " milliseconds");
        }
        finally {
            System.out.flush();
        }
    }
    
    // preprocess online transaction into format to send into Model predictor
    public static double buildPredictionInputVector (Vector onlineTransaction)
    
    {
    	double predictedValue = 0.0;
    	
    	int featuresmetadatasize = // total number of cols with 3 one-hots added
    			
    	DatasetBuilder.COL_COUNT + DatasetBuilder.CATEGORICAL_FEATURES.length - 1;
    	
    	
    	
    	double [] predictvectorvalues = new double [featuresmetadatasize]; // set size first
    	
    	// now copy columns of onlineTransaction vector
    	
        for (int i = 0; i < onlineTransaction.size(); i++)
        	predictvectorvalues[i] = onlineTransaction.get(i);
   
    	
    	try {
    		    	
    			Vector inputvector1 = new DenseVector(predictvectorvalues);
    	
    			int categoryvalue = (int) onlineTransaction.get(CATEGORICAL_FEATURE_ID);
    	


    	
    			try {
    			
    				// these are sent to the model predict function - in doubles with one-hots added    	
    				
    				predictvectorvalues[15] = predictvectorvalues[16] = predictvectorvalues[17] = 0.0;
    				predictvectorvalues[15 + (categoryvalue - 1)] = 1.0;

 
 
    				inputvector1.assign(predictvectorvalues);
    				
    				
    				
    				if (ConfigPipeLineSettings.TEST_MODE)
    				 for (int i = 0; i < inputvector1.size() ; i++)
    				 {
    					ConfigPipeLineSettings.statusFileWriteMsg("TEST Message from preprocess, model input vector [" + i + "], " + inputvector1.get(i));
    				 }

    			}
    			
    			catch (Exception e) {	
    				ConfigPipeLineSettings.statusFileWriteMsg("exception vector parsing" + e.getMessage());
    		
    			}
    			
    			
    		
    			try {         		
    				predictedValue = 	
    					Step3TrainRF.RF_MODELS[Step3TrainRF.MODEL_CURRENT_VERSION_NUMBER].predict(inputvector1);
    			
    			}
    	
    			catch (Exception e) {	
    				ConfigPipeLineSettings.statusFileWriteMsg("exception during Model.predict call" + e.getMessage());
 
    			}
		
    	
    	}
    	catch(Exception e) {
    		ConfigPipeLineSettings.statusFileWriteMsg("exception during PreProcessor Prediction Input Builder: " + e.getMessage());
    	}
    	
    	return predictedValue;
    	
    }
   
  
    	
    
}
