#! /bin/bash
scriptdir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $scriptdir

/opt/activator-1.3.2-minimal/activator dist

unzip -d svc target/universal/*-1.0-SNAPSHOT.zip && mv svc/*/* svc/ && rm svc/bin/*.bat && mv svc/bin/* svc/bin/start

docker build -t skos-lookup .
docker run -it -p 9000:9000 --rm skos-lookup
