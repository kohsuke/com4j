#!/bin/bash -ex
# generate javadoc and deploy it to site
ant javadoc
git checkout gh-pages
git pull origin gh-pages
cp -R build/javadoc/* apidocs/
git add apidocs
git commit -m "javadoc updates" apidocs
git push origin gh-pages
git checkout master


