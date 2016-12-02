export MAVEN_OPTS='-server -Xmx25000m'
nohup mvn clean compile -U exec:java -Dexec.mainClass="com.github.rinde.jaamas17.PerformExperiment" -Dexec.args="-exp \
vanlon15 \
MAS_TUNING_3_REAUCT \
--show-gui false \
--repetitions 1 \
--scenarios.exclude p0 \
--scenarios.add files/tuning-dataset/ \
--scenarios.filter glob:**.scen \
--warmup 30000 \
--ordering SEED_REPS,REPS,SCENARIO,CONFIG" &
