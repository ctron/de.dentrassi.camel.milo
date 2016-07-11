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

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.component.milo.server.internal.CamelNamespace;
import org.apache.camel.impl.UriEndpointComponent;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfigBuilder;
import org.eclipse.milo.opcua.sdk.server.identity.AnonymousIdentityValidator;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.application.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;

/**
 * OPC UA Server based component
 */
public class MiloServerComponent extends UriEndpointComponent {

	private static final OpcUaServerConfig DEFAULT_SERVER_CONFIG;
	static {
		final OpcUaServerConfigBuilder cfg = OpcUaServerConfig.builder();

		cfg.setCertificateManager(new DefaultCertificateManager());
		cfg.setCertificateValidator(new CertificateValidator() {

			@Override
			public void validate(final X509Certificate certificate) throws UaException {
				throw new UaException(StatusCodes.Bad_CertificateUseNotAllowed);
			}

			@Override
			public void verifyTrustChain(final X509Certificate certificate, final List<X509Certificate> chain)
					throws UaException {
				throw new UaException(StatusCodes.Bad_CertificateUseNotAllowed);
			}

		});
		cfg.setSecurityPolicies(EnumSet.of(SecurityPolicy.None));
		cfg.setIdentityValidator(new AnonymousIdentityValidator());

		cfg.setUserTokenPolicies(Arrays.asList(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS));

		DEFAULT_SERVER_CONFIG = cfg.build();
	}

	private String namespaceUri = CamelNamespace.NAMESPACE_URI;

	private final OpcUaServerConfig serverConfig;

	private OpcUaServer server;
	private CamelNamespace namespace;

	private final Map<String, MiloServerEndpoint> endpoints = new HashMap<>();

	public MiloServerComponent() {
		this(DEFAULT_SERVER_CONFIG);
	}

	public MiloServerComponent(final OpcUaServerConfig serverConfig) {
		super(MiloServerEndpoint.class);
		this.serverConfig = serverConfig;
	}

	@Override
	protected void doStart() throws Exception {
		this.server = new OpcUaServer(this.serverConfig);

		this.namespace = this.server.getNamespaceManager().registerAndAdd(this.namespaceUri,
				ctx -> new CamelNamespace(ctx, this.server));

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

	/**
	 * Set the namespace URI, defaults to <code>urn:org:apache:camel</code>
	 */
	public void setNamespaceUri(final String namespaceUri) {
		this.namespaceUri = namespaceUri;
	}

}
