export MAVEN_OPTS=-server
nohup mvn clean compile -U exec:java -Dexec.mainClass="com.github.rinde.jaamas16.PerformExperiment" -Dexec.args="-exp \
vanlon15 \
MAIN_CONFIGS \
--show-gui false \
--repetitions 3 \
--scenarios.filter glob:**.scen \
--warmup 30000 \
--ordering SEED_REPS,REPS,SCENARIO,CONFIG" &
