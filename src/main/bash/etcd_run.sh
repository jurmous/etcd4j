#!/usr/bin/env bash

OPTIND=0

while getopts ":b:" opt; do
  case $opt in
  	b)
      BIND_ADDRESS=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit -1
      ;;
  esac
done
shift $((OPTIND-1))

ETCD_ARGS=( '--name' 'etcd4j.etcd' )

if [ $# -eq 1 ]; then
    ETCD_PATH=$1
    ETCD_ARGS=(${ETCD_ARGS[@]} '--data-dir' "${ETCD_PATH}/etcd4j.etcd")
else
    echo "Installation path is missing"
    exit -1
fi

if [ "${BIND_ADDRESS+x}" ]; then
	ETCD_ARGS=(${ETCD_ARGS[@]} \
	    "-listen-client-urls=http://${BIND_ADDRESS}:2379,http://${BIND_ADDRESS}:4001" \
	    "-advertise-client-urls=http://localhost:2379,http://localhost:4001" )
fi

${ETCD_PATH}/etcd ${ETCD_ARGS[@]}
