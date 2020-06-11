# Overview of the com.gg.interactive.* Package

The code in this package runs the ML Pipeline in data generation and Model building steps in the background, and then when you get to the Predictive step will stop and ask you to enter in a number to use for "new" transactions that need to have predictive values assigned. For this first version of the demo it will simply save both the predicted and the "actual" (which in this case is just a hidden value already provided but will be ignored)

NOTE: See comments in org.gridgain.demo.batch programs for ML Pipeline configuration settings 

NOTE: this code produces a single large status file with all of the steps and timestamps saved. It will be overwritten each time you run this unless you change the file prefix in the [<path>/config/MLPLProperties.txt] file


0. [ Step0RunTestCacheNode.java ] You manually start this node as an optional cache server node if you want to run inside your IDE for example. You need this if you do ont have cache servers in the cluster.

1. [ Step1DataProviderNode.java ] You manually starts one instance (only one) to perform the data generation steps. No CSV files are output, just send the data to cache. No data generation will be performed until the applicable method is called on the contained ServiceGrid proxy for data generation services, when you run [A0_Build_Model.java]

NOTE: See comments in org.gridgain.demo.batch programs for ML Pipeline configuration settings 


2. [ Step2RFModelNote.java ]This node is run just once to perform preprocessing and training on the training dataset, that will be performed on demand via the Data provider. No preprocessing or training will be performed until the applicable operations are called on the Service Grid proxy for Model services, when you run [A0_Build_Model.java]

3. [A0_Biuld_Model.java] You run this when the steps0-2 are up and running; at this time this code acts as an orchestrator that calls services for data generation and model building (preprocessing and training)

4. [A1_Perform_Predictions.java] When A0_build model has run and the model service is ready, now you run this to first get some new synthetic transactions (schema of the transactions is defined in the data provider service. 

NOTE: THese are not real transactions coming from an actual client system, rather they are simulated, and have the same output as training data but have the labels "hidden" and ignored at first. The predicted values from the RandomForest model service and the now revealed actual values are saved together and used to calculate the predictive accuracy of the Random Forest model. If the accuracy does not meet the hard-coded MAE then an update on the model is performed with this "new" data


# maven commands to clear out and restart:

cd <path>\ml-demo-interactive    { with pom.xml}
mvn clean install
mvn exec:java -Dexec.mainClass="org.gridgain.demo.interactive.A*.java file you want to run>"

# To run

0. delete the <fileprefix>-interactive-status.txt file if you don't want to keep it growing with each run
 
1. Step0RunTestCacheNode.java  -> start the test cache server in IDE (one or more) if you don't have cache servers up already

2. Step1DataProviderNode.java -> start one node only, can be done in parallel with other service nodes

3. Step2RFModelNode.java -> start one Random Forest model node only, can be done in parallel with steps 0 and 1
 
 
4. A0_Build_Model.java  -> this initializes the training dataset (see properties file) and preprocesses / trains RandomForest model. ONce it completes you can now do predictions with simulated transactions 
 
 5. A1_Perform_Predictions.java  -> makes calls on Model methods to predict outputs for new transactions; you will get a loop statement when running this that asks you to enter one of these inputs at the console command line:
 	a. enter to default to 10 new transactions
 	b. any number you want, say 100, to generate 100 new transactions
 	c. "0" if you want to quit entering new transactions and have process continue on the accuracy and retrain logic
 	
 	
 6. once the transactions are run and predicted upon, the A1_Perform_Prediction.java runs a simple MAE (Mean absolute error) check and if this error exceeds the MAE you hard-coded, then it will send the new transaction cache is input to the RF Model to retrain.

