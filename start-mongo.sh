#!/bin/bash

cd /root/cave
source setupLocalSocket.sh
export SKYCAVE_CAVESTORAGE_IMPLEMENTATION=cloud.cave.server.service.ServerCaveStorage
export SKYCAVE_DBSERVER=172.17.0.17:27017
ant ocean-daemon
