#!/bin/bash
sudo apt-get update

# install etcd from a github tag.
/vagrant/src/main/bash/etcd_install.sh "/opt/etcd-v2.2.5-linux-amd64"

sudo cp /vagrant/src/integration/vagrant/etcd.conf /etc/init
sudo service etcd start

