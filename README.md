[![Build Status](https://travis-ci.org/cparram/ap-networking-proxy-cache.svg?branch=staging)](https://travis-ci.org/cparram/ap-networking-proxy-cache)

# ap-networking-proxy-cache
Project with Academic Purpose about Proxy cache implementation.

## Usage
### Build
* `$ ./gradlew buildClientJar`: Creates a jar for client source set
* `$ ./gradlew buildNodeJar`: Creates a jar for node source set

### Daemons
**You must create your own topology.properties file**

* `./proxy-node.sh [start|stop|restart] [port] [path]`

### Run Client
* `java -jar build/libs/proxy-cache-client-<version>.jar [command [file]] [options]`

### Assumptions
* The port used to listen nodes will be the master consultations 8181 which can not be used for other purposes
* Is used configuration files used in each node to store the file list (if it is a proxy) and store ips of synchronized proxies (if master). These files are small
* It is not accepted that upload files with the same name.

### Vagrant
For testing purposes vagrant file is created for creating virtual machines:

#### Steps:
* `$ bundle install`
* `$ bundle exec librarian-chef install`
* `$ vagrant up --provision`: this could take a long time
* `$ vagrant ssh master`: to enter a master machine
* `$ vagrant ssh node1`: to enter a node machine