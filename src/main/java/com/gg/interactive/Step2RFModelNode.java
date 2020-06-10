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

package com.gg.interactive;

import com.gg.interactive.sg.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.io.Serializable;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCluster;
import org.apache.ignite.IgniteServices;
import org.apache.ignite.services.*;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.ml.composition.ModelsComposition;
import org.apache.ignite.ml.dataset.feature.FeatureMeta;
import org.apache.ignite.ml.dataset.feature.extractor.Vectorizer;
import org.apache.ignite.ml.dataset.feature.extractor.impl.DummyVectorizer;
import org.apache.ignite.ml.environment.LearningEnvironmentBuilder;
import org.apache.ignite.ml.environment.logging.ConsoleLogger;
import org.apache.ignite.ml.environment.parallelism.ParallelismStrategy;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.math.primitives.vector.impl.DenseVector;
import org.apache.ignite.ml.preprocessing.Preprocessor;
import org.apache.ignite.ml.preprocessing.encoding.EncoderTrainer;
import org.apache.ignite.ml.preprocessing.encoding.EncoderType;
import org.apache.ignite.ml.selection.split.TrainTestDatasetSplitter;
import org.apache.ignite.ml.selection.split.TrainTestSplit;
import org.apache.ignite.ml.tree.randomforest.RandomForestRegressionTrainer;
import org.apache.ignite.ml.tree.randomforest.data.FeaturesCountSelectionStrategies;
import org.apache.ignite.resources.ServiceResource.*;

import com.gg.interactive.sg.RFModelInterface.*;



/**
 * Example that demonstrates how to deploy distributed services in Ignite.
 * Distributed services are especially useful when deploying singletons on the ignite,
 * be that cluster-singleton, or per-node-singleton, etc...
 * <p>
 * To start remote nodes, you must run {@link ExampleNodeStartup} in another JVM
 * which will start node with {@code examples/config/example-ignite.xml} configuration.
 * <p>
 * NOTE:<br/>
 * Starting {@code ignite.sh} directly will not work, as distributed services
 * cannot be peer-deployed and classes must be on the classpath for every node.
 */
public class Step2RFModelNode {
	
	public static  Preprocessor<Integer, Vector> ONEHOT_ENCODER_PREPROCESSOR = null;

	public static List<FeatureMeta> FEATURES_META_DATA = null;

	public static int MODEL_CURRENT_VERSION_NUMBER = 0;
	public static int MODEL_VERSION_NUMBER_MAX = 10;

	public static  ModelsComposition [] RF_MODELS = new ModelsComposition[MODEL_VERSION_NUMBER_MAX];


	public static  RandomForestRegressionTrainer RF_TRAINER = null;
	
	private static int PROCESS_STAGE=0;
	
	public static Ignite ignite = null;
	
