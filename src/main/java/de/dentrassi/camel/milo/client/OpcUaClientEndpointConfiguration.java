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

import java.net.URI;

public class OpcUaClientEndpointConfiguration implements Cloneable {

	private String host;

	private int port;

	private String namespaceUri;

	private String item;

	public OpcUaClientEndpointConfiguration() {
	}

	public OpcUaClientEndpointConfiguration(final OpcUaClientEndpointConfiguration other) {
		this.host = other.host;
		this.port = other.port;
		this.item = other.item;
		this.namespaceUri = other.namespaceUri;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(final String host) {
		this.host = host;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public String getNamespaceUri() {
		return this.namespaceUri;
	}

	public void setNamespaceUri(final String namespaceUri) {
		this.namespaceUri = namespaceUri;
	}

	public String getItem() {
		return this.item;
	}

	public void setItem(final String item) {
		this.item = item;
	}

	public String toConnectionCacheId() {
		return String.format("%s:%s", this.host, this.port);
	}

	@Override
	public OpcUaClientEndpointConfiguration clone() {
		return new OpcUaClientEndpointConfiguration(this);
	}

	public static OpcUaClientEndpointConfiguration fromUri(final URI uri) {

		if (!"tcp".equals(uri.getScheme())) {
			return null;
		}

		final OpcUaClientEndpointConfiguration result = new OpcUaClientEndpointConfiguration();

		// connection related

		result.setHost(uri.getHost());
		result.setPort(uri.getPort());

		// endpoint related

		result.setItem(uri.getPath().substring(1));

		return result;
	}
}
