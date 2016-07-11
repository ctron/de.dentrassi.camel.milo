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

package org.apache.camel.component.milo.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfigBuilder;

import org.apache.camel.component.milo.server.internal.CamelNamespace;

/**
 * OPC UA Server based component
 */
public class MiloServerComponent extends UriEndpointComponent {

	private final OpcUaServer server;
	private final CamelNamespace namespace;

	private final Map<String, MiloServerEndpoint> endpoints = new HashMap<>();

	public MiloServerComponent(final OpcUaServerConfigBuilder cfgBuilder) {
		super(MiloServerEndpoint.class);

		this.server = new OpcUaServer(cfgBuilder.build());

		this.namespace = this.server.getNamespaceManager().registerAndAdd(CamelNamespace.NAMESPACE_URI,
				ctx -> new CamelNamespace(ctx, this.server));
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
		this.server.startup();
	}

	@Override
	protected void doStop() throws Exception {
		this.server.shutdown();
		super.doStop();
	}

	@Override
	protected Endpoint createEndpoint(final String uri, final String remaining, final Map<String, Object> parameters)
			throws Exception {
		synchronized (this) {
			if (remaining == null || remaining.isEmpty()) {
				return null;
			}

			MiloServerEndpoint endpoint = this.endpoints.get(remaining);

			if (endpoint == null) {
				endpoint = new MiloServerEndpoint(uri, remaining, this.namespace, this);
				setProperties(endpoint, parameters);
				this.endpoints.put(remaining, endpoint);
			}

			return endpoint;
		}
	}

}
