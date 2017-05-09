/*Copyright (c) 2016 "hbz"

This file is part of skos-lookup.

skos-lookup is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elasticsearch;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;

import services.MyTripleStore;
import services.RdfGraphToJson;

/**
 * @author Jan Schnasse
 *
 */
public class ElasticsearchBuilder {

	EmbeddedElasticsearch es;

	/**
	 * Creates a new Elasticsearchinterface
	 */
	public ElasticsearchBuilder() {
		es = new EmbeddedElasticsearch();
	}

	/**
	 * @param index reinitialize index
	 */
	public void init(String index) {
		es.init(index);
	}

	/**
	 * @param dir indexes a directory with files in RDF-Ntriple format. Files are
	 *          converted to JSONLD.
	 * @param index the index to put data into
	 */
	public void indexDirectory(final File dir, String index) {
		try (BulkProcessor bulkProcessor = es.getBulkProcessor()) {
			long i = 1;
			for (File file : dir.listFiles()) {
				String id = index + ":" + i;
				String source = generateSourceFromFile(file);
				bulkProcessor
						.add(new IndexRequest(index, "concept", id).source(source));
				i++;
			}
			bulkProcessor.awaitClose(30L, TimeUnit.SECONDS);
			bulkProcessor.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String generateSourceFromFile(File file) {
		try (InputStream in = new FileInputStream(file)) {
			return RdfGraphToJson.getPrettyJsonLdString(in, RDFFormat.NTRIPLES);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param in inputstream with rdf data in Format f. gzip is also supported
	 * @param index the name of the index to add data to
	 * @param f the RDFFormat of the inputstream
	 */
	public void indexFile(final InputStream in, String index, RDFFormat f) {
		try {
			MyTripleStore ts = new MyTripleStore();
			ts.loadFile(in, f);
			index(index, ts);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void index(String index, MyTripleStore ts) {
		try (BulkProcessor bulkProcessor = es.getBulkProcessor()) {
			long i = 1;
			for (String c : ts.getAllConcepts()) {
				String id = index + ":" + i;
				String source = RdfGraphToJson.getPrettyJsonLdString(ts.getDocument(c));
				bulkProcessor
						.add(new IndexRequest(index, "concept", id).source(source));
				i++;
			}
			bulkProcessor.awaitClose(30L, TimeUnit.SECONDS);
			bulkProcessor.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return an elasticsearch wrapper
	 */
	public EmbeddedElasticsearch getInstance() {
		return es;
	}
}
