**Note:** This component is now available in Apache Camel 2.19.0 out of the box. This repository will no longer be maintained. If you want the most recent version, please switch to Apache Camel.

# Apache Camel™ OPC UA Component [![Build status](https://api.travis-ci.org/ctron/de.dentrassi.camel.milo.svg "Travis Build Status")](https://travis-ci.org/ctron/de.dentrassi.camel.milo) [![Maven Central](https://img.shields.io/maven-central/v/de.dentrassi.camel.milo/camel-milo.svg)](https://search.maven.org/#search|ga|1|g%3A%22de.dentrassi.camel.milo%22%20AND%20a%3A%22camel-milo%22)

This is an Apache Camel component for providing OPC UA client and server functionality based on Eclipse Milo™.

## Install into Karaf

To install `camel-milo` into a Karaf 4+ container run the following commands inside the Karaf shell:

    feature:repo-add mvn:org.apache.camel.karaf/apache-camel/2.18.0/xml/features
    feature:repo-add mvn:de.dentrassi.camel.milo/feature/0.1.1/xml/features
    feature:install camel-milo

If you want to install use a different version of Camel, this is possible by selecting a different Camel
feature version:

    feature:repo-add mvn:org.apache.camel.karaf/apache-camel/2.17.0/xml/features

## Build your own

If you want to re-compile the component yourself try the following:

    git clone https://github.com/ctron/de.dentrassi.camel.milo.git
    cd de.dentrassi.camel.milo
    mvn clean install
    
Et voilà … you have a Karaf archive (KAR) at `features/milo/target/milo-*.kar`
which contains a Karaf repository with all the dependencies that you need.
