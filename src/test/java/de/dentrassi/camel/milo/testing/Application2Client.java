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

package de.dentrassi.camel.milo.testing;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfigBuilder;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;

import de.dentrassi.camel.milo.client.OpcUaClientComponent;

public class Application2Client {
	public static void main(final String[] args) throws Exception {

		// camel conext

		final CamelContext context = new DefaultCamelContext();

		// add paho

		// no need to register, gets auto detected
		// context.addComponent("paho", new PahoComponent());

		// OPC UA configuration

		final OpcUaServerConfigBuilder cfg = OpcUaServerConfig.builder();
		cfg.setCertificateManager(new DefaultCertificateManager());
		cfg.setCertificateValidator(new DefaultCertificateValidator(new File("certs")));
		cfg.setSecurityPolicies(EnumSet.of(SecurityPolicy.None));
		cfg.setIdentityValidator(new UsernameIdentityValidator(true, auth -> true));
		cfg.setUserTokenPolicies(Arrays.asList(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS,
				OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME));

		// add OPC UA

		context.addComponent("opcuaclient", new OpcUaClientComponent());

		// add routes

		context.addRoutes(new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				from("opcuaclient:tcp://localhost:12685/items-MyItem?namespaceUri=urn:camel")
						.log("From OPC UA: ${body}")
						.to("opcuaclient:tcp://localhost:12685/items-MyItem2?namespaceUri=urn:camel");
			}
		});

		// start

		context.start();

		// sleep

		while (true) {
			Thread.sleep(Long.MAX_VALUE);
		}
	}
}
