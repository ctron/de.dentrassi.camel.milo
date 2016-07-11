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

package org.apache.camel.component.milo;

import java.net.URI;
import java.util.Objects;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;

@UriEndpoint(scheme = "opcuaclient", syntax = "opcuaclient:tcp://host:port/ItemId?namespaceUri=urn:foo:bar", title = "OPC UA Client", consumerClass = MiloConsumer.class, label = "iot")
public class MiloEndpoint extends DefaultEndpoint {

	/**
	 * The main path
	 */
	@UriPath
	@Metadata(required = "true")
	private final String path;

	private final MiloConnection connection;
	private final MiloComponent component;
	private final MiloEndpointConfiguration configuration;

	public MiloEndpoint(final String uri, final URI itemUri, final MiloComponent component,
						final MiloConnection connection, final MiloEndpointConfiguration configuration) {
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
		return new MiloProducer(this, this.connection, this.configuration);
	}

	@Override
	public Consumer createConsumer(final Processor processor) throws Exception {
		return new MiloConsumer(this, processor, this.connection, this.configuration);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public MiloConnection getConnection() {
		return this.connection;
	}

}
