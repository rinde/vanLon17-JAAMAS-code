export MAVEN_OPTS=-server
nohup mvn exec:java -Dexec.mainClass="com.github.rinde.jaamas16.PerformExperiment" -Dexec.args="-exp gendreau_offline -g false -i c78 -t 1 -r 3 -sf glob:**req_rapide_* -w 0 -jppf" &
