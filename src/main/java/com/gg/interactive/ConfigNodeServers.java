package com.gg.interactive;
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