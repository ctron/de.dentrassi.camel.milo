# Apache Camel OPC UA Component [![Build status](https://api.travis-ci.org/ctron/de.dentrassi.camel.milo.svg "Travis Build Status")](https://travis-ci.org/ctron/de.dentrassi.camel.milo)

This is an Apache Camel component for OPC UA client and server based on Eclipse Milo™.

## Build your own

As of now there is no release of Eclipse Milo™, this is not since Milo is not stable enough
but simply due to the way releases at the Eclipse Foundation work.

So if you want to use Milo and the Apache Camel™ Milo adapter, you need to build it yourself.
At least for the moment.

    git clone https://github.com/eclipse/milo.git
    cd milo
    mvn clean install
    cd ..
    git clone https://github.com/ctron/de.dentrassi.camel.milo.git
    cd de.dentrassi.camel.milo
    mvn clean install
    
Et voilà … you have a Karaf archive (KAR) at `features/milo/target/milo-0.1.0-SNAPSHOT.kar`
which contains a Karaf repository with all the dependencies that you need.