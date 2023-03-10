#!/usr/bin/env bash
set -euo pipefail

CURR_DIR=$PWD;

if [[ -d synthesizer ]]; then
    cd synthesizer;
fi;

if [[ ! -f pom.xml ]]; then
    echo "Please navigate to home (~) or the synthesizer directory (~/synthesizer) before running this script.";
    exit 1;
fi;

echo -n "Compiling... ";
if BUILD_RESULTS=$(mvn clean compile -DskipTests); then
    echo "OK";
    MAVEN_OPTS="-Xmx8G" mvn exec:java -Dexec.mainClass="edu.ucsd.snippy.NoCFBenchmarks";
else
    echo "Compiling the synthesizer failed:";
    echo "$BUILD_RESULTS";
fi;

cd $CURR_DIR;
