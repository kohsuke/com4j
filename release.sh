#!/bin/zsh -ex
ant clean dist deploy
dt=$(date +%Y%m%d)
jnupload com4j "/$(date +%Y-%m-%d) release" "$(date +%Y/%m/%d) release" stable build/com4j-$dt.zip
