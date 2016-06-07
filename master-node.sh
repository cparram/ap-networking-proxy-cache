#!/usr/bin/env bash
my_dir="$(dirname "$0")"
source "$my_dir/config.sh"
SERVICE_NAME="Master node"
PATH_TO_JAR="$my_dir/build/libs/proxy-cache-master-0.1.0.jar"
PID_PATH_NAME="$my_dir/build/tmp/master-pid"

case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            nohup java -jar $PATH_TO_JAR --port-proxy-master $PORT_PROXY_MASTER --port-master $PORT_MASTER  --path $PATH_MASTER > build/tmp/master.out 2>&1 & echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stoping ..."
            kill $PID;
            echo "$SERVICE_NAME stopped ..."
            rm $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
esac