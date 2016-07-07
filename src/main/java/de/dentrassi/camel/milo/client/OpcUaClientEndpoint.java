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

import java.util.Objects;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

public class OpcUaClientEndpoint extends DefaultEndpoint {

	private final OpcUaClientConnection connection;
	private final OpcUaClientComponent component;
	private final OpcUaClientEndpointConfiguration configuration;

	public OpcUaClientEndpoint(final String uri, final OpcUaClientComponent component,
			final OpcUaClientConnection connection, final OpcUaClientEndpointConfiguration configuration) {
		super(uri, component);

		Objects.requireNonNull(component);
		Objects.requireNonNull(connection);
		Objects.requireNonNull(configuration);

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
		return null;
	}

	@Override
	public Consumer createConsumer(final Processor processor) throws Exception {
		return new OpcUaClientConsumer(this, processor, this.connection, this.configuration);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public OpcUaClientConnection getConnection() {
		return this.connection;
	}

}
