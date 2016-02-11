#!/usr/bin/env bash

if [ $# -eq 1 ]; then
    ETCD_PATH=$1

    ${ETCD_PATH}/etcd \
        --name 'etcd4j.etcd' \
        --data-dir ${ETCD_PATH}/etcd4j.etcd &
else
    echo "Installation path is missing"
    exit -1
fi
