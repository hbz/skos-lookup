[![Travis Ci](https://travis-ci.org/hbz/skos-lookup.svg?branch=master)](https://travis-ci.org/hbz/skos-lookup)
#About

A webservice to lookup SKOS concepts from an elasticsearch index.
- Start service. 
- Upload a list of Ntriple files each representing one SKOS Concept. 
- Perform autocompletion lookup over the SKOS Vocabular
- get back an URI for a particular SKOS Concept.

Useful to support your users when filling forms.

#Usage

##Java 8

	echo $JAVA_HOME //check if java 8 is configured

##Download Activator

	wget http://downloads.typesafe.com/typesafe-activator/1.3.2/typesafe-activator-1.3.2-minimal.zip
	unzip typesafe-activator-1.3.2-minimal.zip
	sudo mv activator-1.3.2-minimal /opt

##Git clone

	cd /tmp
	git clone https://github.com/hbz/skos-lookup
	cd skos-lookup

##Run

	/opt/activator-1.3.2-minimal/activator run

##Add sample data to index

	curl -XPOST localhost:9000/tools/skos-lookup/init?dataDirectory=/tmp/skos-lookup/test/resources/testData&index=agrovoc_test
	
##Perform sample query

	curl -XGET 'localhost:9000/tools/skos-lookup/autocomplete?lang=de&q=Erdnus&callback=mycallback&index=agrovoc_test'
	
Response
	
	/**/mycallback([{"label":"Erdnuss","value":"c_11368"}])
	
	
##Example UI with jQuery autocomplete

	firefox http://localhost:9000/tools/skos-lookup/example

#Add skos data

**Attention** This will create >32000 files under /tmp/skos-split. **This can take hours!**

	cd test/resources
	editor split.sh #uncomment lines on bottom to create ddc or agrovoc data directory
	./split.sh
	
Indexing should not take longer than 2min.
	
	curl -XPOST localhost:9000/tools/skos-lookup/init?dataDirectory=/tmp/skos-split

or
	
	curl -XPOST localhost:9000/tools/skos-lookup/init?dataDirectory=/tmp/ddc-split

		
#Install on Ubuntu

	cd /tmp/skos-lookup
	/opt/activator-1.3.2-minimal/activator dist
	cp target/universal/skos-lookup-1.0-SNAPSHOT.zip  /tmp
	cd /tmp
	unzip skos-lookup-1.0-SNAPSHOT.zip
	mv skos-lookup-1.0-SNAPSHOT /opt/skos-lookup

edit startscript

	sudo cp /tmp/skos-lookup/install/skos-lookup.tmpl /etc/init.d/skos-lookup
	sudo chmod u+x /etc/init.d/skos-lookup
	sudo editor /etc/init.d/skos-lookup

set the following vars

	JAVA_HOME=/opt/java
	HOME="/opt/skos-lookup"
	USER="user to run skos-lookup"
	GROUP="user to run skos-lookup"
	SECRET=`uuidgen` # generate a secret e.g. using uuidgen
	PORT=9000

include into system start and shutdown

	sudo update-rc.d skos-lookup defaults 99 20
	
start

	sudo service skos-lookup start

#Update
	rm -rf /tmp/skos-lookup
	cd /tmp
	git clone https://github.com/hbz/skos-lookup
	cd /tmp/skos-lookup
	/opt/activator-1.3.2-minimal/activator dist
	cp target/universal/skos-lookup-1.0-SNAPSHOT.zip  /tmp
	cd /tmp
	unzip skos-lookup-1.0-SNAPSHOT.zip
	cp /opt/skos-lookup/conf/application.conf /tmp/skos-lookup-1.0-SNAPSHOT/conf
	cp -r /opt/skos-lookup/data /tmp/skos-lookup-1.0-SNAPSHOT
	sudo service skos-lookup stop
	rm -rf /opt/skos-lookup/*
	mv /tmp/skos-lookup-1.0-SNAPSHOT/* /opt/skos-lookup/
	sudo service skos-lookup start

#LICENSE

GNU AFFERO GENERAL PUBLIC LICENSE
Version 3, 19 November 2007
