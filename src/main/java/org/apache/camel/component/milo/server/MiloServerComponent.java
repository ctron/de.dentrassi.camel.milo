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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.camel.Endpoint;
import org.apache.camel.component.milo.server.internal.CamelNamespace;
import org.apache.camel.impl.UriEndpointComponent;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfigBuilder;
import org.eclipse.milo.opcua.sdk.server.identity.IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.application.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;

/**
 * OPC UA Server based component
 */
public class MiloServerComponent extends UriEndpointComponent {

	private static final String URL_CHARSET = "UTF-8";

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

		cfg.setUserTokenPolicies(Arrays.asList(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS));

		DEFAULT_SERVER_CONFIG = cfg.build();
	}

	private String namespaceUri = CamelNamespace.NAMESPACE_URI;

	private final OpcUaServerConfigBuilder serverConfig;

	private OpcUaServer server;
	private CamelNamespace namespace;

	private final Map<String, MiloServerEndpoint> endpoints = new HashMap<>();

	private Boolean enableAnonymousAuthentication;

	private Map<String, String> userMap;

	private List<String> bindAddresses;

	public MiloServerComponent() {
		this(DEFAULT_SERVER_CONFIG);
	}

	public MiloServerComponent(final OpcUaServerConfig serverConfig) {
		super(MiloServerEndpoint.class);
		this.serverConfig = OpcUaServerConfig.copy(serverConfig != null ? serverConfig : DEFAULT_SERVER_CONFIG);
	}

	@Override
	protected void doStart() throws Exception {
		this.server = new OpcUaServer(buildServerConfig());

		this.namespace = this.server.getNamespaceManager().registerAndAdd(this.namespaceUri,
				ctx -> new CamelNamespace(ctx, this.server));

		super.doStart();
		this.server.startup();
	}

	/**
	 * Build the final server configuration, apply all complex configuration
	 *
	 * @return the new server configuration, never returns {@code null}
	 */
	private OpcUaServerConfig buildServerConfig() {

		if (this.userMap != null || this.enableAnonymousAuthentication != null) {
			// set identity validator

			final Map<String, String> userMap = this.userMap != null ? new HashMap<>(this.userMap)
					: Collections.emptyMap();
			final boolean allowAnonymous = this.enableAnonymousAuthentication != null
					? this.enableAnonymousAuthentication : false;
			final IdentityValidator identityValidator = new UsernameIdentityValidator(allowAnonymous, challenge -> {
				final String pwd = userMap.get(challenge.getUsername());
				if (pwd == null) {
					return false;
				}
				return pwd.equals(challenge.getPassword());
			});
			this.serverConfig.setIdentityValidator(identityValidator);
		}

		if (this.bindAddresses != null) {
			this.serverConfig.setBindAddresses(new ArrayList<>(this.bindAddresses));
		}

		// build final configuration

		return this.serverConfig.build();
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
	 * The URI of the namespace, defaults to <code>urn:org:apache:camel</code>
	 */
	public void setNamespaceUri(final String namespaceUri) {
		this.namespaceUri = namespaceUri;
	}

	/**
	 * The application name
	 */
	public void setApplicationName(final String applicationName) {
		Objects.requireNonNull(applicationName);
		this.serverConfig.setApplicationName(LocalizedText.english(applicationName));
	}

	/**
	 * The application URI
	 */
	public void setApplicationUri(final String applicationUri) {
		Objects.requireNonNull(applicationUri);
		this.serverConfig.setApplicationUri(applicationUri);
	}

	/**
	 * The product URI
	 */
	public void setProductUri(final String productUri) {
		Objects.requireNonNull(productUri);
		this.serverConfig.setProductUri(productUri);
	}

	/**
	 * The TCP port the server binds to
	 */
	public void setBindPort(final int port) {
		this.serverConfig.setBindPort(port);
	}

	/**
	 * Set whether strict endpoint URLs are enforced
	 */
	public void setStrictEndpointUrlsEnabled(final boolean strictEndpointUrlsEnforced) {
		this.serverConfig.setStrictEndpointUrlsEnabled(strictEndpointUrlsEnforced);
	}

	/**
	 * Server name
	 */
	public void setServerName(final String serverName) {
		this.serverConfig.setServerName(serverName);
	}

	/**
	 * Set user password combinations in the form of "user1:pwd1,user2:pwd2"
	 * <p>
	 * Usernames and passwords will be URL decoded
	 * </p>
	 */
	public void setUserAuthenticationCredentials(final String userAuthenticationCredentials) {
		if (userAuthenticationCredentials != null) {
			this.userMap = new HashMap<>();

			for (final String creds : userAuthenticationCredentials.split(",")) {
				final String[] toks = creds.split(":", 2);
				if (toks.length == 2) {
					try {
						this.userMap.put(URLDecoder.decode(toks[0], URL_CHARSET),
								URLDecoder.decode(toks[1], URL_CHARSET));
					} catch (final UnsupportedEncodingException e) {
						// FIXME: do log
					}
				}
			}
		} else {
			this.userMap = null;
		}
	}

	/**
	 * Enable anonymous authentication, disabled by default
	 */
	public void setEnableAnonymousAuthentication(final boolean enableAnonymousAuthentication) {
		this.enableAnonymousAuthentication = enableAnonymousAuthentication;
	}

	/**
	 * Set the addresses of the local addresses the server should bind to
	 */
	public void setBindAddresses(final String bindAddresses) {
		if (bindAddresses != null) {
			this.bindAddresses = Arrays.asList(bindAddresses.split(","));
		} else {
			this.bindAddresses = null;
		}
	}

	/**
	 * Server build info
	 */
	public void setBuildInfo(final BuildInfo buildInfo) {
		this.serverConfig.setBuildInfo(buildInfo);
	}
}
