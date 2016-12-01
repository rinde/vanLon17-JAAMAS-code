# When do agents outperform centralized algorithms? - A systematic empirical evaluation in logistics

This repository contains the code that was used to perform the experiments described in:

 > *When do agents outperform centralized algorithms? - A systematic empirical evaluation in logistics.* Rinde R.S. van Lon and Tom Holvoet. Journal of Autonomous Agents and Multi-Agent Systems.

## Overview

Since the paper is part of a long term research effort, the code used for the experiments is distributed over several open source repositories. The code in the current repository is the glue that instantiates and binds the code from the other repositories to create a cohesive experiment setup.

### In this repository

This repository contains several scripts that can be used to execute each experiment conducted for this paper: 

| Purpose of script        							| Command |
| ------------- 									|-------------| 
| Generate main dataset     						| ```./generate-main-dataset.sh``` | 
| Perform main experiment    						| ```./main-experiment.sh``` |
| Real-time reliability (sensitivity) experiment 	| ```./sensitivity-experiment.sh``` |
| Generate MAS tuning dataset 						|  ```./generate-mas-tuning-dataset.sh``` | 
| MAS tuning part 1 								| ```./mas-tuning1.sh``` |
| MAS tuning part 2 								| ```./mas-tuning2.sh``` |
| MAS tuning part 3 								| ```./mas-tuning3.sh``` |
| OptaPlanner tuning on Gendreau dataset 			| ```./optaplanner-tuning.sh``` |


One can also use the generated files as described in the next section.

### Dependencies


### How to run
