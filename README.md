# When do agents outperform centralized algorithms? - A systematic empirical evaluation in logistics

This repository contains the code that was used to perform the experiments described in:

 > *When do agents outperform centralized algorithms? - A systematic empirical evaluation in logistics.* Rinde R.S. van Lon and Tom Holvoet. Journal of Autonomous Agents and Multi-Agent Systems.

## Overview

Since the paper is part of a long term research effort, the code used for the experiments is distributed over several open source repositories. The code in the current repository is the glue that instantiates and binds the code from the other repositories to create a cohesive experiment setup.

### In this repository

This repository contains several scripts that can be used to execute each experiment conducted for this paper: 

| Purpose of script        							| Prerequisites 																		| Command 
| ------------- 									| -------------																			| -------------
| Generate main dataset     						| 																						| ```./generate-main-dataset.sh``` 
| Perform main experiment    						| Requires the main dataset, either generate it or download it here ..					| ```./main-experiment.sh``` 
| Real-time reliability (sensitivity) experiment 	| Requires the main dataset, either generate it or download it here ..					| ```./sensitivity-experiment.sh``` 
| Generate MAS tuning dataset 						|																						| ```./generate-mas-tuning-dataset.sh``` 
| MAS tuning part 1 								| Requires the MAS tuning dataset, either generate it, or download it here ..			| ```./mas-tuning1.sh``` 
| MAS tuning part 2 								| Requires the MAS tuning dataset, either generate it, or download it here ..			| ```./mas-tuning2.sh``` 
| MAS tuning part 3 								| Requires the MAS tuning dataset, either generate it, or download it here ..			| ```./mas-tuning3.sh``` 
| OptaPlanner tuning on Gendreau dataset 			| Requires the Gendreau dataset, download it here ..									| ```./optaplanner-tuning.sh``` 



### Dependencies

| Library										| Description																									| Version		| DOI
| -------------									| ------------- 																								| ------------- | -------------
| [RinSim](https://github.com/rinde/RinSim)		| Real-time logistics simulator																					| 4.3.0		    |
| [RinLog](https://github.com/rinde/RinLog)		| Collection of algorithms, including DynCNET multi-agent system and OptaPlanner for dynamic PDPTW problems		| 3.2.0         |


### How to run
