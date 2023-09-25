
# Buffering MQTT Message Publisher


**BufferingPublisher** is a Java program designed for publishing MQTT (Message Queuing Telemetry Transport) messages to a broker using a buffering approach. It utilizes the HiveMQ MQTT client library to achieve this functionality.
    
## Purpose

The primary purpose of this program is to efficiently publish MQTT messages to a broker with the option to buffer messages and control their publication rate. This can be useful for various testing and evaluation scenarios.


## How It Works

1. The program establishes an MQTT client connection to the specified broker.
3. The program generates messages with sequential numbers and publishes them to the broker at a controlled rate.
5. The program prints status messages, including the generation and publication of messages.
6. The program handles interruptions gracefully.


## Building From The Source

`./gradlew clean shadowJar`


---
Please note that this program is provided under the Apache License 2.0, so ensure compliance with the license terms if you choose to use or distribute it.

For more information about the HiveMQ MQTT client library, refer to the [HiveMQ MQTT Client Library documentation](https://hivemq.github.io/hivemq-mqtt-client/).

## License

This program is licensed under the Apache License, Version 2.0. You can find a copy of the license [here](http://www.apache.org/licenses/LICENSE-2.0).
