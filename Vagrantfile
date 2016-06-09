# -*- mode: ruby -*-
# vi: set ft=ruby :

VAGRANTFILE_API_VERSION = 2

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "hashicorp/precise32"
  config.vm.provider :virtualbox do |vb|
    vb.customize ["modifyvm", :id, "--memory", "512"]
  end

  config.vm.provision :chef_solo do |chef|
    chef.cookbooks_path = ["cookbooks", "site-cookbooks"]
    chef.add_recipe "java"

    # structure of chef json with example values
    chef.json = { java: { jdk_version: '7' } }
  end

  config.vm.define "master" do |master|
    master.vm.network "public_network", ip: "192.168.0.30"
  end

  # This would be N times
  1.times do |n|
    config.vm.define "node#{n+1}" do |node|
      node.vm.network "public_network", ip: "192.168.0.#{31 + n}"
    end
  end
end