	private static IgniteCache<Integer, Vector> TRAINING_CACHE = null; // generated from data provider with its training cache creates its cache

	
	static Double [] tests = { 0.0, 1.2, 3.0 };
    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws Exception If example execution failed.
     */
    public static void main(String[] args) throws Exception {
        // Mark this node as client node.
    	

 

        try {
        		
           		ConfigPipeLineSettings.main(args);
        	
        	
           		ConfigPipeLineSettings.statusFileWriteMsg("RandomForest Model node starting: " + java.time.LocalTime.now());

           	
           	//	ServiceConfiguration svcCfg2 = new ServiceConfiguration();
             //  	svcCfg2.setName(RFModelInterface.class.getName());
             //  	svcCfg2.setMaxPerNodeCount(1);
             //  	svcCfg2.setService(new RFModelServiceImpl());

               	
          	IgniteConfiguration cfg1 = ConfigNodeClient.getIgniteClientConfiguration("DGN");
           		
//            		IgniteConfiguration cfg1 = ConfigNodeServers.getIgniteModelNodeConfiguration("RFMN");
   
          //	cfg1.setServiceConfiguration(svcCfg2);
          	
          		Map <String, String> userAttr = new HashMap<>();
          		userAttr.put("nodelabel", "rfmodel");
          		cfg1.setUserAttributes(userAttr);
           	
           		
           		
           		try {
           			ignite  = Ignition.start(cfg1.setIgniteInstanceName(Step2RFModelNode.class.getName()));
           		}
           		catch(Exception e)
           		{
           			ConfigPipeLineSettings.statusFileWriteMsg("Exception during Model node Ignition start() call: " + e.getMessage());
           		}
           		
           		try {
           			
           			// Per-node singleton configuration.
           			IgniteServices svcs = ignite.services(ignite.cluster().forAttribute("nodelabel", "rfmodel"));
           			svcs.deployNodeSingleton(RFModelInterface.class.getName(), new RFModelServiceImpl());
           
           			RFModelInterface rfmdl = //	svcs.service(RFModelInterface.class.getName());
           					svcs.serviceProxy(RFModelInterface.class.getName(), RFModelInterface.class, true);
           			ConfigPipeLineSettings.statusFileWriteMsg("Locally deployed service is present: " + rfmdl.toString());
           		}
           		catch (Exception e)
           		{
           			ConfigPipeLineSettings.statusFileWriteMsg("Exception trying to deploy Model node service: " + e.getMessage());
           		}
           		
               
           		
           		ConfigPipeLineSettings.statusFileWriteMsg("Random Forest Model node / service impl / service name started up: " 
           					+ ignite.name() + ", " 
           					+ RFModelServiceImpl.class.getName() + ", "
           					+ RFModelInterface.class.getName()) ; 
               
            } 
            catch(Exception e)
            {
            	ConfigPipeLineSettings.statusFileWriteMsg("RF training node startup Exception" + e.getMessage());
            }
    
        	ConfigPipeLineSettings.statusFileWriterFlush();
    }

    public static int preprocess () {
    	int status = 0;
        try  {
        	
        	String trainingDataCacheName = ConfigPipeLineSettings.TRAINING_CACHE_NAME;
        	TRAINING_CACHE = ignite.getOrCreateCache(trainingDataCacheName);
    	
    		IgniteCluster cluster = ignite.cluster();
            

            ClusterGroup dataCacheNodes = 
            		cluster.forCacheNodes(trainingDataCacheName);
            
            ConfigPipeLineSettings.statusFileWriteMsg("Cache nodes in this Cluster used to PreProcess this data set {" +
            		trainingDataCacheName + "} : "+ dataCacheNodes.hostNames());

            final Vectorizer<Integer, Vector, Integer, Double> vectorizer = new DummyVectorizer<Integer>()
                .labeled(Vectorizer.LabelCoordinate.LAST);
            
            ConfigPipeLineSettings.statusFileWriteMsg("Vectorizer instanced: {" + vectorizer.toString() + "}");

            long startPreProcessing = System.currentTimeMillis();


            com.gg.interactive.Step2RFModelNode.ONEHOT_ENCODER_PREPROCESSOR = new EncoderTrainer<Integer, Vector>()
                .withEncoderType(EncoderType.ONE_HOT_ENCODER)
                .withEncodedFeature(DatasetBuilder.CATEGORICAL_FEATURE_ID)
                .fit(ignite, TRAINING_CACHE, vectorizer
                );

            
            ConfigPipeLineSettings.statusFileWriteMsg("PreProcessor instanced: {" 
            			+ com.gg.interactive.Step2RFModelNode.ONEHOT_ENCODER_PREPROCESSOR.toString() + "}");

            AtomicInteger idx = new AtomicInteger(0);
            int amountOfFeatures = DatasetBuilder.COL_COUNT + DatasetBuilder.CATEGORICAL_FEATURES.length; 
            
           
            // basic features + amount of one-hot-encoded columns
            FEATURES_META_DATA = IntStream.range(0, amountOfFeatures - 1)
                .mapToObj(x -> new FeatureMeta("PreProcessorFeature"  , idx.getAndIncrement(), false)).collect(Collectors.toList());


            
            ConfigPipeLineSettings.statusFileWriteMsg("Preprocess dataset Meta features built: " 
            
            		+ FEATURES_META_DATA.toString() );
            		
           

            ConfigPipeLineSettings.statusFileWriteMsg("PreProcessing elapsed time is "
            			+ (System.currentTimeMillis() - startPreProcessing) + " milliseconds");
        }
        catch(Exception e)
        {
        	status = 1;
        	ConfigPipeLineSettings.statusFileWriteMsg("Exception during PreProcessing: " + e.getMessage());
        }
        
        PROCESS_STAGE = 1;
        
        return status;

    	
    }
   
