# ap-networking-proxy-cache
Proxy cache implementation

# ap-networking-proxy-cache
Proxy cache implementation

## Usage
### Build
* `$ ./gradlew buildClientJar`: Creates a jar for client source set
* `$ ./gradlew buildNodeJar`: Creates a jar for node source set

### Run proxy server daemon
You must export a system variable called `$PATH_TO_PROXY` where are the server files cache
* `./proxy-node.sh [start|stop|restart]`

### Run Client
* `java -jar build/libs/proxy-cache-client-<version>.jar [command [file]] [options]`
