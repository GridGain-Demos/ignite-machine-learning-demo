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
import java.util.Random;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.math.primitives.vector.impl.DenseVector;

/**
* proxy to Data Generator provider service
*  
*  */
public interface DataGeneratorInterface {
     
 
	public 	int size ();
	
	public	int generateLabeledRows(int rowCount); // for training data
	
	public Serializable[] []   generateUnLabeledRows(int rowCount);  //  for "new" unlabeled data, pre-prediction without a label
	
	public Serializable[]   generateUnLabeledRow(int key);  //  for "new" unlabeled data, pre-prediction without a label
	
	
	public Double [] [] rows = new Double [10] [10];   // test
	
	public Double [] getRow(int key);   
	
	public Vector getRowVector(int key);
	
	public int put(int key, Double [] values);
	
	public int printCSV(String filepath);
	
	public String getTrainingCacheName();
	
	public String getOperationalCacheName();
	
	
}
