package com.gg.batch;
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
    	
    	// have to change this to 
    	IgniteConfiguration config1 = Ignition.loadSpringBean("config/ignite-client.xml", "ignite.cfg");
        
        return config1;
        
    }
    
    
    
}