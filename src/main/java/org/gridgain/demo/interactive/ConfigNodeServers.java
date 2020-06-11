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

package org.gridgain.demo.interactive;
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

// Note this class only called when you start a local TEST_SERVER

public class ConfigNodeServers {
    public static  	String 	SERVER_CONFIG_FILE = "ignite-server.xml";	// client config uses local IP finder
    
    public static String initialize () {  // this is used for Cache servers; ServerGrid server nodes see below..
    	
    	String configEnvSetting = SERVER_CONFIG_FILE;
    

    	return configEnvSetting;
    	
    }
    
    // unused
    public static IgniteConfiguration getIgniteServiceNodeConfiguration (String parameters)
    {
    	
    	
    	
    	// this config needs to be redirected if deployed to Kubernetes
    	IgniteConfiguration config1 = Ignition.loadSpringBean("config/ignite-server.xml", "igniteServiceNode.cfg");
        
        return config1;
        
    }
    
    public static IgniteConfiguration getIgniteModelNodeConfiguration (String parameters)
    {
    	
    	
    	
    	// unused - this config needs to be redirected if deployed to Kubernetes
    	IgniteConfiguration config1 = Ignition.loadSpringBean("config/ignite-server.xml", "igniteModelNode.cfg");
        
        return config1;
        
    }
    
    public static IgniteConfiguration getIgniteCacheServerConfiguration (String parameters)
    {
    	
    	
    	
    	// this config needs to be redirected if deployed to Kubernetes
    	IgniteConfiguration config1 = Ignition.loadSpringBean("config/ignite-server.xml", "igniteCacheServer.cfg");
        
        return config1;
        
    }
    
    
}