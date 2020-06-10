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

package com.gg.interactive.sg;





import com.gg.interactive.sg.DatasetBuilder;
import com.gg.interactive.Step2RFModelNode.*;

import com.gg.interactive.*;

import static com.gg.interactive.sg.DatasetBuilder.CATEGORICAL_FEATURE_ID;

import java.io.Serializable;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCluster;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.math.primitives.vector.impl.DenseVector;


import it.unimi.dsi.fastutil.Arrays;

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
import org.h2.store.Data;



/**
 *  
 */
public class RFModelServiceImpl implements Service, RFModelInterface {
    /** Serial version UID. */
    private static final long serialVersionUID = 02;

    double [] [] dummy = new double[2][2];
	
    private int processStage = 0;

    /** Ignite instance. */
    @IgniteInstanceResource
    private Ignite ignite;

    /** Underlying cache map. */
    private IgniteCache<Integer, Double [] > cache;
    
   

	
    
    
    
    public int getProcessStage () {
    	return processStage;
    }
	
	

	
    public	 int  preProcess()
    {
	
    	return Step2RFModelNode.preprocess();
    }

    public int  train()
    {
    	return Step2RFModelNode.train();
    }
    
    public String updateModel(String newcachename) {
    	
    	return Step2RFModelNode.updateModel(newcachename);
    }

    public double predictOnSerializable(int key, Serializable[] inputXaction) {
	

    	return Step2RFModelNode.predict(inputXaction);
    }





	
    public double predictOnDoubles(int key, Double [] values)
    {
    	return dummy[0][0];
    }
	
    public double predictOnVector(int key, Vector values)
    {
    	double dummy = 0.0;
	
	return dummy;

    }
	
    public double[]  batchPredictOnDoubles(int key, Double [] values)
    {
    	return dummy[0];
    }
	
    public double[]  batchPredictOnVector(int key, Vector values)
    {
    	return dummy[0];
    }
	
    public Double [] [] rows = new Double [10] [10];   // test
	
    public Double [] getRow(int key)
    {
    	return rows[10];
    }
	
    public Vector getRowVector(int key)
    {
    	org.apache.ignite.ml.math.primitives.vector.Vector vec = 
			new org.apache.ignite.ml.math.primitives.vector.impl.DenseVector();
	
    	return vec;
    }
	
    public int put(int key, Double [] values)
    {
		return 0;
    }
	
    public int printModel(String filepath)
    { 
    	return Step2RFModelNode.printModel();
    }
	
    public String getTrainingCacheName()
    {		
		return "";
    }
	
	
    


   
  
   
	
   
	
	
    
    /** {@inheritDoc} */
    @Override public int size() {
        return cache.size();
    }

    /** {@inheritDoc} */
    @Override public void cancel(ServiceContext ctx) {
        ignite.destroyCache(ctx.name());

        ConfigPipeLineSettings.statusFileWriteMsg("Model Service was cancelled: " + ctx.name());
    }

    /** {@inheritDoc} */
    @Override public void init(ServiceContext ctx) throws Exception {
        // Create a new cache for every service deployment.
        // Note that we use service name as cache name, which allows
        // for each service deployment to use its own isolated cache.
        cache = ignite.getOrCreateCache(new CacheConfiguration<Integer, Double[] >(ctx.name()));

        ConfigPipeLineSettings.statusFileWriteMsg("Random Forest Model Service was initialized: " + ctx.name());
     }

    /** {@inheritDoc} */
    @Override public void execute(ServiceContext ctx) throws Exception {
    	ConfigPipeLineSettings.statusFileWriteMsg("Executing RF Model distributed service: " + ctx.name());
    }
}
