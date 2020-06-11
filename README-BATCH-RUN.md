# Overview of the  [org.gridgain.demo.batch] package
-------------------------

Business use case: Claims data transactions (simulated to model "typical" healthcare payers input fields) each require an expected monetary value be calculated for each claims transaction, based on expected likelihood of obtaining reimbursement in a timely manner. The claims are then prioritized in the claims adjustment workflow accordingly so that maximum financial value can be captured during the claims adjudication process. The data which is generated (you can set the # of records to create benchmarking jobs that run on Apache Ignite clusters) makes use of a statistical regression algorithm in order to (A) simulate a Random Forest parallel preprocessing and training workload, and then (B) make use of the trained model to ascertain the expected value of new transactions.


The code in this package runs the entire RandomForest pipeline in one straight-through process batch run. It performs these steps in order

0. "Step0" just starts an optional cache server node if you want to run inside your IDE for example. 

1. Generates sample training dataset of configurable size (based on claims processing data)

NOTE: Writes training dataset to a CSV file (this should be turned off if you plan to generate a large dataset). This is if you want to use the CSV for other training set inputs, not needed here since the generated dataset is fed directly into a cache. NOTE: the CSV loader is not yet implemented in this code.

2. This training dataset is used for Preprocessing into a vectorized dataset and then sent to the Random Forest training model. 

3. A simple batch-oriented prediction cycle is run, first to get the predicted value from the Random Forest model, then to save both the predicted and actual values in a map.

NOTE: the same datagenerator is used to create the "live" transactions, so the label is just ignored and then called up later to simulate an actual output value. 

4. A process compares the Actuals to the Predictions and if a target MAE (mean absolute error) is exceeded, will trigger an update to the Random Forest model and then point to the latest version.
	

Maven commands to manually clear out and restart:
------
	cd <path>\ml-demo-interactive    { directory with pom.xml}
	mvn clean install
	mvn exec:java -Dexec.mainClass="org.gridgain.demo.batch.A0_Run_Steps0to5.java

How to run the steps
-------------------
	(1) programs in package "org.gridgain.demo.batch" starting with name A0* are parent programs that call the individual pipeline steps.
	

	 	A0ExecSteps0_5.java 
	 	-----------------
	 	calls Steps0-5 to start cache server, generate synthetic data, preprocess, train, then do  predictions
	
		 A0ExecSteps1_5.java 
	 	-----------------
	 	assumes you have already started cache cluster, then generate does synthetic data, preprocess, train, then do  predictions
	
	


	(2) Optional Thin client UI

		Step9RemoteUI.java
		-----------------------
		optional thin client connect to cache when done to see # entries. Edit if you want to connect remotely to Kubernetes cluster, right now localhost

	(3) Step0RunTestCacheNode  is the test server

		not used when you already have started cache servers externally
	
----- Configuration Details ------
-----------------------------------
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
		
