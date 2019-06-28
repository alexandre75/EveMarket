#!/bin/sh
LIB=/usr/share/java
export CLASSPATH=$LIB/guice.jar:$LIB/guava.jar:$LIB/mongo-java-driver.jar:$LIB/javax.json-1.1.4.jar:$LIB/reactive-streams-flow-adapters-1.0.2.jar:$LIB/rxjava-2.2.9.jar:$LIB/gson.jar:$LIB/atinject-jsr330-api.jar:$LIB/aopalliance.jar:$LIB/amqp-client.jar:$LIB/reactive-streams-1.0.2.jar:$LIB/slf4j-api.jar
export CLASSPATH=$LIB/EveMarket-0.0.2-RELEASE.jar:$CLASSPATH
#CLASSPATH=./build/libs/EveMarket-0.0.1-SNAPSHOT.jar:$CLASSPATH
java lan.groland.eve.bootstrap.EveMarket
