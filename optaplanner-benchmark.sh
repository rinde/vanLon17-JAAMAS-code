export MAVEN_OPTS=-server
nohup mvn exec:java -Dexec.mainClass="com.github.rinde.jaamas16.PerformExperiment" -Dexec.args="-exp gendreau -g false -t 11 -i c4,..,c32 -r 3 -sf glob:**req_rapide_* -w 30000 -o SEED_REPS,REPS,SCENARIO,CONFIG" &
