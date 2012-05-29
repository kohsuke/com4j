#!/bin/bash -ex

# build the bootstrap jar
pushd runtime
  ant clean jar
popd
pushd tlbimp
  ant clean jar
popd
cp runtime/build/com4j.jar bootstrap/com4j.jar
cp tlbimp/build/tlbimp.jar  bootstrap/com4j.jar

# use that bootstrap jar to build the whole distribution
ant clean dist deploy -lib ./bootstrap

# done
#dt=$(date +%Y%m%d)
#jnupload com4j "/$(date +%Y-%m-%d) release" "$(date +%Y/%m/%d) release" stable build/com4j-$dt.zip
