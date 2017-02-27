#!/bin/bash
if [ "$1" == "server" ]; then
	cd src
	javac -d ../bin Server.java
elif [ "$1" == "client" ]; then
	cd src
	javac -d ../bin Client.java
elif [ "$1" == "lb" ]; then
	cd src
	javac -d ../bin LoadBalancer.java
elif [ "$1" == "clean" ]; then
	rm bin/*.class
else
	cd src
	javac -d ../bin *.java
fi
