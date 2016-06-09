#!/usr/bin/env bash

SERVICE_NAME="Node server $2"
my_dir="$(dirname "$0")"

PATH_TO_JAR="$my_dir/build/libs/proxy-cache-node-0.1.0.jar"
PID_PATH_NAME="$my_dir/build/tmp/node$2-pid"

case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            nohup java -jar $PATH_TO_JAR  --path $3 --port $2 > build/tmp/node$2.out 2>&1 & echo $! > $PID_PATH_NAME
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