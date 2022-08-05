#!/usr/bin/env bash

#
# This script will extract and format the _default_ ldap schema for use in the unity ldap-endpoint
#

echo "Building default.zip"
mkdir -p default
(
  cd default

  #TODO: how to get the api-ldap-schema-data-x.y.z.jar before building the project
  tar -xf ../../../distribution/target/unity-server-distribution-3.10.0-1.0.0-SNAPSHOT-dist.tar.gz
  mkdir -p lib
  cd lib
  echo "Extracting api-ldap-schema-data-2.0.0.jar"
  unzip -q ../unity-server-distribution-3.10.0-1.0.0-SNAPSHOT/lib/api-ldap-schema-data-2.0.0.jar
  cd ..

  echo "Compressing default.zip"
  rm -rf partitions/schema
  mkdir -p partitions
  mv lib/schema partitions
  zip -q -r default.zip partitions
)

mv default/default.zip .
rm -rf default/lib
rm -rf default/unity-server-distribution-3.10.0-1.0.0-SNAPSHOT

if [ -d ext ]; then
  echo "Building ext.zip"
  (
    cd ext
    zip -q -r ext.zip partitions
  )
  mv ext/ext.zip .
fi
