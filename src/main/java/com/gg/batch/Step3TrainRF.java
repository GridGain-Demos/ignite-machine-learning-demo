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

package com.gg.batch;

import static com.gg.batch.DatasetBuilder.CATEGORICAL_FEATURE_ID;

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
import org.apache.ignite.ml.composition.ModelsComposition;
import org.apache.ignite.ml.dataset.feature.FeatureMeta;
import org.apache.ignite.ml.dataset.feature.extractor.Vectorizer;
import org.apache.ignite.ml.dataset.feature.extractor.impl.DummyVectorizer;
import org.apache.ignite.ml.environment.LearningEnvironmentBuilder;
import org.apache.ignite.ml.environment.logging.ConsoleLogger;
import org.apache.ignite.ml.environment.parallelism.ParallelismStrategy;
import org.apache.ignite.ml.math.primitives.vector.Vector;

import org.apache.ignite.ml.selection.split.TrainTestDatasetSplitter;
import org.apache.ignite.ml.selection.split.TrainTestSplit;
import org.apache.ignite.ml.tree.randomforest.RandomForestRegressionTrainer;
import org.apache.ignite.ml.tree.randomforest.data.FeaturesCountSelectionStrategies;

import com.gg.batch.ConfigPipeLineSettings.*;



public class Step3TrainRF {
    /**;
     * Run example.
     */
	
	
	public static int MODEL_CURRENT_VERSION_NUMBER = 0;
	public static int MODEL_VERSION_NUMBER_MAX = 10;
	
	public static  ModelsComposition [] RF_MODELS = new ModelsComposition[MODEL_VERSION_NUMBER_MAX];

	
	public static  RandomForestRegressionTrainer RF_TRAINER = null;
	
    public static void main(String[] args) {
    	
    	

    	ConfigPipeLineSettings.statusFileWriteMsg("\n--- starting Random Forest Training: ");

    	ConfigPipeLineSettings.checkDataSplitSettings(); // check to see if Env settings, otherwise set defaults
    	
    	ConfigPipeLineSettings.statusFileWriteMsg("DATA_SPLIT set to :" + ConfigPipeLineSettings.DATA_SPLIT);
     	
    	
        try  {
        	
            IgniteCluster cluster = ConfigPipeLineSettings.IGNITE_ML_PIPELINE.cluster();
            

            ClusterGroup dataCacheNodes = 
            		cluster.forCacheNodes(ConfigPipeLineSettings.TRAINING_DATASET_CACHE.getName());
            
            ConfigPipeLineSettings.statusFileWriteMsg("Cache nodes in this Cluster used to train this data: " + dataCacheNodes.hostNames());

            // pass in the final set of features from preprocess stages
            
            RF_TRAINER = new RandomForestRegressionTrainer(Step2PreProcess.FEATURES_META_DATA)
                .withAmountOfTrees(500)
                .withFeaturesCountSelectionStrgy(FeaturesCountSelectionStrategies.ALL)
                .withMaxDepth(5)
                .withMinImpurityDelta(0.)
                .withSubSampleSize(0.33)
                .withSeed(0);
            
            ConfigPipeLineSettings.statusFileWriteMsg("Trainer instanced: {" + RF_TRAINER.toString() + "}");


            RF_TRAINER.withEnvironmentBuilder(LearningEnvironmentBuilder.defaultBuilder()
                .withParallelismStrategyTypeDependency(ParallelismStrategy.NO_PARALLELISM)
                .withLoggingFactoryDependency(ConsoleLogger.Factory.LOW)
            );
            
            
            
            ConfigPipeLineSettings.statusFileWriteMsg("Trainer Environment blder instanced: {" + RF_TRAINER.toString() + "}");


            long startTraining = System.currentTimeMillis();

            TrainTestSplit<Integer, Vector> split = TrainTestSplitter(ConfigPipeLineSettings.DATA_SPLIT);
            ConfigPipeLineSettings.statusFileWriteMsg("TrainTestSplit set: " + split.toString() + ", = " + ConfigPipeLineSettings.DATA_SPLIT);	

            RF_MODELS[MODEL_CURRENT_VERSION_NUMBER] = RF_TRAINER.fit(
            		ConfigPipeLineSettings.IGNITE_ML_PIPELINE,
            		ConfigPipeLineSettings.TRAINING_DATASET_CACHE,
                split.getTrainFilter(),
                Step2PreProcess.ONEHOT_ENCODER_PREPROCESSOR
            );

            long endTraining = System.currentTimeMillis();

            ConfigPipeLineSettings.statusFileWriteMsg("\n>>> Trained RF model, Revision: {" + 
            		MODEL_CURRENT_VERSION_NUMBER + "}"
            		+  RF_MODELS[MODEL_CURRENT_VERSION_NUMBER].toString());
            ConfigPipeLineSettings.statusFileWriteMsg("Training time is " + (endTraining - startTraining) + " milliseconds");

          
            
            long endEvaluation = System.currentTimeMillis();
            ConfigPipeLineSettings.statusFileWriteMsg("Evaluation time is: " + (endEvaluation - endTraining) + " milliseconds");

           ConfigPipeLineSettings.setPredictiveModelReady();
            
            // dataCache.destroy();
            
  
        }
        catch(Exception e) {
        	ConfigPipeLineSettings.statusFileWriteMsg("Exception during Training process: " + e.getMessage());
        }
        finally {
            System.out.flush();
        }
    }
    
   
    
    public static     TrainTestSplit<Integer, Vector> TrainTestSplitter (float ratio)
    {
    	return new TrainTestDatasetSplitter<Integer, Vector>().split(ratio);
    }
    
   
    public static String updateModel()
    {
    	String status = "RF_MODELS" + "[" +  MODEL_CURRENT_VERSION_NUMBER + "]";
    	boolean isUpdatable = RF_TRAINER.isUpdateable(RF_MODELS[MODEL_CURRENT_VERSION_NUMBER]);
    	
    	if(isUpdatable == false)
    		status += " is not updateable";
    	
    	else if(MODEL_CURRENT_VERSION_NUMBER < MODEL_VERSION_NUMBER_MAX)
    	{
    		
    		RF_MODELS[ MODEL_CURRENT_VERSION_NUMBER + 1 ] = 
    				
    		RF_TRAINER.update(RF_MODELS[MODEL_CURRENT_VERSION_NUMBER], 
    				ConfigPipeLineSettings.IGNITE_ML_PIPELINE, 
    				ConfigPipeLineSettings.XACTION_CACHE, 
    				Step2PreProcess.ONEHOT_ENCODER_PREPROCESSOR);
    		
    		MODEL_CURRENT_VERSION_NUMBER++;
    		
    		status += " Updated to new Model version: " + MODEL_CURRENT_VERSION_NUMBER
    				+ ", used Online cache: " 
    				+ ConfigPipeLineSettings.XACTIONS_CACHE_NAME 
    				+ "\nNumber of entries used: " 
    				+ ConfigPipeLineSettings.XACTION_CACHE.size()
    				+ "\nNew Model algorithm: " + RF_MODELS[MODEL_CURRENT_VERSION_NUMBER].toString(true);
    		
    		
    		
    		
    	}
    	else 
    	{
    		status += "Can't update again Exceeded maximum # of updates: " + MODEL_VERSION_NUMBER_MAX;
    		ConfigPipeLineSettings.statusFileWriteMsg(status);
    	}
    	return status;
    }

    	
    
}
