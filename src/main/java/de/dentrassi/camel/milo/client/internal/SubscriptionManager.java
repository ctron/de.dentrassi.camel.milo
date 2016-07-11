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

package de.dentrassi.camel.milo.client.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
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

import de.dentrassi.camel.milo.client.OpcUaClientEndpointConfiguration;

public class SubscriptionManager {

	private final static Logger LOG = LoggerFactory.getLogger(SubscriptionManager.class);

	private final AtomicLong clientHandleCounter = new AtomicLong(0);

	public interface Worker<T> {
		public void work(T on) throws Exception;
	}

	private static class Subscription {
		private final String namespaceUri;
		private final String itemId;
		private final Consumer<DataValue> valueConsumer;

		public Subscription(final String namespaceUri, final String itemId, final Consumer<DataValue> valueConsumer) {
			this.namespaceUri = namespaceUri;
			this.itemId = itemId;
			this.valueConsumer = valueConsumer;
		}

		public String getNamespaceUri() {
			return this.namespaceUri;
		}

		public String getItemId() {
			return this.itemId;
		}

		public Consumer<DataValue> getValueConsumer() {
			return this.valueConsumer;
		}
	}

	private class Connected {
		private OpcUaClient client;
		private final UaSubscription manager;

		private final Map<UInteger, Subscription> badSubscriptions = new HashMap<>();

		private final Map<UInteger, UaMonitoredItem> goodSubscriptions = new HashMap<>();

		public Connected(final OpcUaClient client, final UaSubscription manager) {
			this.client = client;
			this.manager = manager;
		}

		public void putSubscriptions(final Map<UInteger, Subscription> subscriptions) throws Exception {

			if (subscriptions.isEmpty()) {
				return;
			}

			// convert to requests

			final List<MonitoredItemCreateRequest> items = new ArrayList<>(subscriptions.size());

			for (final Map.Entry<UInteger, Subscription> entry : subscriptions.entrySet()) {
				final Subscription s = entry.getValue();

				final UShort namespaceIndex = lookupNamespace(s.getNamespaceUri());

				final NodeId nodeId = new NodeId(namespaceIndex, s.getItemId());
				final ReadValueId itemId = new ReadValueId(nodeId, AttributeId.Value.uid(), null,
						QualifiedName.NULL_VALUE);
				final MonitoringParameters parameters = new MonitoringParameters(entry.getKey(), null, null, null,
						null);
				items.add(new MonitoredItemCreateRequest(itemId, MonitoringMode.Reporting, parameters));
			}

			// create monitors

			final List<UaMonitoredItem> result = this.manager.createMonitoredItems(TimestampsToReturn.Both, items)
					.get();

			// set value listeners

			// FIXME: use atomic API when available

			for (final UaMonitoredItem item : result) {
				final Subscription s = subscriptions.get(item.getClientHandle());

				if (item.getStatusCode().isBad()) {
					this.badSubscriptions.put(item.getClientHandle(), s);
					s.getValueConsumer().accept(new DataValue(item.getStatusCode()));
				} else {
					this.goodSubscriptions.put(item.getClientHandle(), item);
					item.setValueConsumer(s.getValueConsumer());
				}
			}

			if (!this.badSubscriptions.isEmpty()) {
				SubscriptionManager.this.executor.schedule(this::resubscribe, SubscriptionManager.this.reconnectTimeout,
						TimeUnit.MILLISECONDS);
			}
		}

		private void resubscribe() {
			final Map<UInteger, Subscription> subscriptions = new HashMap<>(this.badSubscriptions);
			this.badSubscriptions.clear();
			try {
				putSubscriptions(subscriptions);
			} catch (final Exception e) {
				handleConnectionFailue(e);
			}
		}

		public void activate(final UInteger clientHandle, final Subscription subscription) throws Exception {
			putSubscriptions(Collections.singletonMap(clientHandle, subscription));
		}

		public void deactivate(final UInteger clientHandle) throws Exception {
			final UaMonitoredItem item = this.goodSubscriptions.remove(clientHandle);
			if (item != null) {
				this.manager.deleteMonitoredItems(Collections.singletonList(item)).get();
			} else {
				this.badSubscriptions.remove(clientHandle);
			}
		}

		private UShort lookupNamespace(final String namespaceUri) throws Exception {
			return lookupNamespaceIndex(namespaceUri).get();
		}

		private CompletableFuture<UShort> lookupNamespaceIndex(final String namespaceUri) {

			// FIXME: implement cache

			final CompletableFuture<DataValue> future = this.client.readValue(0, TimestampsToReturn.Neither,
					Identifiers.Server_NamespaceArray);

			return future.thenApply(value -> {
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
			});
		}

		public void dispose() {
			if (this.client != null) {
				this.client.disconnect();
				this.client = null;
			}
		}

