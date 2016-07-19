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

package org.apache.camel.component.milo.server;

import java.util.function.Consumer;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.AssertionClause;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.Test;

/**
 * Unit tests for milo server component which require an actual connection
 */
public class MiloServerComponentRemoteTest extends CamelTestSupport {

	private static final String DIRECT_START_1 = "direct:start1";
	private static final String DIRECT_START_2 = "direct:start2";

	private static final String MILO_SERVER_ITEM_1 = "milo-server:myitem1";
	private static final String MILO_SERVER_ITEM_2 = "milo-server:myitem2";

	private static final String MILO_CLIENT_BASE_C1 = "milo-client:tcp://foo:bar@localhost:12685";
	private static final String MILO_CLIENT_BASE_C2 = "milo-client:tcp://foo2:bar2@localhost:12685";

	private static final String MILO_CLIENT_ITEM_C1_1 = MILO_CLIENT_BASE_C1 + "?nodeId=items-myitem1&namespaceUri="
			+ MiloServerComponent.DEFAULT_NAMESPACE_URI;
	private static final String MILO_CLIENT_ITEM_C1_2 = MILO_CLIENT_BASE_C1 + "?nodeId=items-myitem2&namespaceUri="
			+ MiloServerComponent.DEFAULT_NAMESPACE_URI;

	private static final String MILO_CLIENT_ITEM_C2_1 = MILO_CLIENT_BASE_C2 + "?nodeId=items-myitem1&namespaceUri="
			+ MiloServerComponent.DEFAULT_NAMESPACE_URI;
	private static final String MILO_CLIENT_ITEM_C2_2 = MILO_CLIENT_BASE_C2 + "?nodeId=items-myitem2&namespaceUri="
			+ MiloServerComponent.DEFAULT_NAMESPACE_URI;

	private static final String MOCK_TEST_1 = "mock:test1";
	private static final String MOCK_TEST_2 = "mock:test2";

	@EndpointInject(uri = MOCK_TEST_1)
	protected MockEndpoint test1Endpoint;

	@EndpointInject(uri = MOCK_TEST_2)
	protected MockEndpoint test2Endpoint;

	@Produce(uri = DIRECT_START_1)
	protected ProducerTemplate producer1;

	@Produce(uri = DIRECT_START_2)
	protected ProducerTemplate producer2;

	@Override
	protected RoutesBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			@Override
			public void configure() throws Exception {

				final MiloServerComponent server = getContext().getComponent("milo-server", MiloServerComponent.class);
				configureMiloServer(server);

				from(MILO_SERVER_ITEM_1).to(MOCK_TEST_1);
				from(MILO_SERVER_ITEM_2).to(MOCK_TEST_2);

				from(DIRECT_START_1).to(MILO_CLIENT_ITEM_C1_1);
				from(DIRECT_START_2).to(MILO_CLIENT_ITEM_C1_2);
			}
		};
	}

	protected void configureMiloServer(final MiloServerComponent server) {
		server.setBindAddresses("localhost");
		server.setBindPort(12685);
		server.setUserAuthenticationCredentials("foo:bar,foo2:bar2");
	}

	public static void testBody(final AssertionClause clause, final Consumer<DataValue> valueConsumer) {
		testBody(clause, DataValue.class, valueConsumer);
	}

	public static <T> void testBody(final AssertionClause clause, final Class<T> bodyClass,
			final Consumer<T> valueConsumer) {
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

	@Test
	public void testWrite() throws Exception {
		// item 1
		this.test1Endpoint.setExpectedCount(2);
		testBody(this.test1Endpoint.message(0), assertGoodValue("Foo"));
		testBody(this.test1Endpoint.message(1), assertGoodValue("Foo2"));

		// item 2
		this.test2Endpoint.setExpectedCount(0);

		// send
		this.producer1.sendBody(new Variant("Foo"));
		this.producer1.sendBody(new Variant("Foo2"));

		// assert
		this.assertMockEndpointsSatisfied();
	}
}
