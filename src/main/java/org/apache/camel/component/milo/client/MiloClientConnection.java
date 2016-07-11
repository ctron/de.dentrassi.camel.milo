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

import java.util.Objects;
import java.util.function.Consumer;

import org.apache.camel.component.milo.client.internal.SubscriptionManager;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.stack.core.Stack;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

public class MiloClientConnection implements AutoCloseable {

	private final MiloClientConfiguration configuration;

	private SubscriptionManager manager;

	private boolean initialized;

	private final OpcUaClientConfigBuilder clientConfiguration;

	public MiloClientConnection(final MiloClientConfiguration configuration,
			final OpcUaClientConfigBuilder clientConfiguration) {
		Objects.requireNonNull(configuration);

		// make a copy since the configuration is mutable
		this.configuration = configuration.clone();
		this.clientConfiguration = clientConfiguration;
	}

	protected void init() throws Exception {
		this.manager = new SubscriptionManager(this.configuration, this.clientConfiguration,
				Stack.sharedScheduledExecutor(), 10_000);
	}

	@Override
	public void close() throws Exception {
		if (this.manager != null) {
			this.manager.dispose();
			this.manager = null;
		}
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

	public interface MonitorHandle {
		public void unregister();
	}

	public MonitorHandle monitorValue(final MiloClientItemConfiguration configuration,
			final Consumer<DataValue> valueConsumer) {

		Objects.requireNonNull(configuration);
		Objects.requireNonNull(valueConsumer);

		checkInit();

		final UInteger handle = this.manager.registerItem(configuration.getNamespaceUri(),
				configuration.getNamespaceIndex(), configuration.getNodeId(), configuration.getSamplingInterval(),
				valueConsumer);

		return new MonitorHandle() {

			@Override
			public void unregister() {
				MiloClientConnection.this.manager.unregisterItem(handle);
			}
		};
	}

	public String getConnectionId() {
		return this.configuration.toCacheId();
	}

	public void writeValue(final String namespaceUri, final Integer namespaceIndex, final String item,
			final Object value) {
		checkInit();

		this.manager.write(namespaceUri, namespaceIndex, item, mapValue(value));
	}

	/**
	 * Map the incoming value to some value writable to the milo client
	 *
	 * @param value
	 *            the incoming value
	 * @return the outgoing value
	 */
	private DataValue mapValue(final Object value) {
		if (value instanceof DataValue) {
			return (DataValue) value;
		}
		if (value instanceof Variant) {
			return new DataValue((Variant) value);
		}
		return new DataValue(new Variant(value));
	}

}