    public static int train ()
    {
    	if (PROCESS_STAGE < 1)
    	{
    		ConfigPipeLineSettings.statusFileWriteMsg("\nCan't start Random Forest Training, no preprocessing done yet: ");
    		return -1; 
    	}
    	
    	String trainingDataCacheName = ConfigPipeLineSettings.TRAINING_CACHE_NAME;
    	
    	ConfigPipeLineSettings.statusFileWriteMsg("\n--- starting Random Forest Training, using training cache { + "
    			+ trainingDataCacheName + "}: ");

    	ConfigPipeLineSettings.checkDataSplitSettings(); // check to see if Env settings, otherwise set defaults
    	
    	ConfigPipeLineSettings.statusFileWriteMsg("DATA_SPLIT set to :" 
    	+ ConfigPipeLineSettings.DATA_SPLIT);
     	
    	 try  {
         	
             IgniteCluster cluster = ignite.cluster();
             

             ClusterGroup dataCacheNodes = 
             		cluster.forCacheNodes(trainingDataCacheName);
             
             ConfigPipeLineSettings.statusFileWriteMsg("Cache nodes in this Cluster used to train this data: " + dataCacheNodes.hostNames());

             // pass in the final set of features from preprocess stages
             
             RF_TRAINER = 
            		 new RandomForestRegressionTrainer(FEATURES_META_DATA)
                 .withAmountOfTrees(500)
                 .withFeaturesCountSelectionStrgy(FeaturesCountSelectionStrategies.ALL)
                 .withMaxDepth(5)
                 .withMinImpurityDelta(0.)
                 .withSubSampleSize(0.33)
                 .withSeed(0);
             
             ConfigPipeLineSettings.statusFileWriteMsg("Trainer instanced: {" 
            		 + RF_TRAINER.toString() + "}");


             RF_TRAINER.withEnvironmentBuilder(LearningEnvironmentBuilder.defaultBuilder()
                 .withParallelismStrategyTypeDependency(ParallelismStrategy.NO_PARALLELISM)
                 .withLoggingFactoryDependency(ConsoleLogger.Factory.LOW)
             );
             
             
             
             ConfigPipeLineSettings.statusFileWriteMsg("Trainer Environment blder instanced: {" + 
            		 RF_TRAINER.toString() + "}");


             long startTraining = System.currentTimeMillis();

             TrainTestSplit<Integer, Vector> split = 
            		 TrainTestSplitter(ConfigPipeLineSettings.DATA_SPLIT);
             
             ConfigPipeLineSettings.statusFileWriteMsg("TrainTestSplit set: " + split.toString() + ", = " + 
            		 ConfigPipeLineSettings.DATA_SPLIT);	

             RF_MODELS[MODEL_CURRENT_VERSION_NUMBER] 
            		 = RF_TRAINER.fit(
            				 ignite,
            				 TRAINING_CACHE,
            				 split.getTrainFilter(),
            				 ONEHOT_ENCODER_PREPROCESSOR
             );

             long endTraining = System.currentTimeMillis();

             com.gg.interactive.ConfigPipeLineSettings.statusFileWriteMsg("\n>>> Trained RF model, Revision: {" + 
            		 MODEL_CURRENT_VERSION_NUMBER + "}"
             		+  RF_MODELS[MODEL_CURRENT_VERSION_NUMBER].toString());
             com.gg.interactive.ConfigPipeLineSettings.statusFileWriteMsg("Training time is " + (endTraining - startTraining) + " milliseconds");

           
             
             long endEvaluation = System.currentTimeMillis();
             com.gg.interactive.ConfigPipeLineSettings.statusFileWriteMsg("Evaluation time is: " + (endEvaluation - endTraining) + " milliseconds");

             com.gg.interactive.ConfigPipeLineSettings.setPredictiveModelReady();
             
     
             // dataCache.destroy();
             

            PROCESS_STAGE=2;
         }
         catch(Exception e) {
        	 com.gg.interactive.ConfigPipeLineSettings.statusFileWriteMsg("Exception during Training process: " + e.getMessage());
         }
    	
    	return PROCESS_STAGE;
    	
    }
    
