/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gridgain.demo.interactive;

import java.util.Scanner;  // Import the Scanner class




import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
// import org.apache.ignite.examples.model.Address;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.client.ClientException;



/**
 * Demonstrates how to use Ignite thin client for basic put/get cache operations.
 *
 */
public class Step9RemoteUI {
    /** Entry point. */
    public static void main(String[] args) {
        
    	// make sure to activate the remote cluster first if it is persistent....
    	// login to one of the cache nodes and:   $IGNITE_HOME/bin/control.sh --activate 
       // ClientConfiguration cfg = new ClientConfiguration().setAddresses("52.149.207.252:10800");
    	
    	System.out.println(">> Make sure you have activated remote cluster first if persistent for example: " 
    			+ "\nbash-4.4$ $IGNITE/bin/control.sh --activate");

    	Scanner myObj = new Scanner(System.in);  // Create a Scanner object
        System.out.print("remote Host:port  endpoint: (<CR> for  default locahost:10800)\n>>");

        String remoteEndPoint = myObj.nextLine();  // Read user input
        
        
        if (remoteEndPoint.length() == 0)
        	remoteEndPoint = "localhost:10800";
        
        System.out.println("remoteEndPoint IP:port= " + remoteEndPoint);  // Output user input
        
        // String remotehost = "localhost"; // where cluster / load-balancer is running in Kubernetes
       

        
        ClientConfiguration cfg = new ClientConfiguration().setAddresses(remoteEndPoint);

        
        try (IgniteClient igniteClient = Ignition.startClient(cfg)) {
            
        	ClientCache<Integer, Vector> trainingData = null;
        	ClientCache<Integer, Vector> opsData = null;

            try {
            	trainingData = igniteClient.getOrCreateCache("TrainingDataSet");
                System.out.println("Connecting to the Training  data set: " + trainingData.getName() + 
                		"# records: " + trainingData.size());
                for (int key =1; key < 10; key++) {
                	Vector value = trainingData.get(key);
                	
                	// label column is column 15
                	System.out.println("cols / key / output label  = " + "< " + value.size() + " / " + key + " / " +  value.get(15) + " > ");
                }
            }
            catch (Exception e)
            {
            	System.out.println("Exception during Training cache connection attempt");
            }
            
            try {
            	trainingData = igniteClient.getOrCreateCache("OperationalDataSet");
                System.out.println("Connecting to the Training  data set: " + opsData.getName() + 
                		"# records: " + opsData.size());
                for (int key =1; key < 10; key++) {
                	Vector value = opsData.get(key);
                	
                	// label column is column 15
                	System.out.println("cols / key / output label  = " + "< " + value.size() + " / " + key + " / " +  value.get(15) + " > ");
                }
            }
            catch (Exception e)
            {
            	System.out.println("Exception during Online OLTP cache connection attempt");
            }

            
          
            


 
         
  //          Address val = new Address("1545 Jackson Street", 94612);

    //        cache.put(key, val);

      //      System.out.format(">>> Saved [%s] in the cache.\n", val);

        //    Address cachedVal = cache.get(key);

          //  System.out.format(">>> Loaded [%s] from the cache.\n", cachedVal);
            
        }
        catch (ClientException e) {
            System.err.println(e.getMessage());
        }
        catch (Exception e) {
            System.err.format("Unexpected failure: %s\n", e);
        }
    }
}
