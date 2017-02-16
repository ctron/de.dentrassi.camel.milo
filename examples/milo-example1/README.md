In order to use this example download and run Karaf:

    karaf> feature:repo-add mvn:org.apache.camel.karaf/apache-camel/2.18.0/xml/features
    karaf> feature:install aries-blueprint
    karaf> feature:install shell-compat camel camel-blueprint camel-paho
    karaf> kar:install file:location/to/milo-2.18.0-SNAPSHOT.kar
    karaf> bundle:install -s file:location/to/examples/milo-2.18.0-SNAPSHOT.jar
    
Now the Camel route should be running and you can look at the log:

    karaf> log:display
    
You should see something like:

    2016-10-14 12:50:38,138 | INFO  | -599474557429248 | milo1                            | 133 - org.apache.camel.camel-core - 2.18.0 | iot.eclipse.org - temperature: 23.59
