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

package org.apache.camel.component.milo.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class MiloClientComponent extends UriEndpointComponent {

	private static final Logger LOG = LoggerFactory.getLogger(MiloClientComponent.class);

	private final Map<String, MiloClientConnection> cache = new HashMap<>();
	private final Multimap<String, MiloClientEndpoint> connectionMap = HashMultimap.create();

	public MiloClientComponent() {
		super(MiloClientEndpoint.class);
	}

	@Override
	protected Endpoint createEndpoint(final String uri, final String remaining, final Map<String, Object> parameters)
			throws Exception {

		final MiloClientConfiguration configuration = new MiloClientConfiguration();
		configuration.setEndpointUri(remaining);
		setProperties(configuration, parameters);

		return createEndpoint(uri, configuration, parameters);
	}

	private synchronized MiloClientEndpoint createEndpoint(final String uri,
			final MiloClientConfiguration configuration, final Map<String, Object> parameters) throws Exception {

		MiloClientConnection connection = this.cache.get(configuration.toCacheId());

		if (connection == null) {
			LOG.debug("Cache miss - creating new connection instance: {}", configuration.toCacheId());

			connection = new MiloClientConnection(configuration, mapToClientConfiguration(configuration));
			this.cache.put(configuration.toCacheId(), connection);
		}

		final MiloClientEndpoint endpoint = new MiloClientEndpoint(uri, this, connection,
				configuration.getEndpointUri());

		setProperties(endpoint, parameters);

		// register connection with endpoint

		this.connectionMap.put(configuration.toCacheId(), endpoint);

		return endpoint;
	}

	private OpcUaClientConfigBuilder mapToClientConfiguration(final MiloClientConfiguration configuration) {
		final OpcUaClientConfigBuilder builder = new OpcUaClientConfigBuilder();

		whenHasText(configuration::getApplicationName,
				value -> builder.setApplicationName(LocalizedText.english(value)));
		whenHasText(configuration::getApplicationUri, builder::setApplicationUri);
		whenHasText(configuration::getProductUri, builder::setProductUri);

		return builder;
	}

	private void whenHasText(final Supplier<String> valueSupplier, final Consumer<String> valueConsumer) {
		final String value = valueSupplier.get();
		if (value != null && !value.isEmpty()) {
			valueConsumer.accept(value);
		}
	}

	public synchronized void disposed(final MiloClientEndpoint endpoint) {

		final MiloClientConnection connection = endpoint.getConnection();

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
