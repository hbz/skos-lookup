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

import java.util.Arrays;
import java.util.Collection;

import org.elasticsearch.Version;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.Plugin;
import org.xbib.elasticsearch.plugin.bundle.BundlePlugin;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

class EmbeddedElasticsearch {

	private static class ConfigurableNode extends Node {
		public ConfigurableNode(Settings settings,
				Collection<Class<? extends Plugin>> list) {
			super(InternalSettingsPreparer.prepareEnvironment(settings, null),
					Version.CURRENT, list);
		}
	}

	private String indexSettings;
	private String jsonldContext;
	private String index;
	private Node node;
	private Client client;

	@SuppressWarnings("resource")
	protected EmbeddedElasticsearch() {
		indexSettings = "conf/skos-settings.json";
		jsonldContext = "conf/skos-context.json";
		Config conf = ConfigFactory.load();
		Settings mySettings = Settings.settingsBuilder()
				.put("network.host", conf.getString("index.host"))
				.put("path.home", conf.getString("index.location"))
				.put("http.port", conf.getString("index.http_port"))
				.put("transport.tcp.port", conf.getString("index.tcp_port")).build();
		index = conf.getString("index.name");
		node = new ConfigurableNode(NodeBuilder.nodeBuilder().settings(mySettings)
				.local(true).getSettings().build(), Arrays.asList(BundlePlugin.class))
						.start();
		client = node.client();
	}

	public String getIndex() {
		return index;
	}

	public Node getNode() {
		return node;
	}

	public Client getClient() {
		return client;
	}

	public String getJsonldContext() {
		return jsonldContext;
	}

	public String getIndexSettings() {
		return indexSettings;
	}
}