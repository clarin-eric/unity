#!/bin/bash

function bell() {
  while true; do
    echo -e "Building..."
    sleep 60
  done
}
bell &

mvn install --show-version --log-file build.log --batch-mode
tail -n 1000 build.log

exit $?