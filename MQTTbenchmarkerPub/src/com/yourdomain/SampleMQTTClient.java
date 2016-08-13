
/*******************************************************************************
	 * Copyright (c) 2009, 2014 IBM Corp.
	 *
	 * All rights reserved. This program and the accompanying materials
	 * are made available under the terms of the Eclipse Public License v1.0
	 * and Eclipse Distribution License v1.0 which accompany this distribution. 
	 *
	 * The Eclipse Public License is available at 
	 *    http://www.eclipse.org/legal/epl-v10.html
	 * and the Eclipse Distribution License is available at 
	 *   http://www.eclipse.org/org/documents/edl-v10.php.
	 *
	 * Contributors:
	 *    Dave Locke - initial API and implementation and/or initial documentation
	 */

package com.yourdomain;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

/**
 * A sample application that demonstrates how to use the Paho MQTT v3.1 Client
 * blocking API.
 *
 * It can be run from the command line in one of two modes: - as a publisher,
 * sending a single message to a topic on the server - as a subscriber,
 * listening for messages from the server
 *
 * There are three versions of the sample that implement the same features but
 * do so using using different programming styles:
 * <ol>
 * <li>Sample (this one) which uses the API which blocks until the operation
 * completes</li>
 * <li>SampleAsyncWait shows how to use the asynchronous API with waiters that
 * block until an action completes</li>
 * <li>SampleAsyncCallBack shows how to use the asynchronous API where events
 * are used to notify the application when an action completes
 * <li>
 * </ol>
 *
 * If the application is run with the -h parameter then info is displayed that
 * describes all of the options / parameters.
 */
public class SampleMQTTClient implements MqttCallback {

    private Long timeIn  = null;
    private Long timeOut = null;

