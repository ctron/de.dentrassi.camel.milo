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

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.impl.DefaultMessage;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.component.milo.client.MiloClientConnection.MonitorHandle;

public class MiloClientConsumer extends DefaultConsumer {

	private static final Logger LOG = LoggerFactory.getLogger(MiloClientConsumer.class);

	private final MiloClientConnection connection;

	private final MiloClientEndpointConfiguration configuraton;

	private MonitorHandle handle;

	public MiloClientConsumer(final MiloClientEndpoint endpoint, final Processor processor,
							  final MiloClientConnection connection, final MiloClientEndpointConfiguration configuration) {
		super(endpoint, processor);

		Objects.requireNonNull(connection);
		Objects.requireNonNull(configuration);

		this.connection = connection;
		this.configuraton = configuration;
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();

		this.handle = this.connection.monitorValue(this.configuraton.getNamespaceUri(), this.configuraton.getItem(),
				this::handleValueUpdate);
	}

	@Override
	protected void doStop() throws Exception {
		if (this.handle != null) {
			this.handle.unregister();
			this.handle = null;
		}

		super.doStop();
	}

	private void handleValueUpdate(final DataValue value) {
		final Exchange exchange = getEndpoint().createExchange();
		exchange.setIn(mapMessage(value));
		try {
			getAsyncProcessor().process(exchange);
		} catch (final Exception e) {
			LOG.debug("Failed to process message", e);
		}
	}

	private Message mapMessage(final DataValue value) {
		if (value == null) {
			return null;
		}

		final DefaultMessage result = new DefaultMessage();

		result.setBody(value);

		result.setHeader("opcua.host", this.configuraton.getHost());
		result.setHeader("opcua.port", this.configuraton.getPort());
		result.setHeader("opcua.item.id", this.configuraton.getItem());
		result.setHeader("opcua.item.namespaceUri", this.configuraton.getNamespaceUri());

		return result;
	}

}
