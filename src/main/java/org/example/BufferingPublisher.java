/*
 * Copyright 2018-present HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.concurrent.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import picocli.CommandLine;

@CommandLine.Command(name = "publish", mixinStandardHelpOptions = true,
        description = "publishes N messages to M topics")
public class BufferingPublisher implements Callable<Integer> {
    final private long startTime = System.nanoTime();
    
    final private static AtomicInteger sequenceNumber = new AtomicInteger(0);

    @CommandLine.Option(names = {"--host"}, description = "MQTT broker host", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    String host = "localhost";
    @CommandLine.Option(names = {"--port"}, description = "MQTT broker port", defaultValue = "1883",showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    int port = 1883;

    @CommandLine.Option(names = {"--secure"}, description = "Use TLS", defaultValue = "false",showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    boolean secure =false;

    @CommandLine.Option(names = {"--user"}, description = "Username", defaultValue = "", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    String user = "";

    @CommandLine.Option(names = {"--password"}, description = "Passphrase", arity = "0..1", interactive = true, defaultValue = "", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    String password = "";

    @CommandLine.Option(names = {"--topicPrefix"}, description = "Topic prefix", defaultValue = "test/",showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    String topicPrefix="test/";

    @CommandLine.Option(names = {"--messageNumber"}, description = "How many messages", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    int messageNumber=10;

    @CommandLine.Option(names = {"--messageSizeBytes"}, description = "Size of payload of MQTT PUBLISH packet in bytes", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    int messageSizeBytes=10;

    @CommandLine.Option(names = {"--qos"}, description = "QoS", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    int qos=1;

    @CommandLine.Option(names = {"--verbose"}, description = "Verbose output", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    boolean verbose = false;

    @CommandLine.Option(names = {"--clientId"}, description = "ClientId", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    String clientId="pub_"+ startTime;

    @Override
    public Integer call() {
        if (verbose) {
            System.out.println("clientId: " + clientId);
            System.out.println("host: " + host);
            System.out.println("messageNumber: " + messageNumber);
            System.out.println("messageSizeBytes: " + messageSizeBytes);
            System.out.println("password: " + password);
            System.out.println("port: " + port);
            System.out.println("qos: " + qos);
            System.out.println("secure: " + secure);
            System.out.println("topicPrefix: " + topicPrefix);
            System.out.println("user: " + user);
            System.out.println("verbose: " + verbose);
        }


        System.out.println("Hello!");
        try {
            // Create an MQTT client
            final Mqtt5AsyncClient client =
                    Mqtt5Client.builder()
                            .serverHost("localhost")
                            .serverPort(1883)
                            .automaticReconnectWithDefaultConfig()
                            .addConnectedListener(context -> System.out.println("connected " + LocalTime.now()))
                            .addDisconnectedListener(context -> System.out.println("disconnected " + LocalTime.now()))
                            .buildAsync();

            BlockingQueue<Integer> numberQueue = new LinkedBlockingQueue<>();

            // Create a CompletableFuture to generate the sequence every 10 seconds
            CompletableFuture<Void> sequenceGeneratorFuture = CompletableFuture.runAsync(() -> {
                while (true) {
                    try {
                        TimeUnit.SECONDS.sleep(10);
                        int number = sequenceNumber.incrementAndGet();
                        numberQueue.offer(number);
                        System.out.println("Generated: " + number + ". Queue size: " + numberQueue.size());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, Executors.newSingleThreadExecutor());

            // Create a CompletableFuture to consume and print numbers from the queue
            CompletableFuture<Void> numberConsumerFuture = CompletableFuture.runAsync(() -> {
                while (true) {
                    try {
                        int number = numberQueue.take();
                        client.publishWith()
                                .topic("topic/"+number)
                                .payload(("Hello " + number).getBytes(StandardCharsets.UTF_8))
                                .qos(MqttQos.AT_LEAST_ONCE).send()
                                .thenAccept(pubAck ->
                                        System.out.println("Published: " + number))
                                .exceptionally(ex -> {
                                    System.err.println("Failed to publish: " + number);
                                    numberQueue.offer(number); // Return the number back to the queue
                                    return null;
                                })
                                .join();
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted!!!");
                        Thread.currentThread().interrupt();
                    }
                }
            }, Executors.newSingleThreadExecutor());

            System.out.println("Connecting as: "+client.getConfig().getClientIdentifier());
            client.connect()
                    .thenAccept(connAck -> System.out.println("Connected as: " + connAck.getAssignedClientIdentifier()))
                    .thenCompose(v -> CompletableFuture.allOf(sequenceGeneratorFuture, numberConsumerFuture))
                    .thenCompose(v -> client.disconnect())
                    .thenAccept(v -> System.out.println("Disconnected: " + client.getConfig().getClientIdentifier()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}






