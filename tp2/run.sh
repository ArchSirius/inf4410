#!/bin/bash
cd bin
if [ "$1" == "server" ]; then
	java -Djava.security.policy=../config/server-policy.txt Server $2 $3
elif [ "$1" == "client" ]; then
	java -Djava.security.policy=../config/server-policy.txt Client
elif [ "$1" == "lb" ]; then
	java -Djava.security.policy=../config/server-policy.txt LoadBalancer
fi
