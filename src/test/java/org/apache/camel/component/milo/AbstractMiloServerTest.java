/*
 * Copyright (C) 2016 Jens Reimann <jreimann@redhat.com> and others
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

package org.apache.camel.component.milo;

import java.util.function.Consumer;

import org.apache.camel.CamelContext;
import org.apache.camel.component.milo.server.MiloServerComponent;
import org.apache.camel.component.mock.AssertionClause;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

public abstract class AbstractMiloServerTest extends CamelTestSupport {

	public static void testBody(final AssertionClause clause, final Consumer<DataValue> valueConsumer) {
		testBody(clause, DataValue.class, valueConsumer);
	}

	public static <T> void testBody(final AssertionClause clause, final Class<T> bodyClass, final Consumer<T> valueConsumer) {
		clause.predicate(exchange -> {
			final T body = exchange.getIn().getBody(bodyClass);
			valueConsumer.accept(body);
			return true;
		});
	}

	public static Consumer<DataValue> assertGoodValue(final Object expectedValue) {
		return value -> {
			assertNotNull(value);
			assertEquals(expectedValue, value.getValue().getValue());
			assertTrue(value.getStatusCode().isGood());
			assertFalse(value.getStatusCode().isBad());
		};
	}

	@Override
	protected CamelContext createCamelContext() throws Exception {
		final CamelContext context = super.createCamelContext();
		configureContext(context);
		return context;
	}

	protected void configureContext(final CamelContext context) {
		final MiloServerComponent server = context.getComponent("milo-server", MiloServerComponent.class);
		configureMiloServer(server);
	}

	protected void configureMiloServer(final MiloServerComponent server) {
		server.setBindAddresses("localhost");
		server.setBindPort(12685);
		server.setUserAuthenticationCredentials("foo:bar,foo2:bar2");
	}

}