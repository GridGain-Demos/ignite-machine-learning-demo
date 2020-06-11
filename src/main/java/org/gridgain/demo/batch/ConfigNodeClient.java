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


import java.io.FileNotFoundException;
import java.io.Serializable;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Scanner;
import java.util.UUID;


public class ConfigNodeClient {
    public static  	String 	LOCAL_CONFIG_FILE = "ignite-client.xml";	// client config uses local IP finder
    public static	String	AKS_CONFIG_FILE = "/gridgain/config/ml-demo-client.xml";   // otherwise Kubernetes IP finder
    public static String CONFIG_FILE = "";
    
    public static boolean  initialize () {
    	
    
    
       boolean  configFileinProps = false;

    	String configEnvSetting  = // System.getenv("CONFIG_FILE"); // only set this var for local, don't set on Azure
    	
    	ConfigPipeLineSettings.MLPIPELINEPROPS.getProperty("CONFIG_FILE").toString();
    	
    	if (configEnvSetting == null) {
    		configEnvSetting = AKS_CONFIG_FILE ;   // if null/missing then assume this is running in Azure AKS...
    		System.out.println("CONFIG_FILE Env variable not set, so use a default: " + configEnvSetting);
   
    	}  
    	else
    		configFileinProps = true;
	
    	CONFIG_FILE = configEnvSetting;
	
    	
    	return configFileinProps == false;
    	
    	
    }
    
   
    
    public static IgniteConfiguration getIgniteClientConfiguration (String parameters)
    {
    	
    	boolean fromFile = initialize();
    	
    	// have to change this to use bean name
    	IgniteConfiguration config1 = Ignition.loadSpringBean("config/ignite-client.xml", "ignite.cfg");
        
        return config1;
        
    }
    
    
    
}