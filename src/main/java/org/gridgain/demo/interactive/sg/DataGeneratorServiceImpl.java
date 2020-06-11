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

package org.gridgain.demo.interactive.sg;
import java.io.Serializable;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCluster;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.gridgain.demo.interactive.ConfigNodeClient;
import org.gridgain.demo.interactive.ConfigPipeLineSettings;
import org.gridgain.demo.interactive.sg.DatasetBuilder;

import java.io.Serializable;
import java.util.Random;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.math.primitives.vector.impl.DenseVector;


import it.unimi.dsi.fastutil.Arrays;




/**
 * 
 */
public class DataGeneratorServiceImpl implements Service, DataGeneratorInterface {
    /** Serial version UID. */
    private static final long serialVersionUID = 0L;
    
    
    /** Ignite instance. */
    @IgniteInstanceResource
    private Ignite ignite;

    
   

   
    /** Underlying cache map. */
     private IgniteCache<Integer, Double [] > cache;
//     private IgniteCache<K, V> cache2;

    
    private IgniteCache<Integer, Vector> trainingCache;
    
    private IgniteCache<Integer, Vector> operationalCache;
    
    
    private int rowcount;
    
  
    public Serializable [] []  generateUnLabeledRows(int rowCount)   // unlabeled means new transactions, not training
    {
    	Serializable [] [] dummyrows = new Serializable[10][10];
    	return dummyrows;
    }
    
    public Serializable []   generateUnLabeledRow(int key) // unlabeled new transaction for prediction, not training
    {
    	
    	return DatasetBuilder.newTransactionEvent(key);
    	
    }
	
	
	public int generateLabeledRows(int rowCount)  // labeled = training data; row count  ignored for now, use properties file
	{
		try {
		
	
		
			IgniteCluster cluster = ignite.cluster();
   
    	// true to write output CSV file
    	
			ConfigPipeLineSettings.TRAINING_DATASET_CACHE = 
        		DatasetBuilder.generateTrainingDataset(ignite, false);

			ClusterGroup dataCacheNodes = 
        		cluster.forCacheNodes( ConfigPipeLineSettings.TRAINING_CACHE_NAME);
        
			org.gridgain.demo.interactive.ConfigPipeLineSettings.statusFileWriteMsg("dataset generator run, training data cache name: " 
        		+ ConfigPipeLineSettings.TRAINING_CACHE_NAME + ", total size = " 
        		+ ConfigPipeLineSettings.TRAINING_DATASET_CACHE.sizeLong());
		}
		catch(Exception e)
		{
			System.out.println("exception during data generation" + e.getLocalizedMessage());
		}
		
		return (int) ConfigPipeLineSettings.TRAINING_DATASET_CACHE.size();
	}
	
	public Double [] [] rows = new Double [10] [10];
	
	public Double [] getRow(int key)
	{
		return rows[key];
		
	}
	

	
	public Vector getRowVector(int key)
	{
		 org.apache.ignite.ml.math.primitives.vector.Vector vec = null;
		 return vec;
	}
	
	
	
	public int printCSV(String filepath)
	{
		String filename = "";
		return 0;
	}
	
	public String getTrainingCacheName()
	{
		return "";
	}
	
	public String getOperationalCacheName()
	{
		if (operationalCache == null)
			operationalCache = DatasetBuilder.createNewTransactionsCache(ignite);
		
		return operationalCache.getName();
		
		
		
		
	}
	

	
	public int put(int key, Double [] values)
	{
		rows[key] = values;
		
		cache.put(key, values);
		
	
		for(int i = 0; i < values.length;i++)
			if (i == 0)
				System.out.println("inserted value via put()" + key);
			else 
			 System.out.print(", " + values[i]);
		return key;
	}
    
    /** {@inheritDoc} */
    @Override public int size() {
        return ConfigPipeLineSettings.TRAINING_DATASET_CACHE.size();
    }

    /** {@inheritDoc} */
    @Override public void cancel(ServiceContext ctx) {
        ignite.destroyCache(ctx.name());

        System.out.println("Data Generator Service was cancelled: " + ctx.name());
    }

    /** {@inheritDoc} */
    @Override public void init(ServiceContext ctx) throws Exception {
        // Create a new cache for every service deployment.
        // Note that we use service name as cache name, which allows
        // for each service deployment to use its own isolated cache.
        cache = ignite.getOrCreateCache(new CacheConfiguration<Integer, Double[] >(ctx.name()));

        System.out.println("Data Generator Service was initialized: " + ctx.name());
    }

    /** {@inheritDoc} */
    @Override public void execute(ServiceContext ctx) throws Exception {
        System.out.println("Executing distributed Data Generator service: " + ctx.name());
    }
}
