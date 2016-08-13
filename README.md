# mqtt_benchmark

MQTTbenchmarker

Author: Mark Cowan m.y.cowan@gmail.com
2015

This application runs a series of benchmark mqtt connection tests
between a series of subscription and publish clients and a Mosquitto broker.

The average speed of the connections is measured and a percentage of successful
connections is given.


Syntax:

    Sample [-h] [-q <true|false>] [-t <topic>] [-m <1234>]
           [-l <1234>] [-s 0|1|2] -b <hostname|IP address>]
           [-p <brokerport>] [-c <true|false>]  [-w <1234>]

    -h  Print this help text and quit
    -q  Set logging reports to quiet mode (default is true)
    -t  Publish/subscribe to <topic> instead of the default
            (topic: "bench/mark/test/")
            Note: a final "/" is required for these tests
    -m  Use this max no. of clients - default (100)
    -l  Use this max no. of test loops - default (5)
    -s  Use this QoS instead of the default (2)
    -b  Use this name/IP address instead of the default (m2m.eclipse.org)
    -p  Use this port instead of the default (1883)
    -c  Connect to the server with a clean session (default is true)
            Warning: If clean session is false, you may get
                  Persistence already in use." Client Ids
                  must be unique. On the broker, to clear clients after one day
                  be sure to set - persistent_client_expiration 1d
                  in /etc/mosquitto/conf.d/mosquitto.conf
    -w  Use this wait between test loops period 
 instead of the default (5000ms)

Delimit strings containing spaces with ""

Publishers transmit a single message then disconnect from the server.
Subscribers remain connected to the server and receive appropriate
messages until <enter> is pressed.


The author is heavily indebted to the advice from this article

Robust Java benchmarking
24 June 2008
Brent Boyer of Elliptic Group, Inc.

http://www.ibm.com/developerworks/java/library/j-benchmark1/index.html
