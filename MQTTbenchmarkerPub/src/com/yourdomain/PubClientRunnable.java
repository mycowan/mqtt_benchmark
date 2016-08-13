package com.yourdomain;

/**
 * @author Mark Cowan 
 */

import java.nio.charset.Charset;

import org.eclipse.paho.client.mqttv3.MqttException;
import jp.sanix.SampleMQTTClient;

public class PubClientRunnable implements Runnable {
    // For MQTT Testing
    // Default settings
    protected boolean quietMode    = false;
    protected String  action       = "publish";
    protected byte[]  message      = null;
    protected int     qos          = 2;
//    protected broker = "m2m.eclipse.org";
    protected String  broker       = "localhost";
//    protected String  broker       = "12.34.56.78";
    protected String  protocol     = "tcp://";
    protected int     port         = 1883;
    protected String  clientId     = "JavaSamplePub";
    protected String  topic     = "bench/mark/test";
    protected boolean cleanSession = true;             // Non durable
                                                       // subscriptions
    protected boolean ssl          = false;
    protected String  password     = null;
    protected String  userName     = null;

    // Publisher Client
    protected SampleMQTTClient pub = null;

    private int counter = 0;

    public void run() {

        try {
            String url = getProtocol().concat(getBroker());
            SampleMQTTClient pub = new SampleMQTTClient(url,
                    getClientId(), isCleanSession(), isQuietMode(),
                    getUserName(), getPassword());
            pub.publish(getTopic(), getQos());
        } catch (MqttException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public boolean isQuietMode() {
        return quietMode;
    }

    public void setQuietMode(boolean quietMode) {
        this.quietMode = quietMode;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    public String getBroker() {
        return broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(String qos) {
        this.qos = Integer.parseInt(qos);
    }

}
