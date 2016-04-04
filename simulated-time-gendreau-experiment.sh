export MAVEN_OPTS=-server
nohup mvn exec:java -Dexec.mainClass="com.github.rinde.jaamas16.PerformExperiment" -Dexec.args="-exp gendreau_simulated -g false -i c78 -t 16 -r 5 -sf glob:**req_rapide_* -w 0" &
