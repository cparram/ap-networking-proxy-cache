[![Build Status](https://travis-ci.org/cparram/ap-networking-proxy-cache.svg?branch=staging)](https://travis-ci.org/cparram/ap-networking-proxy-cache)

# ap-networking-proxy-cache
Proxy cache implementation

## Usage
### Build
* `$ ./gradlew buildClientJar`: Creates a jar for client source set
* `$ ./gradlew buildNodeJar`: Creates a jar for node source set
* `$ ./gradlew buildMasterJar`: Creates a jar for master source set

### Daemons
**You must create your own config.sh file**

* `./proxy-node.sh [start|stop|restart]`
* `./proxy-master.sh [start|stop|restart]`

### Run Client
* `java -jar build/libs/proxy-cache-client-<version>.jar [command [file]] [options]`
