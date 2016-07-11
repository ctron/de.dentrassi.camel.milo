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

import java.net.URI;
import java.util.Objects;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;

@UriEndpoint(scheme = "milo-client", syntax = "milo-client:tcp://host:port/ItemId?namespaceUri=urn:foo:bar", title = "OPC UA Client", consumerClass = MiloClientConsumer.class, label = "iot")
public class MiloClientEndpoint extends DefaultEndpoint {

	/**
	 * The main path
	 */
	@UriPath
	@Metadata(required = "true")
	private final String path;

	private final MiloClientConnection connection;
	private final MiloClientComponent component;
	private final MiloClientEndpointConfiguration configuration;

	public MiloClientEndpoint(final String uri, final URI itemUri, final MiloClientComponent component,
							  final MiloClientConnection connection, final MiloClientEndpointConfiguration configuration) {
		super(uri, component);

		Objects.requireNonNull(component);
		Objects.requireNonNull(connection);
		Objects.requireNonNull(configuration);

		this.path = itemUri.toString();

		this.component = component;
		this.connection = connection;
		this.configuration = configuration.clone();
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
	}

	@Override
	protected void doStop() throws Exception {
		this.component.disposed(this);
		super.doStop();
	}

	@Override
	public Producer createProducer() throws Exception {
		return new MiloClientProducer(this, this.connection, this.configuration);
	}

	@Override
	public Consumer createConsumer(final Processor processor) throws Exception {
		return new MiloClientConsumer(this, processor, this.connection, this.configuration);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public MiloClientConnection getConnection() {
		return this.connection;
	}

}
