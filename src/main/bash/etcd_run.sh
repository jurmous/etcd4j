#!/usr/bin/env bash

OPTIND=0

while getopts ":db:p:" opt; do
  case $opt in
  	b)
      BIND_ADDRESS=$OPTARG
      ;;
    d)
      DAEMON="true"
      ;;
    p)  
      ETCD_PATH=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit -1
      ;;
  esac
done
shift $((OPTIND-1))

if [ ! "${ETCD_PATH+x}" ]; then
  echo "Missing ETCD_PATH"
  exit -1
fi

ETCD_ARGS=(\
  '--name' \
  'etcd4j.etcd' \
  '--data-dir' \ 
  "${ETCD_PATH}/etcd4j.etcd"\
  )

if [ "${BIND_ADDRESS+x}" ]; then
	ETCD_ARGS=(${ETCD_ARGS[@]} \
	    "-listen-client-urls=http://${BIND_ADDRESS}:2379,http://${BIND_ADDRESS}:4001" \
	    "-advertise-client-urls=http://localhost:2379,http://localhost:4001" )
fi

if [  "${DAEMON+x}" ]; then
  ${ETCD_PATH}/etcd ${ETCD_ARGS[@]} > ${ETCD_PATH}/etcd.log 2>&1 &
else
  ${ETCD_PATH}/etcd ${ETCD_ARGS[@]}
fi
