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
import java.util.*;

import com.gg.interactive.sg.*;

import java.util.Collection;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteServices;
import org.apache.ignite.services.*;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.ServiceResource.*;
import org.apache.ignite.IgniteCluster;


import com.gg.interactive.sg.DataGeneratorInterface.*;
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
public class A0_Build_Model {
	
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
           		
 
           		ConfigPipeLineSettings.statusFileWriteMsg("Data generation and Model building starting up: " + java.time.LocalTime.now());

           		
           		// will start a local test cache server if specified in properties
           		// not needed if running in Kubernetes which will have cluster nodes already running
           	// 	ConfigPipeLineSettings.checkTestServerFlag(args); 
           	
           		IgniteConfiguration cfg1 = ConfigNodeClient.getIgniteClientConfiguration("Called from ML builder workflow");

           		Ignite ignite  = Ignition.start(cfg1.setIgniteInstanceName("ML builder workflow"));
           		
          

    			
    			int status = 0;
    			try {
    				
    				// in this code we are only running a single node with Data Generator and RF model
    				// in production need to have a cluster of multiple nodes for each server
    					DataGeneratorInterface dgService = 
    							ignite.services(ignite.cluster().forAttribute("nodelabel", "dataprovider")).serviceProxy(DataGeneratorInterface.class.getName(),
    									DataGeneratorInterface.class,
    									true);
    
    					status = dgService.generateLabeledRows(1); // input value ignored, using properties file
    					com.gg.interactive.ConfigPipeLineSettings.statusFileWriteMsg("Data Generation service GenerateRows()  performed, record count: " + dgService.size());
			  
    			}
    			catch(Exception e) {
    				com.gg.interactive.ConfigPipeLineSettings.statusFileWriteMsg("Exception during data gen service invoke; " + e.getMessage());
    			}
    			
               
    			try {
    					RFModelInterface mdlService = 
    					ignite.services(ignite.cluster().forAttribute("nodelabel", "rfmodel")).serviceProxy(RFModelInterface.class.getName(),
    													RFModelInterface.class,
    													true);
    	               
    	    			status = mdlService.preProcess();
    	    			ConfigPipeLineSettings.statusFileWriteMsg("ML Service preprocess() 1 of 2 completed"); 
    	               
    	                status = mdlService.train();
    	                ConfigPipeLineSettings.statusFileWriteMsg("ML service Training() 2 of 2 completed");   
    			}
    			catch(Exception e) {
    				ConfigPipeLineSettings.statusFileWriteMsg("Exception during ML service calls; " + e.getMessage());
    			}
			
           
    			
                       
               

               
            } 
            catch(Exception e)
            {
            	ConfigPipeLineSettings.statusFileWriteMsg(" preprocessing and training test failed: " + A0_Build_Model.class.getName());
            }
        
        	ConfigPipeLineSettings.statusFileWriterFlush();
    
    }

   

    
}