    public static double predict (Serializable [] inboundtransaction)
    {
    	Vector rawDenseVector = new DenseVector(inboundtransaction);
		
    	int expandedColumnCount =  // number of cols with one-hots added
    			
    			DatasetBuilder.COL_COUNT + DatasetBuilder.CATEGORICAL_FEATURES.length - 1;  // leave off the label at the end
    	
    	double predictedValue = -1.0;
    	
    	double [] predictDoublesValues = new double [expandedColumnCount]; // all columns including one-hots 
    	
    	if(ConfigPipeLineSettings.TEST_MODE)
    		ConfigPipeLineSettings.statusFileWriteMsg("TEST MESSAGE in predict()  > Trained RF model, Revision: {" + 
            		 MODEL_CURRENT_VERSION_NUMBER + "}"
             		+  RF_MODELS[MODEL_CURRENT_VERSION_NUMBER].toString());
    	
    	
    	// now copy columns of onlineTransaction dense vector into first 15 values 
    	if(ConfigPipeLineSettings.TEST_MODE)
    		for (int i = 0; i < rawDenseVector.size(); i++)
    		{
    			predictDoublesValues[i] = rawDenseVector.get(i);
        	
    			ConfigPipeLineSettings.statusFileWriteMsg("TEST message in predict() doubles array ["  +i + "] " + predictDoublesValues[i]);
    		}
    	
    	try {
    		    	
    			Vector modelInputDenseVector = new DenseVector(predictDoublesValues);
    	
    			int categoryvalue = (int) rawDenseVector.get(13);
    	
    			try {
    			
    				
    				// these are sent to the model predict function - in doubles with one-hots added    	
    				predictDoublesValues[15] = predictDoublesValues[16] = predictDoublesValues[17] = 0.0;
    				predictDoublesValues[15 + (categoryvalue - 1)] = 1.0;
    				
    				

    				modelInputDenseVector.assign(predictDoublesValues);
    				
    				if(ConfigPipeLineSettings.TEST_MODE)
    				  for (int i = 0; i < modelInputDenseVector.size() ; i++)
    				  {
    					System.out.println("TEST Message in predict() model input vector [" + i + "], " + modelInputDenseVector.get(i));
    				  }
    			}
    			
    			catch (Exception e) {	
    				ConfigPipeLineSettings.statusFileWriteMsg("exception vector parsing" + e.getMessage());
    		
    			}
    			
    	
    		
    			try {         		
    				predictedValue = 	
    						RF_MODELS[MODEL_CURRENT_VERSION_NUMBER].predict(modelInputDenseVector);
    				System.out.println("output from model .predict() : " + predictedValue);
    			
    			}
    	
    			catch (Exception e) {	
    				ConfigPipeLineSettings.statusFileWriteMsg("exception during Model.predict() call, vector value: " + modelInputDenseVector.toString());

    			}
    	
    	
    	}
    	catch(Exception e) {
    		ConfigPipeLineSettings.statusFileWriteMsg("exception during PreProcessor Prediction Input Builder: " + e.getMessage());
    	}
    	
    	return predictedValue;
    	
    }
    
    public static int printModel()
    {
		String status = "Model Class, Version , contents:  " +  
				"\n CLASS="+ RF_MODELS[MODEL_CURRENT_VERSION_NUMBER].getClass() 
				+ "\n version = " + MODEL_CURRENT_VERSION_NUMBER
				+ "\n CONTENTS = \n" + RF_MODELS[MODEL_CURRENT_VERSION_NUMBER].toString(true);

		
		com.gg.interactive.ConfigPipeLineSettings.statusFileWriteMsg(status);
 		return 0;
    	
    }
    
