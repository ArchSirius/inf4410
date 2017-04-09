#!/bin/bash

display_usage() {
  echo 'This script is used to monitor the latency of a client using ab from apache.'
  echo -e 'Usage: sh load_test.sh ${server name} http://${hostname}/'
}

if [ $# -ne 2 ]
then
  display_usage
  exit 1
fi

ab -n 40 -c 40 -g $1 $2
