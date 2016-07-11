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

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcUaClientProducer extends DefaultProducer {

	private static final Logger LOG = LoggerFactory.getLogger(OpcUaClientConsumer.class);

	private final OpcUaClientConnection connection;
	private final OpcUaClientEndpointConfiguration configuration;

	public OpcUaClientProducer(final Endpoint endpoint, final OpcUaClientConnection connection,
			final OpcUaClientEndpointConfiguration configuration) {
		super(endpoint);

		this.connection = connection;
		this.configuration = configuration;
	}

	@Override
	public void process(final Exchange exchange) throws Exception {
		final Message msg = exchange.getIn();
		final Object value = msg.getBody();

		LOG.debug("Processing message: {}", value);

		this.connection.writeValue(this.configuration.getNamespaceUri(), this.configuration.getItem(), value);
	}

}