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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.SearchHits;
import org.xbib.elasticsearch.plugin.bundle.BundlePlugin;

import com.google.common.io.CharStreams;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * @author Jan Schnasse
 *
 */
public class EmbeddedElasticsearch {

	private static class ConfigurableNode extends Node {
		public ConfigurableNode(Settings settings,
				Collection<Class<? extends Plugin>> list) {
			super(InternalSettingsPreparer.prepareEnvironment(settings, null),
					Version.CURRENT, list);
		}
	}

	private String indexSettings;
	private Node node;
	private Client client;

	/**
	 * Creates a client to an internal elasticsearch node in order to provide
	 * further actions.
	 */
	@SuppressWarnings("resource")
	protected EmbeddedElasticsearch() {
		indexSettings = "skos-settings.json";
		Config conf = ConfigFactory.load();
		Settings mySettings = Settings.settingsBuilder()
				.put("network.host", conf.getString("index.host"))
				.put("path.home", conf.getString("index.location"))
				.put("http.port", conf.getString("index.http_port"))
				.put("transport.tcp.port", conf.getString("index.tcp_port")).build();
		node = new ConfigurableNode(NodeBuilder.nodeBuilder().settings(mySettings)
				.local(true).getSettings().build(), Arrays.asList(BundlePlugin.class))
						.start();
		client = node.client();
		try (InputStream settingsIn =
				play.Environment.simple().resourceAsStream(indexSettings)) {
			client.admin().cluster().prepareHealth().setWaitForYellowStatus()
					.execute().actionGet();
			client.admin().indices().refresh(new RefreshRequest()).actionGet();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Client getClient() {
		return client;
	}

	/**
	 * Delete index and apply settings
	 * 
	 * @param index name of the index
	 */
	protected void init(String index) {
		try (InputStream settingsIn =
				play.Environment.simple().resourceAsStream(indexSettings)) {
			getClient().admin().cluster().prepareHealth().setWaitForYellowStatus()
					.execute().actionGet();
			if (getClient().admin().indices().prepareExists(index).execute()
					.actionGet().isExists()) {
				getClient().admin().indices().delete(new DeleteIndexRequest(index))
						.actionGet();
			}
			CreateIndexRequestBuilder cirb =
					getClient().admin().indices().prepareCreate(index);
			if (indexSettings != null) {
				String settingsMappings =
						CharStreams.toString(new InputStreamReader(settingsIn, "UTF-8"));
				cirb.setSource(settingsMappings);
			}
			cirb.execute().actionGet();
			getClient().admin().indices().refresh(new RefreshRequest()).actionGet();

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
		getClient().admin().indices().refresh(new RefreshRequest()).actionGet();
		QueryBuilder query = QueryBuilders.queryStringQuery(q);
		SearchHits hits = query(index, query, from, until);
		return hits;
	}

	private SearchHits query(String index, QueryBuilder query, int from,
			int until) {
		getClient().admin().indices().refresh(new RefreshRequest()).actionGet();
		SearchResponse response = getClient().prepareSearch(index).setQuery(query)
				.setFrom(from).setSize(until - from).execute().actionGet();
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
		getClient().admin().indices().refresh(new RefreshRequest()).actionGet();
		QueryBuilder query = QueryBuilders.matchQuery("prefLabel." + lang, q);
		SearchHits hits = query(index, query, from, until);
		return hits;
	}

	/**
	 * @return a list of available elasticsearch indices
	 */
	public List<String> getIndexList() {
		return Arrays.asList(getClient().admin().cluster().prepareState().execute()
				.actionGet().getState().getMetaData().concreteAllIndices());
	}

	/**
	 * @param index the name of the index or the alias
	 * @return number of documents in the index
	 */
	public long getSize(String index) {
		final org.elasticsearch.action.count.CountResponse response =
				getClient().prepareCount(index).execute().actionGet();
		return response.getCount();
	}

	BulkProcessor getBulkProcessor() {
		return BulkProcessor.builder(getClient(), new BulkProcessor.Listener() {
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

	/**
	 * You can stop the elasticsearch node as well as the client
	 */
	public void stopElasticSearch() {
		node.close();
		getClient().close();
	}
}