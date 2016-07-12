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

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

@UriParams
public class MiloClientConfiguration implements Cloneable {

	private static final String DEFAULT_APPLICATION_URI = "http://camel.apache.org/EclipseMilo";

	private static final String DEFAULT_APPLICATION_NAME = "Apache Camel adapter for Eclipse Milo";

	private static final String DEFAULT_PRODUCT_URI = "http://camel.apache.org/EclipseMilo";

	private String endpointUri;

	@UriParam
	private String clientId;

	@UriParam(label = "client", defaultValue = DEFAULT_APPLICATION_NAME)
	private String applicationName = DEFAULT_APPLICATION_NAME;

	@UriParam(label = "client", defaultValue = DEFAULT_APPLICATION_URI)
	private String applicationUri = DEFAULT_APPLICATION_URI;

	@UriParam(label = "client", defaultValue = DEFAULT_PRODUCT_URI)
	private String productUri = DEFAULT_PRODUCT_URI;

	@UriParam(label = "client")
	private Long requestTimeout;

	@UriParam(label = "client")
	private Long channelLifetime;

	@UriParam(label = "client")
	private String sessionName;

	@UriParam(label = "client")
	private Long sessionTimeout;

	@UriParam(label = "client")
	private Long maxPendingPublishRequests;

	@UriParam(label = "client")
	private Long maxResponseMessageSize;

	@UriParam(label = "client")
	private Boolean secureChannelReauthenticationEnabled;

	public MiloClientConfiguration() {
	}

	public MiloClientConfiguration(final MiloClientConfiguration other) {
		this.clientId = other.clientId;
		this.endpointUri = other.endpointUri;
		this.applicationName = other.applicationName;
		this.productUri = other.productUri;
		this.requestTimeout = other.requestTimeout;
	}

	public void setEndpointUri(final String endpointUri) {
		this.endpointUri = endpointUri;
	}

	public String getEndpointUri() {
		return this.endpointUri;
	}

	/**
	 * A virtual client id to force the creation of a new connection instance
	 */
	public void setClientId(final String clientId) {
		this.clientId = clientId;
	}

	public String getClientId() {
		return this.clientId;
	}

	/**
	 * The application name
	 */
	public void setApplicationName(final String applicationName) {
		this.applicationName = applicationName;
	}

	public String getApplicationName() {
		return this.applicationName;
	}

	/**
	 * The application URI
	 */
	public void setApplicationUri(final String applicationUri) {
		this.applicationUri = applicationUri;
	}

	public String getApplicationUri() {
		return this.applicationUri;
	}

	/**
	 * The product URI
	 */
	public void setProductUri(final String productUri) {
		this.productUri = productUri;
	}

	public String getProductUri() {
		return this.productUri;
	}

	/**
	 * Request timeout in milliseconds
	 */
	public void setRequestTimeout(final Long reconnectTimeout) {
		this.requestTimeout = reconnectTimeout;
	}

	public Long getRequestTimeout() {
		return this.requestTimeout;
	}

	/**
	 * Channel lifetime in milliseconds
	 */
	public void setChannelLifetime(final Long channelLifetime) {
		this.channelLifetime = channelLifetime;
	}

	public Long getChannelLifetime() {
		return this.channelLifetime;
	}

	/**
	 * Session name
	 */
	public void setSessionName(final String sessionName) {
		this.sessionName = sessionName;
	}

	public String getSessionName() {
		return this.sessionName;
	}

	/**
	 * Session timeout in milliseconds
	 */
	public void setSessionTimeout(final Long sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	public Long getSessionTimeout() {
		return this.sessionTimeout;
	}

	/**
	 * The maximum number of pending publish requests
	 */
	public void setMaxPendingPublishRequests(final Long maxPendingPublishRequests) {
		this.maxPendingPublishRequests = maxPendingPublishRequests;
	}

	public Long getMaxPendingPublishRequests() {
		return this.maxPendingPublishRequests;
	}

	/**
	 * The maximum number of bytes a response message may have
	 */
	public void setMaxResponseMessageSize(final Long maxResponseMessageSize) {
		this.maxResponseMessageSize = maxResponseMessageSize;
	}

	public Long getMaxResponseMessageSize() {
		return this.maxResponseMessageSize;
	}

	/**
	 * Whether secure channel re-authentication is enabled
	 */
	public void setSecureChannelReauthenticationEnabled(final Boolean secureChannelReauthenticationEnabled) {
		this.secureChannelReauthenticationEnabled = secureChannelReauthenticationEnabled;
	}

	public Boolean getSecureChannelReauthenticationEnabled() {
		return this.secureChannelReauthenticationEnabled;
	}

	@Override
	public MiloClientConfiguration clone() {
		return new MiloClientConfiguration(this);
	}

	public String toCacheId() {
		if (this.clientId != null && !this.clientId.isEmpty()) {
			return this.endpointUri + "|" + this.clientId;
		} else {
			return this.endpointUri;
		}
	}
}
