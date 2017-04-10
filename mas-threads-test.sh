export MAVEN_OPTS='-server -Xmx25000m'
nohup mvn clean compile -U exec:java -Dexec.mainClass="com.github.rinde.jaamas17.PerformExperiment" -Dexec.args="-exp vanlon15 \
THREADS_TEST \
--show-gui false \
--repetitions 3 \
--scenarios.filter glob:**0.50-20-10.00-[1-5].scen \
--warmup 30000 \
--ordering SEED_REPS,REPS,SCENARIO,CONFIG" &
