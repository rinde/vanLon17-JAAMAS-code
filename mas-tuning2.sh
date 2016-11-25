export MAVEN_OPTS='-server -Xmx25g'
nohup mvn clean compile -U exec:java -Dexec.mainClass="com.github.rinde.jaamas16.PerformExperiment" -Dexec.args="-exp \
vanlon15 \
MAS_TUNING_RP_AND_B_MS \
--show-gui false \
--repetitions 1 \
--scenarios.exclude p0 \
--scenarios.add files/tuning-dataset/ \
--scenarios.filter glob:**.scen \
--warmup 30000 \
--ordering SEED_REPS,REPS,SCENARIO,CONFIG" &
