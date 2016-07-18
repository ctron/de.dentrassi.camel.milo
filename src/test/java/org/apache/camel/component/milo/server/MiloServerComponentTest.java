package org.apache.camel.component.milo.server;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class MiloServerComponentTest extends CamelTestSupport {

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("milo-server:myitem").to("mock:test");
            }
        };
    }

    @Test
    public void shouldStartComponent() {
    }

}
