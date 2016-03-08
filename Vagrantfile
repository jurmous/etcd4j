# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.provider "virtualbox" do |v|
    v.memory = 3072
  end
  config.vm.network :forwarded_port, guest: 4001, host: 4001
  config.vm.network :forwarded_port, guest: 2379, host: 2379
  config.vm.provision :shell, :path => "src/integration/vagrant/provision.sh"
end
