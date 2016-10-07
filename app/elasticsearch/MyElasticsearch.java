/*Copyright (c) 2016 "hbz"

This file is part of agrovoc-lookup.

agrovoc-lookup is free software: you can redistribute it and/or modify
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.openrdf.rio.RDFFormat;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.common.base.Charsets;

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
	}

	/**
	 * @param dir indexes a directory with files in RDF-Ntriple format. Files are
	 *          converted to JSONLD.
	 */
	public void indexDirectory(final File dir) {
		try {
			BulkRequestBuilder indexBulk = es.getClient().prepareBulk();
			int i = 0;
			for (File file : dir.listFiles()) {
				prepareIndexing(indexBulk, "" + i++, file);
			}
			executeIndexing(indexBulk);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void prepareIndexing(BulkRequestBuilder internalIndexBulk, String id,
			File file) {
		try (InputStream filein = new FileInputStream(file);
				InputStream contextIn = new FileInputStream(es.getJsonldContext())) {
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
			internalIndexBulk.add(es.getClient()
					.prepareIndex(es.getIndex(), "concept", id).setSource(source));

		} catch (Exception e) {
			play.Logger.warn("", e);
		}
	}

	private void executeIndexing(BulkRequestBuilder indexBulk) {

		List<String> result = new ArrayList<>();
		try {
			play.Logger.debug("Start building internal Index " + es.getIndex());
			BulkResponse bulkResponse = indexBulk.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				result.add(bulkResponse.buildFailureMessage());
				play.Logger.debug("FAIL: " + bulkResponse.buildFailureMessage());
			}
		} catch (Exception e) {
			play.Logger.warn("", e);
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
	 */
	public void init() {
		try {
			es.getClient().admin().cluster().prepareHealth().setWaitForYellowStatus()
					.execute().actionGet();
			if (es.getClient().admin().indices().prepareExists(es.getIndex())
					.execute().actionGet().isExists()) {
				es.getClient().admin().indices()
						.delete(new DeleteIndexRequest(es.getIndex())).actionGet();
			}
			CreateIndexRequestBuilder cirb =
					es.getClient().admin().indices().prepareCreate(es.getIndex());
			if (es.getIndexSettings() != null) {
				String settingsMappings = Files.lines(Paths.get(es.getIndexSettings()))
						.collect(Collectors.joining());
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
	 * @param q an elasticsearch query string
	 * @param from show results from this index
	 * @param until show results until this index
	 * @return a SearchHits object containing all hits
	 */
	public SearchHits query(String q, int from, int until) {
		es.getClient().admin().indices().refresh(new RefreshRequest()).actionGet();
		play.Logger.debug("Search for " + q);
		QueryBuilder query = QueryBuilders.queryStringQuery(q);
		SearchHits hits = query(query, from, until);
		return hits;
	}

	private SearchHits query(QueryBuilder query, int from, int until) {
		es.getClient().admin().indices().refresh(new RefreshRequest()).actionGet();
		SearchResponse response =
				es.getClient().prepareSearch(es.getIndex()).setQuery(query)
						.setFrom(from).setSize(until - from).execute().actionGet();
		return response.getHits();
	}

	/**
	 * @param q a string to query the field prefLabel.{@parameter lang}
	 * @param lang the language to get autocompletion for
	 * @param from this index
	 * @param until this index
	 * @return a SearchHits object containing all suggestions
	 */
	public SearchHits autocompleteQuery(String q, String lang, int from,
			int until) {
		es.getClient().admin().indices().refresh(new RefreshRequest()).actionGet();
		play.Logger.debug("Search for " + q);
		QueryBuilder query = QueryBuilders.matchQuery("prefLabel." + lang, q);
		SearchHits hits = query(query, from, until);
		return hits;
	}
}
