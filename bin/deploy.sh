#!/bin/bash

if (( $EUID == 0 )); then
    echo "Don't run as root!"
    exit
fi

export TERM=xterm-color
deployingApp="skos-lookup"
branch=$(git status | grep branch | cut -d ' ' -f3)
echo "git currently on branch: "$branch
if [ ! -z "$1" ]; then
    branch="$1"
fi

cd /opt/toscience/git/$deployingApp
git pull origin $branch
/opt/toscience/activator/activator -java-home /opt/jdk clean
/opt/toscience/activator/activator -java-home /opt/jdk clean-files
/opt/toscience/activator/activator -java-home /opt/jdk dist
