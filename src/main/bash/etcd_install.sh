#!/usr/bin/env bash

if [ $# -eq 1 ]; then

    ETCD_PATH=$1
    ETCD_URL="https://github.com/coreos/etcd/releases/download"
    ETCD_VER="v2.2.4"
    ETCD_ARC="linux-amd64"

    # cleanup
    rm -rf ${ETCD_PATH}/etcd-${ETCD_VER}-${ETCD_ARC}.tar.gz
    rm -rf ${ETCD_PATH}/etcd-${ETCD_VER}-${ETCD_ARC}
    rm -rf ${ETCD_PATH}/etcd
    rm -rf ${ETCD_PATH}/default.etcd

    if [ ! -d ${ETCD_PATH} ]; then
        mkdir ${ETCD_PATH}
    fi

    # install etcd
    curl -L ${ETCD_URL}/${ETCD_VER}/etcd-${ETCD_VER}-${ETCD_ARC}.tar.gz \
        | tar xzf - \
            --directory ${ETCD_PATH} \
            --strip-components=1

    # check
    ${ETCD_PATH}/etcd --version
else
    echo "Installation path is missing"
    exit -1
fi