    /**
     * The main entry point of the sample.
     *
     * This method handles parsing of the arguments specified on the
     * command-line before performing the specified action.
     */
    public static void main() {

        try {
            FileOutputStream fout = new FileOutputStream(BenchmarkerPub.log);
            FileOutputStream ferr = new FileOutputStream(BenchmarkerPub.errlog);

            MultiOutputStream multiOut = new MultiOutputStream(System.out,
                    fout);
            MultiOutputStream multiErr = new MultiOutputStream(System.err,
                    ferr);

            PrintStream stdout = new PrintStream(multiOut);
            PrintStream stderr = new PrintStream(multiErr);

            System.setOut(stdout);
            System.setErr(stderr);
        } catch (FileNotFoundException ex) {
            System.err.println("Could not create/open the logging file");
        }

        // Default settings:
        boolean quietMode = false;
        String action = "publish";
        String topic = "";
        String message = "Default Message from blocking Paho MQTTv3 Java client sample";
        int qos = 2;
        String broker = "m2m.eclipse.org"; // Server supported by Paho
        // String broker = "localhost";
        // String broker = "12.34.56.78"; 
        int port = 1883;
        String clientId = null;
        String subTopic = "Default/Sample/#";
        String pubTopic = "Default/Sample/Java/v3";
        boolean cleanSession = true; // Non durable subscriptions
        boolean ssl = false;
        String password = null;
        String userName = null;

        // Validate the provided arguments
        if (!action.equals("publish") && !action.equals("subscribe")) {
            System.out.println("Invalid action: " + action);
            return;
        }
        if (qos < 0 || qos > 2) {
            System.out.println("Invalid QoS: " + qos);
            return;
        }
        if (topic.equals("")) {
            // Set the default topic according to the specified action
            if (action.equals("publish")) {
                topic = pubTopic;
            } else {
                topic = subTopic;
            }
        }

        String protocol = "tcp://";

        if (ssl) {
            protocol = "ssl://";
        }

        String url = protocol + broker + ":" + port;

        if (clientId == null || clientId.equals("")) {
            clientId = "SampleJavaV3_" + action;
        }

        // With a valid set of arguments, the real work of
        // driving the client API can begin
        try {
            // Create an instance of this class
            SampleMQTTClient sampleClient = new SampleMQTTClient(url, clientId,
                    cleanSession, quietMode, userName, password);

            // Perform the requested action
            if (action.equals("publish")) {
                sampleClient.publish(topic, qos);
            } else if (action.equals("subscribe")) {
                sampleClient.subscribe(topic, qos);
            }
        } catch (MqttException me) {
            // Display full details of any exception that occurs
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    // Private instance variables
    private MqttClient         client;
    private String             brokerUrl;
    private boolean            quietMode;
    private MqttConnectOptions conOpt;
    private boolean            clean;
    private String             password;
    private String             userName;

    /**
     * Constructs an instance of the sample client wrapper
     * 
     * @param brokerUrl
     *            the url of the server to connect to
     * @param clientId
     *            the client id to connect with
     * @param cleanSession
     *            clear state at end of connection or not (durable or
     *            non-durable subscriptions)
     * @param quietMode
     *            whether debug should be printed to standard out
     * @param userName
     *            the username to connect with
     * @param password
     *            the password for the user
     * @throws MqttException
     */
    public SampleMQTTClient(String brokerUrl, String clientId,
            boolean cleanSession, boolean quietMode, String userName,
            String password) throws MqttException {
        this.brokerUrl = brokerUrl;
        this.quietMode = quietMode;
        this.clean = cleanSession;
        this.password = password;
        this.userName = userName;
        // This sample stores in a temporary directory... where messages
        // are stored until the message has been delivered to the server.
        // A real application ought to store them somewhere
        // where they are not likely to get deleted or tampered with
        String tmpDir = System.getProperty("java.io.tmpdir");
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(
                tmpDir);

        try {
            // Construct the connection options object that contains connection
            // parameters
            // such as cleanSession and LWT
            conOpt = new MqttConnectOptions();
            conOpt.setCleanSession(clean);
            if (password != null) {
                conOpt.setPassword(this.password.toCharArray());
            }
            if (userName != null) {
                conOpt.setUserName(this.userName);
            }

            // Construct an MQTT blocking mode client
            client = new MqttClient(this.brokerUrl, clientId, dataStore);

            // Set this wrapper as the callback handler
            client.setCallback(this);

        } catch (MqttException e) {
            e.printStackTrace();
            log("Unable to set up client: " + e.toString());
            System.exit(1);
        }
    }

    /**
     * Publish / send a message to an MQTT server
     * 
     * @param topicName
     *            the name of the topic to publish to
     * @param qos
     *            the quality of service to delivery the message at (0,1,2)
     * @throws MqttException
     */
    public void publish(String topicName, int qos) throws MqttException {

        // Connect to the MQTT server
        log("Connecting to " + brokerUrl + " with client ID "
                + client.getClientId());
        client.connect(conOpt);
        log("Connected");

        // Create and configure a message
        MqttMessage message = new MqttMessage(longToBytes(
                System.currentTimeMillis() + BenchmarkerPub.getTimeOffSet()));

        message.setQos(qos);

        // Send the message to the server, control is not returned until
        // it has been delivered to the server meeting the specified
        // quality of service.
        client.publish(topicName, message);

        // Disconnect the client
        client.disconnect();
        log("Disconnected");
    }

    /**
     * Subscribe to a topic on an MQTT server Once subscribed this method waits
     * for the messages to arrive from the server that match the subscription.
     * It continues listening for messages until the enter key is pressed.
     * 
     * @param topicName
     *            to subscribe to (can be wild carded)
     * @param qos
     *            the maximum quality of service to receive messages at for this
     *            subscription
     * @throws MqttException
     */
    public void subscribe(String topicName, int qos) throws MqttException {

        // Connect to the MQTT server
        client.connect(conOpt);
        log("Connected to " + brokerUrl + " with client ID "
                + client.getClientId());

        // Subscribe to the requested topic
        // The QoS specified is the maximum level that messages will be sent to
        // the client at.
        // For instance if QoS 1 is specified, any messages originally published
        // at QoS 2 will
        // be downgraded to 1 when delivering to the client but messages
        // published at 1 and 0
        // will be received at the same level they were published at.
        log("Subscribing to topic \"" + topicName + "\" qos " + qos);
        client.subscribe(topicName, qos);

        // Continue waiting for messages until the Enter is pressed
        log("Press <Enter> to exit");
        try {
            System.in.read();
        } catch (IOException e) {
            // If we can't read we'll just exit
        }

        // Disconnect the client from the server
        client.disconnect();
        log("Disconnected");
    }

    /**
     * Utility method to handle logging. If 'quietMode' is set, this method does
     * nothing
     * 
     * @param message
     *            the message to log
     */
    private void log(String message) {
        if (!quietMode) {
            System.out.println(message);
        }
    }

    /****************************************************************/
    /* Methods to implement the MqttCallback interface */
    /****************************************************************/

    /**
     * @see MqttCallback#connectionLost(Throwable)
     */
    public void connectionLost(Throwable cause) {
        // Called when the connection to the server has been lost.
        // An application may choose to implement reconnection
        // logic at this point. This sample simply exits.
        log("Connection to " + brokerUrl + " lost!" + cause);
        System.exit(1);
    }

    /**
     * @see MqttCallback#deliveryComplete(IMqttDeliveryToken)
     */
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Called when a message has been delivered to the
        // server. The token passed in here is the same one
        // that was passed to or returned from the original call to publish.
        // This allows applications to perform asynchronous
        // delivery without blocking until delivery completes.
        //
        // This sample demonstrates asynchronous deliver and
        // uses the token.waitForCompletion() call in the main thread which
        // blocks until the delivery has completed.
        // Additionally the deliveryComplete method will be called if
        // the callback is set on the client
        //
        // If the connection to the server breaks before delivery has completed
        // delivery of a message will complete after the client has
        // re-connected.
        // The getPendingTokens method will provide tokens for any messages
        // that are still to be delivered.
    }

    /**
     * @see MqttCallback#messageArrived(String, MqttMessage)
     */
    public void messageArrived(String topic, MqttMessage message)
            throws MqttException {
        // Called when a message arrives from the server that matches any
        // subscription made by the client
        this.setTimeIn(System.currentTimeMillis());
        String time = new Timestamp(this.getTimeIn()).toString();
        log("Time: " + time + "  Topic: " + topic + "  TimeLag in ms: "
                + this.getTimeDifference(message.getPayload(), topic)
                + "  QoS: " + message.getQos());
        // this.getTimeDifference(message.getPayload(), topic);
    }

    public Long getTimeIn() {
        return timeIn;
    }

    public void setTimeIn(Long timeNow) {
        this.timeIn = timeNow;
    }

    public Long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(Long timeNow) {
        this.timeOut = timeNow;
    }

    public String getTimeDifference(byte[] nanoTimeSent, String messageNo) {

        Long dif = (System.nanoTime() - bytesToLong(nanoTimeSent));

        int i = messageNo.lastIndexOf('/');
        if (i != -1) {
            messageNo = messageNo.substring(i + 1);
        }

        Integer j = Integer.parseInt(messageNo);

        BenchmarkerPub.getTimeTrail().put(j, dif);

        log("Benchmarker " + messageNo + " : " + BenchmarkerPub.getTimeTrail()
                .get(Integer.parseInt(messageNo)));
        return String.valueOf(dif);
    }

    public long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
        buffer.put(bytes);
        buffer.flip();// need flip
        return buffer.getLong();
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
        buffer.putLong(x);
        return buffer.array();
    }
}
