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

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;

@UriEndpoint(scheme = "milo-client", syntax = "milo-client:tcp://user:password@host:port/path/to/service?itemId=item.id&namespaceUri=urn:foo:bar", title = "Milo based OPC UA Client", consumerClass = MiloClientConsumer.class, label = "iot")
public class MiloClientEndpoint extends DefaultEndpoint implements MiloClientItemConfiguration {

	/**
	 * The OPC UA server endpoint
	 */
	@UriPath
	@Metadata(required = "true")
	private final String endpointUri;

	/**
	 * The the node ID
	 */
	@UriParam
	@Metadata(required = "true")
	private String nodeId;

	/**
	 * The node ID namespace URI
	 */
	@UriParam
	private String namespaceUri;

	/**
	 * The index of the namespace.
	 * <p>
	 * Can be used as an alternative to the "namespaceUri"
	 * </p>
	 */
	@UriParam
	private Integer namespaceIndex;

	/**
	 * The sampling interval in milliseconds
	 */
	@UriParam
	private Double samplingInterval;

	/**
	 * The client configuration
	 */
	@UriParam
	private MiloClientConfiguration client;

	/**
	 * Default "await" setting for writes
	 */
	@UriParam
	boolean defaultAwaitWrites = false;

	private final MiloClientConnection connection;
	private final MiloClientComponent component;

	public MiloClientEndpoint(final String uri, final MiloClientComponent component,
			final MiloClientConnection connection, final String endpointUri) {
		super(uri, component);

		Objects.requireNonNull(component);
		Objects.requireNonNull(connection);
		Objects.requireNonNull(endpointUri);

		this.endpointUri = endpointUri;

		this.component = component;
		this.connection = connection;
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
		return new MiloClientProducer(this, this.connection, this, this.defaultAwaitWrites);
	}

	@Override
	public Consumer createConsumer(final Processor processor) throws Exception {
		return new MiloClientConsumer(this, processor, this.connection, this);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public MiloClientConnection getConnection() {
		return this.connection;
	}

	// item configuration

	@Override
	public String getNodeId() {
		return this.nodeId;
	}

	public void setNodeId(final String nodeId) {
		this.nodeId = nodeId;
	}

	@Override
	public String getNamespaceUri() {
		return this.namespaceUri;
	}

	public void setNamespaceUri(final String namespaceUri) {
		this.namespaceUri = namespaceUri;
	}

	@Override
	public Integer getNamespaceIndex() {
		return this.namespaceIndex;
	}

	public void setNamespaceIndex(final int namespaceIndex) {
		this.namespaceIndex = namespaceIndex;
	}

	@Override
	public Double getSamplingInterval() {
		return this.samplingInterval;
	}

	public void setSamplingInterval(final Double samplingInterval) {
		this.samplingInterval = samplingInterval;
	}

	public boolean isDefaultAwaitWrites() {
		return this.defaultAwaitWrites;
	}

	public void setDefaultAwaitWrites(final boolean defaultAwaitWrites) {
		this.defaultAwaitWrites = defaultAwaitWrites;
	}
}
