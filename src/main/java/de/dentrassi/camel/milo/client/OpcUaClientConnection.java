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

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcUaClientConnection implements AutoCloseable {

	private static final Logger LOG = LoggerFactory.getLogger(OpcUaClientConnection.class);

	private final OpcUaClientEndpointConfiguration configuration;

	private OpcUaClient client;

	private UaSubscription subscription;

	private boolean initialized;

	private long currentClientId = 0;

	public OpcUaClientConnection(final OpcUaClientEndpointConfiguration configuration) {
		Objects.requireNonNull(configuration);

		// make a copy since the configuration is mutable
		this.configuration = configuration.clone();
	}

	protected void init() throws Exception {
		final OpcUaClientConfigBuilder cfg = new OpcUaClientConfigBuilder();
		cfg.setIdentityProvider(new AnonymousProvider());

		final URI uri = new URI("opc.tcp", null, this.configuration.getHost(), this.configuration.getPort(), null, null,
				null);

		final EndpointDescription[] endpoint = UaTcpStackClient.getEndpoints(uri.toString()).get();

		cfg.setEndpoint(endpoint[0]);

		this.client = new OpcUaClient(cfg.build());
		this.client.connect().get();

		this.subscription = this.client.getSubscriptionManager().createSubscription(1000.0).get();
	}

	protected synchronized void checkInit() {
		if (this.initialized) {
			return;
		}

		try {
			init();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		this.initialized = true;
	}

	private interface MonitorHandle {
		public void unregister();
	}

	public MonitorHandle monitorValue(final String namespaceUri, final String itemId,
			final Consumer<DataValue> valueConsumer) {

		Objects.requireNonNull(itemId);
		Objects.requireNonNull(valueConsumer);

		checkInit();

		LOG.debug("Request to add item - ns: {}, id: {}", namespaceUri, itemId);

		try {

			final UShort index = lookupNamespaceIndex(namespaceUri);

			LOG.debug("Namespace resolved as: {}", index);

			// FIXME: check for null

			final UInteger clientId = nextClientId();

			final NodeId nodeIdItem = new NodeId(index, itemId);
			final ReadValueId readItemId = new ReadValueId(nodeIdItem, AttributeId.Value.uid(), null,
					QualifiedName.NULL_VALUE);

			final MonitoringParameters parameters = new MonitoringParameters(clientId,
					1000.0 /* FIXME: second */, null, uint(10), Boolean.TRUE);

			final MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(readItemId,
					MonitoringMode.Reporting, parameters);

			final List<UaMonitoredItem> result = this.subscription
					.createMonitoredItems(TimestampsToReturn.Both, Collections.singletonList(request)).get();

			if (result.size() != 1) {
				throw new IllegalStateException(
						String.format("Subscribe returned %s results, 1 expected!", result.size()));
			}

			final UaMonitoredItem itemResult = result.get(0);

			LOG.debug("Item subscription -> {}", itemResult.getStatusCode());

			if (itemResult.getStatusCode().isBad()) {
				// item not found
				// FIXME: retry later

				throw new IllegalArgumentException(String.format("Item not found: %s", nodeIdItem));
			} else {
				itemResult.setValueConsumer(valueConsumer);
			}

			// return result

			return new MonitorHandle() {

				@Override
				public void unregister() {
					OpcUaClientConnection.this.subscription.deleteMonitoredItems(Collections.singletonList(itemResult));
				}
			};

		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private UInteger nextClientId() {
		// FIXME: range check
		return UInteger.valueOf(this.currentClientId++);
	}

	private UShort lookupNamespaceIndex(final String namespaceUri) throws InterruptedException, ExecutionException {

		final DataValue value = this.client.readValue(0, TimestampsToReturn.Neither, Identifiers.Server_NamespaceArray)
				.get();

		final Object rawValue = value.getValue().getValue();
		if (rawValue instanceof String[]) {
			final String[] namespaces = (String[]) rawValue;
			for (int i = 0; i < namespaces.length; i++) {
				if (namespaces[i].equals(namespaceUri)) {
					return Unsigned.ushort(i);
				}
			}
		}

		return null;
	}

	public void removeItem(final String itemId) {
	}

	@Override
	public void close() throws Exception {
		if (this.client != null) {
			this.client.disconnect().get();
			this.client = null;
		}
	}

	public String getConnectionId() {
		return this.configuration.toConnectionCacheId();
	}
}
