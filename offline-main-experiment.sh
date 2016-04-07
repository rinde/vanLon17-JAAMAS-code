export MAVEN_OPTS=-server
nohup mvn exec:java -Dexec.mainClass="com.github.rinde.jaamas16.PerformExperiment" -Dexec.args="-exp vanlon15_offline -g false -i c77 -t 16 -r 3 -sf glob:**[0-9].scen -w 0" &
