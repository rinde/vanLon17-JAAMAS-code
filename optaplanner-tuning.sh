export MAVEN_OPTS=-server
nohup mvn exec:java -Dexec.mainClass="com.github.rinde.jaamas16.PerformExperiment" -Dexec.args="-exp \
gendreau \
OPTAPLANNER_TUNING \
--show-gui false \
--repetitions 3 \
--scenarios.filter glob:**req_rapide_* \
--warmup 30000 \
--ordering SEED_REPS,REPS,SCENARIO,CONFIG" &
