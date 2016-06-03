export MAVEN_OPTS=-server
nohup mvn exec:java -Dexec.mainClass="com.github.rinde.jaamas16.PerformExperiment" -Dexec.args="-exp time_deviation -g false -i c14,c55 -t 11 -r 1 -seed-repetitions 100 -w 30000 -sf glob:**0.50-20-10.00-0.scen -o SEED_REPS,REPS,SCENARIO,CONFIG" &
