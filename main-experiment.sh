export MAVEN_OPTS=-server
nohup mvn exec:java -Dexec.mainClass="com.github.rinde.jaamas16.PerformExperiment" -Dexec.args="-exp vanlon15 -g false -i c30,c32 -t 11 -r 3 -sf glob:**[0-9].scen -w 30000 -o SEED_REPS,REPS,SCENARIO,CONFIG" &
