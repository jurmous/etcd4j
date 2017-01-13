#!/usr/bin/env bash

DEFAULT_ETCD_VERSION="2.3.7"

function usage {
    cat <<-END >&2
    USAGE: $0 [-p ETCD_PATH] [-v ETCD_VERSION]
            -p ETCD_PATH     # Etcd installation path
            -v ETCD_VERSION  # Etcd version, default ${DEFAULT_ETCD_VERSION}
            -h               # this usage message
END
    exit
}

################################################################################
#
################################################################################

while getopts p:v:h opt
do
    case $opt in
    p)  export ETCD_PATH=$OPTARG ;;
    v)  export ETCD_VERSION=$OPTARG ;;  
    h|?) usage ;;
    esac
done

shift $(( $OPTIND - 1 ))

if [[ ! $ETCD_PATH ]]; then 
    echo "Warning, ETCD_PATH is required"
    usage
fi

if [[ ! $ETCD_VERSION ]]; then 
   ETCD_VERSION=${DEFAULT_ETCD_VERSION}
fi

################################################################################
#
################################################################################

ETCD_URL="https://github.com/coreos/etcd/releases/download"
ETCD_ARC="linux-amd64"

# cleanup
rm -rf ${ETCD_PATH}/etcd-v${ETCD_VERSION}-${ETCD_ARC}.tar.gz
rm -rf ${ETCD_PATH}/etcd-v${ETCD_VERSION}-${ETCD_ARC}
rm -rf ${ETCD_PATH}/etcd
rm -rf ${ETCD_PATH}/default.etcd

if [ ! -d ${ETCD_PATH} ]; then
    mkdir ${ETCD_PATH}
fi

# install etcd
curl -L ${ETCD_URL}/v${ETCD_VERSION}/etcd-v${ETCD_VERSION}-${ETCD_ARC}.tar.gz \
    | tar xzf - \
        --directory ${ETCD_PATH} \
        --strip-components=1

# check
${ETCD_PATH}/etcd --version
