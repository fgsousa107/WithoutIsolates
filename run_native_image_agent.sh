gradle clean jar
$GRAALVM_HOME/bin/java -agentlib:native-image-agent=config-output-dir=config-dir/ -jar build/libs/WithoutIsolates-1.0-SNAPSHOT.jar
