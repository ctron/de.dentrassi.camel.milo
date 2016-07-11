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

public class MiloClientConfiguration implements Cloneable {

	private String endpointUri;

	private String applicationName = "Apache Camel adapter for Eclipse Milo";

	private String applicationUri = "http://camel.apache.org/EclipseMilo";

	private String productUri = "http://camel.apache.org/EclipseMilo";

	public MiloClientConfiguration() {
	}

	public MiloClientConfiguration(final MiloClientConfiguration other) {
		this.endpointUri = other.endpointUri;
		this.applicationName = other.applicationName;
	}

	public void setEndpointUri(final String endpointUri) {
		this.endpointUri = endpointUri;
	}

	public String getEndpointUri() {
		return this.endpointUri;
	}

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

	public void setProductUri(final String productUri) {
		this.productUri = productUri;
	}

	public String getProductUri() {
		return this.productUri;
	}

	@Override
	public MiloClientConfiguration clone() {
		return new MiloClientConfiguration(this);
	}

	public String toCacheId() {
		return this.endpointUri;
	}
}
