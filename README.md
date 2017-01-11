# Apache Camel™ OPC UA Component [![Build status](https://api.travis-ci.org/ctron/de.dentrassi.camel.milo.svg "Travis Build Status")](https://travis-ci.org/ctron/de.dentrassi.camel.milo) [![Maven Central](https://img.shields.io/maven-central/v/de.dentrassi.camel.milo/camel-milo.svg)](https://search.maven.org/#search|ga|1|g%3A%22de.dentrassi.camel.milo%22%20AND%20a%3A%22camel-milo%22)

This is an Apache Camel component for providing OPC UA client and server functionality based on Eclipse Milo™.

## Build your own

If you want to re-compile the component yourself try the following:

    git clone https://github.com/ctron/de.dentrassi.camel.milo.git
    cd de.dentrassi.camel.milo
    mvn clean install
    
Et voilà … you have a Karaf archive (KAR) at `features/milo/target/milo-0.1.0-SNAPSHOT.kar`
which contains a Karaf repository with all the dependencies that you need.