		public CompletableFuture<StatusCode> write(final String namespaceUri, final String item,
				final DataValue value) {

			final CompletableFuture<UShort> future = lookupNamespaceIndex(namespaceUri);
			return future.thenCompose(namespaceIndex -> {

				return this.client.writeValue(new NodeId(namespaceIndex, item), value).whenComplete((status, error) -> {
					if (status != null) {
						LOG.debug("Write to ns={}, id={} = {} -> {}", namespaceUri, item, value, status);
					} else {
						LOG.debug("Failed to write", error);
					}
				});

			});
		}
	}

	private final OpcUaClientEndpointConfiguration configuration;
	private final Map<UInteger, Subscription> subscriptions = new HashMap<>();
	private final ScheduledExecutorService executor;
	private final long reconnectTimeout;

	private Connected connected;

	private boolean disposed;

	private ScheduledFuture<?> reconnectJob;

	public SubscriptionManager(final OpcUaClientEndpointConfiguration configuration,
			final ScheduledExecutorService executor, final long reconnectTimeout) {
		this.configuration = configuration;
		this.executor = executor;
		this.reconnectTimeout = reconnectTimeout;

		connect();
	}

	private synchronized void handleConnectionFailue(final Throwable e) {
		if (this.connected != null) {
			this.connected.dispose();
			this.connected = null;
		}

		// always trigger re-connect

		triggerReconnect();
	}

	private void connect() {
		LOG.info("Starting connect");

		synchronized (this) {
			this.reconnectJob = null;

			if (this.disposed) {
				// we woke up disposed
				return;
			}
		}

		try {
			final Connected connected = performConnect();
			LOG.debug("Connect call done");
			synchronized (this) {
				if (this.disposed) {
					// we got disposed during connect
					return;
				}

				try {
					LOG.debug("Setting subscriptions");
					connected.putSubscriptions(this.subscriptions);

					LOG.debug("Update state : {} -> {}", this.connected, connected);
					final Connected oldConnected = this.connected;
					this.connected = connected;

					if (oldConnected != null) {
						LOG.debug("Dispose old state");
						oldConnected.dispose();
					}

				} catch (final Exception e) {
					LOG.info("Failed to set subscriptions", e);
					connected.dispose();
					throw e;
				}
			}
		} catch (final Exception e) {
			LOG.info("Failed to connect", e);
			triggerReconnect();
		}
	}

	public void dispose() {
		Connected connected;

		synchronized (this) {
			if (this.disposed) {
				return;
			}
			this.disposed = true;
			connected = this.connected;
		}

		if (connected != null) {
			// dispose outside of lock
			connected.dispose();
		}
	}

	private synchronized void triggerReconnect() {
		LOG.info("Trigger re-connect");

		if (this.reconnectJob != null) {
			return;
		}

		this.reconnectJob = this.executor.schedule(this::connect, this.reconnectTimeout, TimeUnit.MILLISECONDS);
	}

	private Connected performConnect() throws Exception {
		final URI uri = new URI("opc.tcp", null, this.configuration.getHost(), this.configuration.getPort(), null, null,
				null);

		final EndpointDescription endpoint = UaTcpStackClient.getEndpoints(uri.toString())
				.thenApply(endpoints -> endpoints[0]).get();

		final OpcUaClientConfigBuilder cfg = new OpcUaClientConfigBuilder();

		cfg.setIdentityProvider(new AnonymousProvider());
		cfg.setEndpoint(endpoint);

		final OpcUaClient client = new OpcUaClient(cfg.build());

		try {

			final UaSubscription manager = client.getSubscriptionManager().createSubscription(1_000.0).get();

			return new Connected(client, manager);
		} catch (final Throwable e) {
			if (client != null) {
				// clean up
				client.disconnect();
			}
			throw e;
		}
	}

	protected synchronized void whenConnected(final Worker<Connected> worker) {
		if (this.connected != null) {
			try {
				worker.work(this.connected);
			} catch (final Exception e) {
				handleConnectionFailue(e);
			}
		}
	}

	public UInteger registerItem(final String namespaceUri, final String itemId,
			final Consumer<DataValue> valueConsumer) {

		final UInteger clientHandle = Unsigned.uint(this.clientHandleCounter.incrementAndGet());
		final Subscription subscription = new Subscription(namespaceUri, itemId, valueConsumer);

		synchronized (this) {
			this.subscriptions.put(clientHandle, subscription);

			whenConnected(connected -> {
				connected.activate(clientHandle, subscription);
			});
		}

		return clientHandle;
	}

	public synchronized void unregisterItem(final UInteger clientHandle) {
		if (this.subscriptions.remove(clientHandle) != null) {
			whenConnected(connected -> {
				connected.deactivate(clientHandle);
			});
		}
	}

	public synchronized void write(final String namespaceUri, final String item, final DataValue value) {
		// schedule operation

		if (this.connected != null) {
			this.connected.write(namespaceUri, item, value).handleAsync((status, e) -> {
				// handle outside the lock, running using handleAsync
				if (e != null) {
					handleConnectionFailue(e);
				}
				return null;
			}, this.executor);
		}
	}

}
