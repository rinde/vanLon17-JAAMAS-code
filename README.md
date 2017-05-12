# When do agents outperform centralized algorithms? - A systematic empirical evaluation in logistics

This repository contains the code that was used to perform the experiments described in:

 > *When do agents outperform centralized algorithms? - A systematic empirical evaluation in logistics.* Rinde R.S. van Lon and Tom Holvoet. Journal of Autonomous Agents and Multi-Agent Systems (2017).

This is version v1.1.0.

The datasets and results belonging to this paper can be found in one big zip file, at this location: [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.576345.svg)](https://doi.org/10.5281/zenodo.576345)

## Overview

Since the paper is part of a long term research effort, the code used for the experiments is distributed over several open source repositories. The code in the current repository is the glue that instantiates and binds the code from the other repositories to create a cohesive experiment setup.

### In this repository

This repository contains several scripts that can be used to execute each experiment conducted for this paper. Maven and Java 7 (or higher) are required.

| Purpose of script        							| Prerequisites 																		| Command 
| ------------- 									| -------------																			| -------------
| Generate main dataset     						| 																						| ```./generate-main-dataset.sh``` 
| Perform main experiment    						| Requires the main dataset, either generate it or use the already generated files from the zip: ```scenarios/vanLonHolvoet15-adapted-to-4-hours/```					| ```./main-experiment.sh``` 
| Real-time reliability (sensitivity) experiment 	| Requires the main dataset, either generate it or use the already generated files from the zip: ```scenarios/vanLonHolvoet15-adapted-to-4-hours/```					| ```./sensitivity-experiment.sh``` 
| Generate MAS tuning dataset 						|																						| ```./generate-mas-tuning-dataset.sh``` 
| MAS tuning part 1 								| Requires the MAS tuning dataset, either generate it or use the already generated files from the zip: ```scenarios/mas-tuning-dataset/```		| ```./mas-tuning1.sh``` 
| MAS tuning part 2 								| Requires the MAS tuning dataset, either generate it or use the already generated files from the zip: ```scenarios/mas-tuning-dataset/```		| ```./mas-tuning1.sh``` 			| ```./mas-tuning2.sh``` 
| MAS tuning part 3 								| Requires the MAS tuning dataset, either generate it or use the already generated files from the zip: ```scenarios/mas-tuning-dataset/```		| ```./mas-tuning1.sh``` 			| ```./mas-tuning3.sh``` 
| OptaPlanner tuning on Gendreau dataset 			| Requires the Gendreau dataset, the files can be found in the zip: ```scenarios/gendreau2006/```								| ```./optaplanner-tuning.sh``` 

### Dependencies

All dependencies are imported via Maven but can also be downloaded manually. The following dependencies are especially relevant:

| Library										| Description																									| Version		| DOI
| -------------									| ------------- 																								| ------------- | -------------
| [RinSim](https://github.com/rinde/RinSim)		| Real-time logistics simulator.																				| [4.3.0](https://github.com/rinde/RinSim/releases/tag/v4.3.0)		    | [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.192106.svg)](https://doi.org/10.5281/zenodo.192106)
| [RinLog](https://github.com/rinde/RinLog)		| Collection of algorithms, including DynCNET multi-agent system and OptaPlanner for dynamic PDPTW problems.	| [3.2.2](https://github.com/rinde/RinLog/releases/tag/v3.2.2)         | [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.571180.svg)](https://doi.org/10.5281/zenodo.571180)
| [PDPTW Dataset Generator](https://github.com/rinde/pdptw-dataset-generator)	| Generator of PDPTW datasets.													| [1.1.0](https://github.com/rinde/pdptw-dataset-generator/releases/tag/v1.1.0)			| [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.59259.svg)](https://doi.org/10.5281/zenodo.59259)


### License

All files in this repository are licensed under the [Apache 2.0 license](LICENSE).
