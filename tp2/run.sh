#!/bin/bash
basepath=$(pwd)
cd bin
if [ "$1" == "server" ]; then
  java -Djava.security.policy="$basepath"/policy Server $2 $3
elif [ "$1" == "client" ]; then
  java -Djava.security.policy="$basepath"/policy Client $2 $3
elif [ "$1" == "lb" ]; then
  java -Djava.security.policy="$basepath"/policy LoadBalancer
fi
