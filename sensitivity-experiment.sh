export MAVEN_OPTS=-server
nohup mvn exec:java -Dexec.mainClass="com.github.rinde.jaamas16.PerformExperiment" -Dexec.args="-exp time_deviation -g false -i c43 -t 11 -r 1 -w 30000 -o SEED_REPS,REPS,SCENARIO,CONFIG" &
