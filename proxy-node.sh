#!/usr/bin/env bash

SERVICE_NAME="Proxy cache"
PATH_TO_JAR=build/libs/proxy-cache-node-0.1.0.jar
PID_PATH_NAME=build/tmp/proxy-cache-pid

my_dir="$(dirname "$0")"
source "$my_dir/config.sh"

case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            nohup java -jar $PATH_TO_JAR --port-proxy-client $PORT_PROXY_CLIENT --port-proxy-master $PORT_PROXY_MASTER --port-master $PORT_MASTER --ip-master $IP_MASTER --path $PATH_PROXY > build/tmp/proxy-cache.out 2>&1 & echo $! > $PID_PATH_NAME
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
    restart)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stopping ...";
            kill $PID;
            echo "$SERVICE_NAME stopped ...";
            rm $PID_PATH_NAME
            echo "$SERVICE_NAME starting ..."
            nohup java -jar $PATH_TO_JAR --port $PORT --path $PATH_TO_PROXY > build/tmp/proxy-cache.out 2>&1 & echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
esac