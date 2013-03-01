#!/bin/bash

CUR_VERSION=`mvn help:evaluate -Dexpression=project.version | grep -v '\['`

while getopts "d:r:" opt
do
    case "$opt" in
        d) DEV_VERSION=$OPTARG;;
        r) REL_VERSION=$OPTARG;;
    esac
done

if [[ -n "$DEV_VERSION" && -n "$REL_VERSION" ]]
then
    echo "Development version: $DEV_VERSION"
    echo "Release version: $REL_VERSION"

    mvn release:clean &&                     \
    mvn release:prepare                      \
        --batch-mode                         \
        -Dtag=saga-root-$REL_VERSION         \
        -DreleaseVersion=$REL_VERSION        \
        -DdevelopmentVersion=$DEV_VERSION && \

    mvn release:perform
else
    echo "Usage: ./release.sh -d <development version> -r <release version>"
    exit 1
fi
 
sed -Ei -e "s/(def projectVersion = )('$CUR_VERSION')/\1'$REL_VERSION'/" ./gradle-saga-plugin/build.gradle

cd gradle-saga-plugin

git add build.gradle
git commit -m "releasing Gradle plugin version $REL_VERSION"
git push

gradle -Prelease uploadArchives

sed -Ei -e "s/(def projectVersion = )('$REL_VERSION')/\1'$DEV_VERSION'/" build.gradle
rm build.gradle-e

git add build.gradle
git commit -m "preparing for next development iterationn ($DEV_VERSION)"
git push

cd -
