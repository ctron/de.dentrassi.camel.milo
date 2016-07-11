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

public class MiloClientEndpointConfiguration implements Cloneable {

	private String host;

	private int port;

	private String namespaceUri;

	private String item;

	public MiloClientEndpointConfiguration() {
	}

	public MiloClientEndpointConfiguration(final MiloClientEndpointConfiguration other) {
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
	public MiloClientEndpointConfiguration clone() {
		return new MiloClientEndpointConfiguration(this);
	}

	public static MiloClientEndpointConfiguration fromUri(final URI uri) {

		if (!"tcp".equals(uri.getScheme())) {
			return null;
		}

		final MiloClientEndpointConfiguration result = new MiloClientEndpointConfiguration();

		// connection related

		result.setHost(uri.getHost());
		result.setPort(uri.getPort());

		// endpoint related

		result.setItem(uri.getPath().substring(1));

		return result;
	}
}
