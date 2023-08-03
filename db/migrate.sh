#!/bin/bash

DB_HOST=$1
DB_PORT=$2
DB_NAME=$3
DB_USER=$4
DB_PASSWORD=$4

if [ -z "$DB_HOST" ]; then
  echo "DB_HOST is empty."
  exit 1
fi
if [ -z "$DB_PORT" ]; then
  echo "DB_PORT is empty."
  exit 1
fi
if [ -z "$DB_NAME" ]; then
  echo "DB_NAME is empty."
  exit 1
fi
if [ -z "$DB_USER" ]; then
  echo "DB_USER is empty."
  exit 1
fi
if [ -z "$DB_PASSWORD" ]; then
  echo "DB_PASSWORD is empty."
  exit 1
fi

cd `dirname $0`

docker run --rm -it \
--add-host=host.docker.internal:host-gateway \
-v ./migration:/flyway/sql \
flyway/flyway \
-url="jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?characterEncoding=utf-8&characterSetResults=utf-8" \
-user="$DB_USER" \
-password="$DB_PASSWORD"
