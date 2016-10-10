[![Travis Ci](https://travis-ci.org/hbz/skos-lookup.svg?branch=master)](https://travis-ci.org/hbz/skos-lookup)
#About

A webservice to lookup SKOS concepts from an elasticsearch index.
- Start service. 
- Upload a list of Ntriple files each representing one SKOS Concept. 
- Perform autocompletion lookup over the SKOS Vocabular
- get back an URI for a particular SKOS Concept.

Useful to support your users when filling forms.

#Usage

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

	curl -XPOST localhost:9000/init?dataDirectory=/tmp/skos-lookup/test/resources/testData
	
##Perform sample query

	curl -XGET 'localhost:9000/autocomplete?lang=de&q=Erdnus&callback=mycallback'
	
Response
	
	/**/mycallback([{"label":"Erdnuss","value":"c_11368"}])
	
	
##Example UI with jQuery autocomplete

	firefox http://localhost:9000/example

#Add Agrovoc data

**Attention** This will create >32000 files under /tmp/skos-split. **This can take hours!**

	cd test/resources
	./split.sh
	
Indexing should not take longer than 2min.
	
	curl -XPOST localhost:9000/init?dataDirectory=/tmp/skos-split
	
		
#LICENSE

GNU AFFERO GENERAL PUBLIC LICENSE
Version 3, 19 November 2007