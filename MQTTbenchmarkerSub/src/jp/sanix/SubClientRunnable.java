package com.yourdomain;

import java.nio.charset.Charset;

import org.eclipse.paho.client.mqttv3.MqttException;
import jp.sanix.SampleMQTTClient;;

/**
 * @author root
 *
 */
class SubClientRunnable implements Runnable {
    // For MQTT Testing
    // Default settings
    protected boolean quietMode = false;
    protected String  action    = "publish";
    protected String  message   = "Benchmark test of MQTTv3 Java client sample";
    protected int     qos       = 2;
    // Server supported by Paho MQTT
    // protected broker = "m2m.eclipse.org";
    protected String  broker    = "localhost";
    // protected String broker  = "12.34.56.78"
    protected String  protocol  = "tcp://";
    protected int     port      = 1883;
    protected String  clientId  = "JavaSampleSub";
    protected String  topic  = "bench/mark/test/";

    protected boolean cleanSession = true; // Non durable subscriptions
    protected boolean ssl          = false;
    protected String  password     = null;
    protected String  userName     = null;

    // Subscription Client
    protected SampleMQTTClient sub = null;

    private Boolean stop = false;

    private int counter = 0;

    public void run() {

        try {

            String url = getProtocol().concat(getBroker());
            SampleMQTTClient sub = new SampleMQTTClient(url,
                    getClientId(), isCleanSession(), isQuietMode(),
                    getUserName(), getPassword());
            sub.subscribe(getTopic(), getQos());

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
        return message.getBytes(Charset.forName("UTF-8"));
    }

    public void setMessage(String message) {
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
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