    public static String  updateModel(String newTransactionsCacheName)
    {
    
    	String status = "RF_MODELS" + "[" +  MODEL_CURRENT_VERSION_NUMBER + "]";
    	
    	boolean isUpdatable 
    	
    	= RF_TRAINER.isUpdateable(RF_MODELS[MODEL_CURRENT_VERSION_NUMBER]);
    	
    	
    	if(isUpdatable == false)
    		status += " is not updateable";
    	
    	IgniteCache<Integer, Vector> xactionCache = 
    			ignite.getOrCreateCache(newTransactionsCacheName);
    	
    	if(MODEL_CURRENT_VERSION_NUMBER < MODEL_VERSION_NUMBER_MAX)
    	{
    		
    		RF_MODELS[MODEL_CURRENT_VERSION_NUMBER + 1 ] = 
    				
    				RF_TRAINER.update(RF_MODELS[MODEL_CURRENT_VERSION_NUMBER], 
    				ignite,	// ignite handle injected into this impl
    				xactionCache, 
    				com.gg.interactive.Step2RFModelNode.ONEHOT_ENCODER_PREPROCESSOR);
    		
    		MODEL_CURRENT_VERSION_NUMBER++;
    		
    		status += " Updated to new Model version: " + MODEL_CURRENT_VERSION_NUMBER
    				+ ", used Online cache: " 
    				+ ConfigPipeLineSettings.XACTIONS_CACHE_NAME 
    				+ "\nNumber of entries used: " 
    				+ xactionCache.size()
    				+ "\nNew Model algorithm: " + RF_MODELS[MODEL_CURRENT_VERSION_NUMBER].toString();
    		
    		
    		
    		
    	}
    	else 
    	{
    		status += "Can't update Model again, Exceeded maximum # of Model updates: " + MODEL_VERSION_NUMBER_MAX;
    		ConfigPipeLineSettings.statusFileWriteMsg(status);
    	}
    	return status;
    }

    	

	static private       TrainTestSplit<Integer, Vector> TrainTestSplitter (float ratio)
    {
    	return new TrainTestDatasetSplitter<Integer, Vector>().split(ratio);
    }
	
	 // preprocess online transaction into format to send into Model predictor
    public  double buildPredictionInputVector (Vector onlineTransaction)
    {
    
    	
	
    	double predictedValue = 0.0;
	
    	int featuresmetadatasize = // does this get garbage collected? FEATURES_META_DATA.size();   // number of cols with one-hots added
			
    			DatasetBuilder.COL_COUNT + DatasetBuilder.CATEGORICAL_FEATURES.length;
	
	
	
    	double [] predictvectorvalues = new double [featuresmetadatasize]; // set size first
	
    	// now copy columns of onlineTransaction vector
	
    	for (int i = 0; i < onlineTransaction.size(); i++)
    		predictvectorvalues[i] = onlineTransaction.get(i);

	
		try {
		    	
			Vector inputvector1 = new DenseVector(predictvectorvalues);
	
			int categoryvalue = (int) onlineTransaction.get(DatasetBuilder.CATEGORICAL_FEATURE_ID);
	


	
			try {
			
				

				//// these are sent to the model predict function - in doubles with one-hots added    	
				predictvectorvalues[15] = predictvectorvalues[16] = predictvectorvalues[17] = 0.0;
				predictvectorvalues[15 + (categoryvalue - 1)] = 1.0;
				/////

				inputvector1.assign(predictvectorvalues);

			}
			
			catch (Exception e) {	
				ConfigPipeLineSettings.statusFileWriteMsg("exception vector parsing" + e.getMessage());
		
			}
			
			
		
			try {         		
				predictedValue = 	
						RF_MODELS[MODEL_CURRENT_VERSION_NUMBER].predict(inputvector1);
			
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


