/*
 * Copyright (C) 2016 Jens Reimann <jreimann@redhat.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dentrassi.camel.milo.client;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class OpcUaClientComponent extends UriEndpointComponent {

	private static final Logger LOG = LoggerFactory.getLogger(OpcUaClientComponent.class);

	private final Map<String, OpcUaClientConnection> cache = new HashMap<>();
	private final Multimap<String, OpcUaClientEndpoint> connectionMap = HashMultimap.create();

	public OpcUaClientComponent() {
		super(OpcUaClientEndpoint.class);
	}

	@Override
	protected Endpoint createEndpoint(final String uri, final String remaining, final Map<String, Object> parameters)
			throws Exception {

		final URI itemUri = URI.create(remaining);

		final OpcUaClientEndpointConfiguration configuration = OpcUaClientEndpointConfiguration.fromUri(itemUri);

		if (configuration == null) {
			return null;
		}

		setProperties(configuration, parameters);

		return createEndpoint(uri, itemUri, configuration);
	}

	private synchronized OpcUaClientEndpoint createEndpoint(final String uri, final URI itemUri,
			final OpcUaClientEndpointConfiguration configuration) {

		OpcUaClientConnection connection = this.cache.get(configuration.toConnectionCacheId());

		if (connection == null) {
			LOG.debug("Cache miss - creating new connection instance: {}", configuration.toConnectionCacheId());

			connection = new OpcUaClientConnection(configuration);
			this.cache.put(configuration.toConnectionCacheId(), connection);
		}

		final OpcUaClientEndpoint endpoint = new OpcUaClientEndpoint(uri, this, connection, configuration);

		this.connectionMap.put(connection.getConnectionId(), endpoint);

		return endpoint;
	}

	public synchronized void disposed(final OpcUaClientEndpoint endpoint) {

		final OpcUaClientConnection connection = endpoint.getConnection();

		// unregister usage of connection

		this.connectionMap.remove(connection.getConnectionId(), endpoint);

		// test if this was the last endpoint using this connection

		if (!this.connectionMap.containsKey(connection.getConnectionId())) {

			// this was the last endpoint using the connection ...

			// ... remove from the cache

			this.cache.remove(connection.getConnectionId());

			// ... and close

			try {
				connection.close();
			} catch (final Exception e) {
				LOG.warn("Failed to close connection", e);
			}
		}
	}
}
