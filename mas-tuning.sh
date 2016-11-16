export MAVEN_OPTS=-server
nohup mvn exec:java -Dexec.mainClass="com.github.rinde.jaamas16.PerformExperiment" -Dexec.args="-exp vanlon15 \
--show-gui false \
--include c32 -r 1 \
--scenarios.exclude p0 \
--scenarios.add files/tuning-dataset/ \
--scenarios.filter glob:**.scen \
--warmup 30000 \
--ordering SEED_REPS,REPS,SCENARIO,CONFIG" &
