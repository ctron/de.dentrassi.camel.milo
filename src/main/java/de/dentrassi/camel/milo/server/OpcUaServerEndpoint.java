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

package de.dentrassi.camel.milo.server;

import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;

import de.dentrassi.camel.milo.server.internal.CamelNamespace;
import de.dentrassi.camel.milo.server.internal.CamelServerItem;

/**
 * OPC UA Server based endpoint
 */
@UriEndpoint(scheme = "opcuaserver", syntax = "opcuaserver:ItemId", title = "OPC UA Server", consumerClass = OpcUaServerConsumer.class, label = "iot")
public class OpcUaServerEndpoint extends DefaultEndpoint {

	@UriPath(label = "Item ID", description = "The ID of the item")
	@Metadata(required = "true")
	private String itemId;

	private final CamelNamespace namespace;

	private CamelServerItem item;

	public OpcUaServerEndpoint(final String uri, final String itemId, final CamelNamespace namespace,
			final Component component) {
		super(uri, component);
		this.itemId = itemId;
		this.namespace = namespace;
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
		this.item = this.namespace.getOrAddItem(this.itemId);
	}

	@Override
	protected void doStop() throws Exception {
		if (this.item == null) {
			this.item.dispose();
			this.item = null;
		}
		super.doStop();
	}

	@Override
	protected void doShutdown() throws Exception {
		// FIXME: need to call back to component?
		super.doShutdown();
	}

	@Override
	public Producer createProducer() throws Exception {
		return new OpcUaServerProducer(this, this.item);
	}

	@Override
	public Consumer createConsumer(final Processor processor) throws Exception {
		return new OpcUaServerConsumer(this, processor, this.item);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	/**
	 * ID of the item
	 *
	 * @param itemId
	 *            the new ID of the item
	 */
	public void setItemId(final String itemId) {
		this.itemId = itemId;
	}

	/**
	 * Get the ID of the item
	 *
	 * @return the ID of the item
	 */
	public String getItemId() {
		return this.itemId;
	}

}
