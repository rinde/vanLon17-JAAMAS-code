export MAVEN_OPTS=-server
nohup mvn clean compile -U exec:java -Dexec.mainClass="com.github.rinde.jaamas16.PerformExperiment" -Dexec.args="-exp \
time_deviation \
OPTAPLANNER_SENSITIVITY \
--show-gui false \
--repetitions 1 \
--seed-repetitions 10 \
--warmup 30000 \
--scenarios.filter glob:**0.50-20-10.00-0.scen \
--ordering SEED_REPS,REPS,SCENARIO,CONFIG" &
