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

import java.util.HashMap;
import java.util.Map;


import com.gg.interactive.sg.*;

import java.util.Collection;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteServices;
import org.apache.ignite.services.*;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.ServiceResource.*;

import com.gg.interactive.sg.DataGeneratorInterface.*;


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
public class Step1DataProviderNode {
	
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
           		
           		ConfigPipeLineSettings.statusFileWriteMsg("DataProviderNode starting: " + java.time.LocalTime.now());
 	 
               	// Per-node singleton configuration.
           		ServiceConfiguration svcCfg2 = new ServiceConfiguration();
               	svcCfg2.setName(DataGeneratorInterface.class.getName());
               	svcCfg2.setMaxPerNodeCount(1);
               	svcCfg2.setService(new DataGeneratorServiceImpl());

               	// calls the ignite config xml file for a single node server
           		// IgniteConfiguration cfg1 = ConfigNodeServers.getIgniteServiceNodeConfiguration("DGN");
               	
               	IgniteConfiguration cfg1 = ConfigNodeClient.getIgniteClientConfiguration("DGN");
               	
               	Map <String, String> userAttr = new HashMap<>();
               	userAttr.put("nodelabel", "dataprovider");
               	cfg1.setUserAttributes(userAttr);
               	
           		
           		

           		Ignite ignite  = Ignition.start(cfg1.setIgniteInstanceName(Step1DataProviderNode.class.getName()));
        
           	
           		IgniteServices svcs = ignite.services(ignite.cluster().forAttribute("nodelabel", "dataprovider"));
           	

           		svcs.deployNodeSingleton(DataGeneratorInterface.class.getName(), new DataGeneratorServiceImpl());
           		
           		
                 DataGeneratorInterface dg = 	svcs.service(DataGeneratorInterface.class.getName());
                 com.gg.interactive.ConfigPipeLineSettings.statusFileWriteMsg("Locally deployed Data provider service is present: " + dg.toString());
           
               
                 ConfigPipeLineSettings.statusFileWriteMsg("Data Provider Node , Service Impl, Service Name started up: " 
           				+ ignite.name() + ", "
           				+ DataGeneratorServiceImpl.class.getName() + ", " 
           				+ DataGeneratorInterface.class.getName());
                 
                 ConfigPipeLineSettings.statusFileWriterFlush();

               
    
            } 
            catch(Exception e)
            {
            	ConfigPipeLineSettings.statusFileWriteMsg("ML Data Gen Service Exception");;
            }
    }

   
   

    
}
