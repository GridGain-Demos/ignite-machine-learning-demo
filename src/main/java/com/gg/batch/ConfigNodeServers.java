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

// Note this class only called when you start a local TEST_SERVER

public class ConfigNodeServers {
    public static  	String 	SERVER_CONFIG_FILE = "ignite-server.xml";	// client config uses local IP finder
    
    public static String initialize () {
    	
    	String configEnvSetting = SERVER_CONFIG_FILE;
    

    	return configEnvSetting;
    	
    }
    
    
    
}