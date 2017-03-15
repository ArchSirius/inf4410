#!/bin/bash
ROOT_DIRECTORY=$(pwd)
if [ "$1" == "server" ]; then
	cd src
	javac -d $ROOT_DIRECTORY/bin Server/Server.java
elif [ "$1" == "client" ]; then
	cd src
	javac -d $ROOT_DIRECTORY/bin Client/Client.java
elif [ "$1" == "lb" ]; then
	cd src
	javac -d $ROOT_DIRECTORY/bin LoadBalancer/LoadBalancer.java
elif [ "$1" == "clean" ]; then
	rm bin/*/*.class
	rmdir bin/*
else
	cd src
	javac -d $ROOT_DIRECTORY/bin */*.java
fi
