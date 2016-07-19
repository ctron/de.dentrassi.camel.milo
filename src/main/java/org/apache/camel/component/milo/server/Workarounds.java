/*
 * Copyright (c) 2016 Kevin Herron and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 * 	http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * 	http://www.eclipse.org/org/documents/edl-v10.html.
 */

package org.apache.camel.component.milo.server;

import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfigBuilder;

public final class Workarounds {

	private Workarounds() {
	}

	/**
	 * Copy OPC UA server configuration
	 * <p>
	 * <em>Note: </em> This is copied from Eclipe Milo and calls to this method
	 * should be replaced with a call to
	 * {@link OpcUaServerConfig#copy(OpcUaServerConfig)} once it is available
	 * upstream.
	 * </p>
	 *
	 * @deprecated replace with a call to
	 *             {@link OpcUaServerConfig#copy(OpcUaServerConfig)} once it is
	 *             available
	 */
	@Deprecated
	static OpcUaServerConfigBuilder copy(final OpcUaServerConfig config) {
		final OpcUaServerConfigBuilder builder = new OpcUaServerConfigBuilder();

		// UaTcpStackServerConfig values
		builder.setServerName(config.getServerName());
		builder.setApplicationName(config.getApplicationName());
		builder.setApplicationUri(config.getApplicationUri());
		builder.setProductUri(config.getProductUri());
		builder.setCertificateManager(config.getCertificateManager());
		builder.setCertificateValidator(config.getCertificateValidator());
		builder.setExecutor(config.getExecutor());
		builder.setUserTokenPolicies(config.getUserTokenPolicies());
		builder.setSoftwareCertificates(config.getSoftwareCertificates());
		builder.setChannelConfig(config.getChannelConfig());
		builder.setStrictEndpointUrlsEnabled(config.isStrictEndpointUrlsEnabled());

		// OpcUaServerConfig values
		builder.setSecurityPolicies(config.getSecurityPolicies());
		builder.setHostname(config.getHostname());
		builder.setBindAddresses(config.getBindAddresses());
		builder.setBindPort(config.getBindPort());
		builder.setIdentityValidator(config.getIdentityValidator());
		builder.setBuildInfo(config.getBuildInfo());
		builder.setLimits(config.getLimits());

		return builder;
	}
}
