# ignite-machine-learning-demo
A set of examples demonstrating Ignite Machine Learning capabilities. 

# Business use case
Claims data transactions (simulated to model "typical" healthcare payers input fields) each require an expected monetary value be calculated for each claims transaction. The value is based on reimbursement amount plus the expected likelihood of obtaining that reimbursement amount in a timely manner. For an example of how the predicted monetary value could be used, an example would be that the claims could be prioritized in a back end  claims adjustment workflow, so that the most valuable claims from a reimbursement view point could be procesed more quickly. In this way the maximum financial value could be obtained from the claims adjudication process. The simulated claims training data, which can generated in bulk (you can set the # of records to create benchmarking jobs that run on Apache Ignite clusters), makes use of a statistical regression algorithm in order to (A) simulate a Random Forest parallel preprocessing and training workload, and then (B) make use of the trained Random Forest model decision tree to return the expected value of new transaction in realtime as the transaction arrives.

# Technical rationale for this demo

If you've worked before with ML training and preprocessing, you know that preprocessing and training an ML model is a highly iterative process. This demo package makes lots of (over) simplifying assumptions about the training and evaluation process, and so focuses more on an example of how the various Apache Ignite ML components can be orchestrated together as key members of a more comprehensive enterprise ML ecosystem. For example, training takes lots of data, and the 1000 or so records that we use here as a default (in order to more quickly run the training stage) would normally need a lot more training data. In addition, the code shows how to do predictions, evaluation of prediction accuracy, and even retraining of a model but in the real world the evaluation and retraining would take a lot more "think time" and detailed evaluation by a data science team. The best way to think about Apache Ignite is as the high performance massively processing platform, or "ML pipeline chassis" if you will, that works with, and greatly accelerates, some of the time consuming aspects of Data science workflows by providing massively parallel preprocessing, training, and predictive transaction processing to the current ecosystem of "ML Ops" workbenches and user-oriented analytics tools. For example, Gridgain offers a Python package that, used in conjunction with Apache Ignite ML libraries and cache servers, can speed up the manual data analysis that needs to be performed in between the ML pipeline steps. 

There are 2 packages:

1. org.gridgain.demo.interactive.* - this package runs the ML model preprocess and training, using generated data. Then you run a second program that acts like an actual client transaction engine sending in new transactions that request predictions as outputs from the trained model, the latter which runs as a service and can be shared by multiple clients.  

2. org.gridgain.demo.batch - this one runs the entire ML Pipeline as a single run from start (generating the data) to the finish (predicting some outputs for new transactions). 

3. At the end of this README in the Configuration Details section there are some configuration options, for example to change the number or rows generated, or the data spit between train and test, that you can review once you are familiar with how to run the project.


# PACKAGE 1: Overview of the org.gridgain.demo.interactive.* Package

The code in this package runs the ML Pipeline in data generation and Model building steps in the background, and then when you get to the Predictive step will stop and ask you to enter in a number to use for "new" transactions that need to have predictive values assigned. For this first version of the demo it will simply save both the predicted and the "actual" (which in this case is just a hidden value already provided but will be ignored)

NOTE: this code will output a single large status file with all of the steps and timestamps saved. The file prefix can be specified in the [config/MLPLProperties.txt] file


0. [ Step0RunTestCacheNode.java ] You manually start this node as an optional cache server node if you want to run inside your IDE for example. You may want to run this this when you want to test out the ML pipeline within your IDE, and don't have access to an existing cache cluster in which to deploy your training dataset.

1. [ Step1DataProviderNode.java ] You manually starts one instance (only one) to perform the data generation steps. No CSV files are output, just send the data to cache. No data generation will be performed until the applicable method is called on the contained ServiceGrid proxy for data generation services, when you run [A0_Build_Model.java]

2. [ Step2RFModelNote.java ]This node is run just once to perform preprocessing and training on the training dataset, that will be performed on demand via the Data provider. No preprocessing or training will be performed until the applicable operations are called on the Service Grid proxy for Model services, when you run [A0_Build_Model.java]

3. [A0_Biuld_Model.java] You run this when the steps0-2 are up and running; at this time this code acts as an orchestrator that calls services for data generation and model building (preprocessing and training)

4. [A1_Perform_Predictions.java] When A0_build model has run and the model service is ready, now you run this to first get some new synthetic transactions (schema of the transactions is defined in the data provider service. 

NOTE: These are not real transactions coming from an actual client system, rather they are simulated, and have the same output as training data but have the labels "hidden" and ignored at first. The predicted values from the RandomForest model service and the now revealed actual values are saved together and used to calculate the predictive accuracy of the Random Forest model. If the accuracy does not meet the hard-coded MAE then an update on the model is performed with this "new" data


# maven commands to clear out and restart:

cd <path>\ml-demo-interactive    { with pom.xml}
mvn clean install
mvn exec:java -Dexec.mainClass="org.gridgain.demo.interactive.<java file you want to run> - there are 3 services and 2 programs you need to run, see below "To Run" instructions

# To run

0. Rename the {your file prefix}-interactive-status.txt file if you want to save the output separately for each different run. You may need to delete it to save space.
 
1. Step0RunTestCacheNode.java  -> start the test cache server in IDE (one or more) if you don't have cache servers up already

2. Step1DataProviderNode.java -> start one node only, can be done in parallel with other service nodes

3. Step2RFModelNode.java -> start one Random Forest model node only, can be done in parallel with steps 0 and 1
 
 
4. A0_Build_Model.java  -> this initializes the training dataset (see properties file) and preprocesses / trains RandomForest model. ONce it completes you can now do predictions with simulated transactions 
 
 5. A1_Perform_Predictions.java  -> makes calls on Model methods to predict outputs for new transactions; you will get a loop statement when running this that asks you to enter one of these inputs at the console command line:
 	a. enter to default to 10 new transactions
 	b. any number you want, say 100, to generate 100 new transactions
 	c. "0" if you want to quit entering new transactions and have process continue on the accuracy and retrain logic
 	
 	
 6. once the transactions are run and predicted upon, the A1_Perform_Prediction.java runs a simple MAE (Mean absolute error) check and if this error exceeds the MAE you hard-coded, then it will send the new transaction cache is input to the RF Model to retrain.

# PACKAGE 2: Overview of the  org.gridgain.demo.batch package

The code in this package runs the entire RandomForest pipeline in one straight-through process batch run. It performs these steps in order

0. "Step0" just starts an optional cache server node if you want to run inside your IDE for example. 

1. Generates sample training dataset of configurable size (based on claims processing data)

NOTE: Writes training dataset to a CSV file (this should be turned off if you plan to generate a large dataset). This is if you want to use the CSV for other training set inputs, not needed here since the generated dataset is fed directly into a cache. NOTE: the CSV loader is not yet implemented in this code.

2. This training dataset is used for Preprocessing into a vectorized dataset and then sent to the Random Forest training model. 

3. A simple batch-oriented prediction cycle is run, first to get the predicted value from the Random Forest model, then to save both the predicted and actual values in a map.

NOTE: the same datagenerator is used to create the "live" transactions, so the label is just ignored and then called up later to simulate an actual output value. 

4. A process compares the Actuals to the Predictions and if a target MAE (mean absolute error) is exceeded, will trigger an update to the Random Forest model and then point to the latest version.
	

  # Maven commands to manually clear out and restart:

	cd <path>\ml-demo-interactive    { directory with pom.xml}
	mvn clean install
	mvn exec:java -Dexec.mainClass="org.gridgain.demo.batch.A0_Run_Steps0to5.java

  # How to run the steps
  
(1) programs in package "org.gridgain.demo.batch" starting with name A0* are parent programs that call the individual pipeline steps.
	
  A0ExecSteps0_5.java 
  -----------------
  calls Steps0-5 to start cache server first, then on to generate synthetic data, preprocess, train, and then do  predictions
	
  A0ExecSteps1_5.java 
  -----------------
  assumes you have already started cache cluster, so no cache server started. Moves directly to generate does synthetic data, preprocess, train, then do  predictions
	
	


(2) Optional Thin client UI

	Step9RemoteUI.java
	-----------------------
	optional thin client connect to cache when done to see # entries. Edit if you want to connect remotely to Kubernetes cluster, right now it connects to localhost

(3) Step0RunTestCacheNode  is the test server

	(not used when you already have started cache servers externally)
	
# Configuration Details (for both batch and interactive projects)
(Can use any of these approaches to change settings: MLPLProperties.txt, Environment variables, or change in ConfigPipeLineSettings.java) 

  Properties File <path>/config/MLPLProperties.txt : 
	 ROWS=1212
	 CONFIG_FILE=ignite-client.xml
	 DATA_SPLIT=0.78
	 OUTPUT_DIR=
	 TEST_SERVER=
	 FILEPREFIX=200608-5-23


  Environment  variables :  
		CONFIG_FILE=ignite-client.xml <only used for local test, blank otherwise>
		ROWS=1234  <data rows to be generated, if not set then default used>
		DATA_SPLIT=.75   <if not set then default used, has to be >0.0 AND <1.0>
		
