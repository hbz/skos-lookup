#! /bin/bash
scriptdir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $scriptdir
cd ..
/opt/activator-1.3.2-minimal/activator dist

if [ -d docker/svc ]
then
	rm -rf docker/svc
fi 
unzip -d docker/svc target/universal/*-1.0-SNAPSHOT.zip && mv docker/svc/*/* docker/svc/ && rm docker/svc/bin/*.bat && mv docker/svc/bin/* docker/svc/bin/start

cd $scriptdir
docker volume create skos-lookup-data
docker build -t skos-lookup .
docker run -d --mount source=skos-lookup-data,target=/svc/data -p 9000:9000 --rm  skos-lookup
