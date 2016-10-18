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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import services.RdfUtils;

/**
 * @author Jan Schnasse
 *
 */
public class MyElasticsearch {

	EmbeddedElasticsearch es;

	/**
	 * Creates a new Elasticsearchinterface
	 */
	public MyElasticsearch() {
		es = new EmbeddedElasticsearch();
		try (InputStream settingsIn =
				play.Environment.simple().resourceAsStream(es.getIndexSettings())) {

			es.getClient().admin().cluster().prepareHealth().setWaitForYellowStatus()
					.execute().actionGet();

			es.getClient().admin().indices().refresh(new RefreshRequest())
					.actionGet();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param dir indexes a directory with files in RDF-Ntriple format. Files are
	 *          converted to JSONLD.
	 * @param index the index to put data into
	 */
	public void indexDirectory(final File dir, String index) {
		try (BulkProcessor bulkProcessor = getBulkProcessor()) {
			long i = 1;
			for (File file : dir.listFiles()) {
				String id = index + ":" + i;
				String source = getJsonLdString(file);
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
	 * @param gzipDataAsInputStream an gzip compressed inputstream with rdf data
	 *          in Format f
	 * @param index the name of the index to add data to
	 * @param f the RDFFormat of the inputstream
	 */
	public void indexZippedFile(final InputStream gzipDataAsInputStream,
			String index, RDFFormat f) {
		try {
			MyTripleStore ts = new MyTripleStore();
			ts.loadZippedFile(gzipDataAsInputStream, f);
			index(index, ts);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param in nputstream with rdf data in Format f
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
		try (BulkProcessor bulkProcessor = getBulkProcessor()) {
			long i = 1;
			for (String c : ts.getAllConcepts()) {
				Collection<Statement> statements = ts.getConcept(c);
				String id = index + ":" + i;
				String source = getJsonLdString(statements);
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

	private BulkProcessor getBulkProcessor() {
		return BulkProcessor.builder(es.getClient(), new BulkProcessor.Listener() {
			@Override
			public void beforeBulk(long executionId, BulkRequest request) {
				play.Logger.info("Going to execute new bulk composed of {} actions",
						request.numberOfActions());
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request,
					BulkResponse response) {
				for (BulkItemResponse bulkItemResponse : response.getItems()) {
					if (bulkItemResponse.isFailed()) {
						BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
						Throwable rootCause =
								ExceptionsHelper.unwrapCause(failure.getCause());
						play.Logger.error("", rootCause);
						play.Logger.error("", bulkItemResponse.getFailure());
					}
				}
				play.Logger.info("Executed bulk composed of {} actions",
						request.numberOfActions());
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request,
					Throwable failure) {
				play.Logger.warn("Error executing bulk", failure);
			}
		}).setBulkActions(1024)
				.setBackoffPolicy(
						BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(100), 3))
				.build();
	}

	private String getJsonLdString(Collection<Statement> statements) {
		try (InputStream contextIn =
				play.Environment.simple().resourceAsStream(es.getJsonldContext())) {
			String ld = RdfUtils.graphToString(statements, RDFFormat.JSONLD);
			ld = ld.substring(1, ld.length() - 1);
			InputStream inputStream =
					new ByteArrayInputStream(ld.getBytes(Charsets.UTF_8));
			Object jsonObject = JsonUtils.fromInputStream(inputStream);
			@SuppressWarnings({ "unchecked" })
			Map<String, Object> context =
					(Map<String, Object>) JsonUtils.fromInputStream(contextIn);
			JsonLdOptions options = new JsonLdOptions();
			Object compact = JsonLdProcessor.compact(jsonObject, context, options);
			String source = JsonUtils.toPrettyString(compact);
			return source;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String getJsonLdString(File file) {
		try (InputStream filein = new FileInputStream(file);
				InputStream contextIn =
						play.Environment.simple().resourceAsStream(es.getJsonldContext())) {
			String ld = RdfUtils.readRdfToString(filein, RDFFormat.NTRIPLES,
					RDFFormat.JSONLD, "");
			ld = ld.substring(1, ld.length() - 1);
			InputStream inputStream =
					new ByteArrayInputStream(ld.getBytes(Charsets.UTF_8));
			Object jsonObject = JsonUtils.fromInputStream(inputStream);
			@SuppressWarnings({ "unchecked" })
			Map<String, Object> context =
					(Map<String, Object>) JsonUtils.fromInputStream(contextIn);
			JsonLdOptions options = new JsonLdOptions();
			Object compact = JsonLdProcessor.compact(jsonObject, context, options);
			String source = JsonUtils.toPrettyString(compact);
			return source;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * You can stop the elasticsearch node as well as the client
	 */
	public void stopElasticSearch() {
		es.getNode().close();
		es.getClient().close();
	}

	/**
	 * Delete index and apply settings
	 * 
	 * @param index name of the index
	 */
	public void init(String index) {
		try (InputStream settingsIn =
				play.Environment.simple().resourceAsStream(es.getIndexSettings())) {
			es.getClient().admin().cluster().prepareHealth().setWaitForYellowStatus()
					.execute().actionGet();
			if (es.getClient().admin().indices().prepareExists(index).execute()
					.actionGet().isExists()) {
				es.getClient().admin().indices().delete(new DeleteIndexRequest(index))
						.actionGet();
			}
			CreateIndexRequestBuilder cirb =
					es.getClient().admin().indices().prepareCreate(index);

			if (es.getIndexSettings() != null) {
				String settingsMappings =
						CharStreams.toString(new InputStreamReader(settingsIn, "UTF-8"));
				cirb.setSource(settingsMappings);
			}
			cirb.execute().actionGet();
			es.getClient().admin().indices().refresh(new RefreshRequest())
					.actionGet();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param index name of the index
	 * @param q an elasticsearch query string
	 * @param from show results from this index
	 * @param until show results until this index
	 * @return a SearchHits object containing all hits
	 */
	public SearchHits query(String index, String q, int from, int until) {
		es.getClient().admin().indices().refresh(new RefreshRequest()).actionGet();
		QueryBuilder query = QueryBuilders.queryStringQuery(q);
		SearchHits hits = query(index, query, from, until);
		return hits;
	}

	private SearchHits query(String index, QueryBuilder query, int from,
			int until) {
		es.getClient().admin().indices().refresh(new RefreshRequest()).actionGet();
		SearchResponse response =
				es.getClient().prepareSearch(index).setQuery(query).setFrom(from)
						.setSize(until - from).execute().actionGet();
		return response.getHits();
	}

	/**
	 * @param index name of the index
	 * @param q a string to query the field prefLabel.{@parameter lang}
	 * @param lang the language to get autocompletion for
	 * @param from this index
	 * @param until this index
	 * @return a SearchHits object containing all suggestions
	 */
	public SearchHits autocompleteQuery(String index, String q, String lang,
			int from, int until) {
		es.getClient().admin().indices().refresh(new RefreshRequest()).actionGet();
		QueryBuilder query = QueryBuilders.matchQuery("prefLabel." + lang, q);
		SearchHits hits = query(index, query, from, until);
		return hits;
	}

	/**
	 * @return a list of available elasticsearch indices
	 */
	public List<String> getIndexList() {
		return Arrays.asList(es.getClient().admin().cluster().prepareState()
				.execute().actionGet().getState().getMetaData().concreteAllIndices());
	}

	/**
	 * @param index the name of the index or the alias
	 * @return number of documents in the index
	 */
	public long getSize(String index) {
		final CountResponse response =
				es.getClient().prepareCount(index).execute().actionGet();
		return response.getCount();
	}
}
