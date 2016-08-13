package com.yourdomian;

/**
 * @author Mark Cowan
 * https://github.com/mycowan/mqtt_benchmark.git 
 */

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
* Get MemoryMeasurer fron Brent Boyer
* https://github.com/magro/elliptic-benchmark/blob/master/src/main/java/bb/util/MemoryMeasurer.java
*/

import bb.util.MemoryMeasurer;

public class BenchmarkerPub {

    private static int maxNumClientsMAX = 100;
    private static int wait             = 5000;
    private static int loops            = 5;

    private static ArrayList<PubClientRunnable> pubTests   = new ArrayList<PubClientRunnable>();
    private static ArrayList<Thread>            pubThreads = new ArrayList<Thread>();

    private static ConcurrentMap<Integer, Long> timeTrail     = new ConcurrentHashMap<Integer, Long>();
    private static ConcurrentMap<Integer, Long> timeTrailAvgs = new ConcurrentHashMap<Integer, Long>();

    private static Long sum = 0L;

    // Server supported by Paho MQTT
    private static String broker = "m2m.eclipse.org";
    // private static String broker = "localhost";
    // private static String broker = "12.34.56.78";

    static String protocol = "tcp://";
    // static String protocol = "ssl://";

    public static String log    = "MQTT_Pub_stdout.log";
    public static String errlog = "MQTT_Pub_stderr.log";

    // Default settings:
    private static boolean quietMode    = true;
    private static String  topic        = "bench/mark/test/";
    // private static String message = ""; // Cannot be modified for tests
    private static int     qos          = 2;
    private static int     port         = 1883;
    // private static String clientId = null; // Cannot be modified for tests
    // private static String subTopic = "Default/Sample/#";
    // private static String pubTopic = "Default/Sample/Java/v3";
    // Non durable subscriptions
    private static boolean cleanSession = true;
    // private static boolean ssl = false;
    // private static String password = null;
    //
    // private static String userName = null;
    
    // For Internet time synchronizing
    private static Clock clock1;
    private static long timeOffSet = 0L;

    // CONSTRUCTOR
    private BenchmarkerPub() {

    }

    // Creates a Singleton of Benchmarker
    private static class BenchmarkerHelper {
        private static final BenchmarkerPub INSTANCE = new BenchmarkerPub();

    }

    public static BenchmarkerPub getInstance() {
        return BenchmarkerHelper.INSTANCE;
    }

    public static void main(String[] args) throws Exception {
        
        // Set timeOffSet against an   NIST Internet Time Server
        
        /**
         * "All users should ensure that their software NEVER queries a
         * server more frequently than once every 4 seconds."
         * http://tf.nist.gov/tf-cgi/servers.cgi
         */
        ZoneId tz = ZoneId.of("UTC");
        Instant timestamp = Instant.ofEpochMilli(TimeSynch.getSynchedTime());
        setClock1(Clock.fixed(timestamp, tz));
        
        setTimeOffSet(getClock1().millis(), System.currentTimeMillis());

        try {
            FileOutputStream fout = new FileOutputStream(log);
            FileOutputStream ferr = new FileOutputStream(errlog);

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

        // Parse the arguments -
        for (int i = 0; i < args.length; i++) {
            // Check this is a valid argument
            if (args[i].length() == 2 && args[i].startsWith("-")) {
                char arg = args[i].charAt(1);
                // Handle arguments that take no value
                switch (arg) {
                case 'h':
                case '?':
                    printHelp();
                    return;
                }

                // Now handle arguments that have a value and
                // check each value is valid
                if (i == args.length - 1 || args[i + 1].charAt(0) == '-') {
                    System.out
                            .println("Missing value for argument: " + args[i]);
                    printHelp();
                    return;
                }
                switch (arg) {
                case 't':
                    String tmp = args[++i];
                    if (tmp.lastIndexOf('/') == (tmp.length() - 1)) {
                        setTopic(tmp);
                    } else {

                        System.out.println(
                                "Missing final \"/\" for: " + args[i - 1]);
                        printHelp();
                        return;
                    }

                    break;
                case 'l':
                    setLoops(Integer.parseInt(args[++i]));
                    break;
                case 'm':
                    setMaxNumClients(Integer.parseInt(args[++i]));
                    break;
                case 's':
                    int j = Integer.parseInt(args[++i]);
                    if (j >= 0 && j <= 2) {
                        setQos(j);
                        break;
                    }
                    System.out.println("Invalid QoS level for: " + args[i - 1]);
                    printHelp();
                    return;

                case 'b':
                    setBroker(args[++i]);
                    break;
                case 'p':
                    setPort(Integer.parseInt(args[++i]));
                    break;
                case 'c':
                    String cleanBool = args[++i];

                    if (cleanBool.equals("true")) {
                        setCleanSession(true);
                    } else if (cleanBool.equals("false")) {
                        setCleanSession(false);
                    } else {

                        System.out.println(
                                "Incorrect value for argument: " + args[i - 1]);
                        printHelp();
                        return;
                    }
                    break;
                case 'q':

                    String quietBool = args[++i];

                    if (quietBool.equals("true")) {
                        setQuietMode(true);
                    } else if (quietBool.equals("false")) {
                        setQuietMode(false);
                    } else {

                        System.out.println(
                                "Incorrect value for argument: " + args[i - 1]);
                        printHelp();
                        return;
                    }
                    break;
                case 'w':
                    setWait(Integer.parseInt(args[++i]));
                    break;
                // case 'k':
                // System.getProperties().put("javax.net.ssl.keyStore",
                // args[++i]);
                // break;
                // case 'w':
                // System.getProperties().put("javax.net.ssl.keyStorePassword",
                // args[++i]);
                // break;
                // case 'r':
                // System.getProperties().put("javax.net.ssl.trustStore",
                // args[++i]);
                // break;
                // case 'v':
                // ssl = Boolean.valueOf(args[++i]).booleanValue();
                // break;
                // case 'u':
                // userName = args[++i];
                // break;
                // case 'z':
                // password = args[++i];
                // break;
                default:
                    System.out.println("Unrecognised argument: " + args[i]);
                    printHelp();
                    return;
                }
            } else {
                System.out.println("Unrecognised argument: " + args[i]);
                printHelp();
                return;
            }
        }

        try {
            cleanJvm();
            // warmupJvm();
        } catch (Exception e) {

            e.printStackTrace();
        }

        // Need to wait for all the warmup messages to be received
        try {
            Thread.sleep(getWait());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        for (int looper = 0; looper < getLoops(); looper++) {

            System.out.println("\nLoop No. " + (looper + 1));

            setPubThreads(new ArrayList<Thread>());

            setTimeTrail(new ConcurrentHashMap<Integer, Long>());

            System.out.print("Setting up publisher clients ...");

            String id = "";

            for (int i = 0; i < maxNumClientsMAX; i++) {
                id = String.valueOf(looper).concat(String.valueOf(i));
                setUpPubClient(id, getProtocol(), getBroker(), isQuietMode());
                System.out.print(".");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

            }

            System.out.print("\nWaiting to send all loop " + (looper + 1)
                    + " messages to broker ... ");

            Iterator<Thread> pubThreadIterator = getPubThreads().iterator();
            while (pubThreadIterator.hasNext()) {
                pubThreadIterator.next().start();
                System.out.print(".");
            }

            // Need to wait for all the messages to be received
            try {
                Thread.sleep(getWait());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // Continue waiting to send next group of messages
            if ((looper + 1) < getLoops()) {
                System.out.println("\nPress <Enter> to send messages for"
                        + " loop  " + (looper + 2));
                try {
                    System.in.read();
                } catch (IOException e) {
                    // If we can't read we'll just exit
                }
            }
            if ((looper + 1) < getLoops()) {
                System.out.println("\nOk, sending loop  " + (looper + 2));
            }
        }

        System.out.println("\n================================");
        System.out
                .println("\nAll messages have been sent. Testing is finished");

        try {
            cleanJvm();
        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    public static Long averageTimeTrial(int loop) {

        BenchmarkerPub.getInstance();
        Set<?> timeTrailSet = BenchmarkerPub.getTimeTrail().entrySet();

        Iterator<?> timeTrialIterator = timeTrailSet.iterator();

        Map.Entry thisTimeEntry;
        int thisTimeTrial;
        Long thisMessageTime;

        while (timeTrialIterator.hasNext()) {

            thisTimeEntry = (Map.Entry) timeTrialIterator.next();
            thisTimeTrial = (int) thisTimeEntry.getKey();
            thisMessageTime = (Long) thisTimeEntry.getValue();

            BenchmarkerPub.setSum(BenchmarkerPub.getSum() + thisMessageTime);
        }

        System.out
                .println(
                        "Messages successfully sent from pubClient to subClient via MQTT "
                                + (((double) timeTrailSet.size()
                                        / (double) maxNumClientsMAX) * 100)
                                + "%");

        BenchmarkerPub.getTimeTrailAvgs().put(loop,
                (BenchmarkerPub.getSum() / timeTrailSet.size()));

        Long sum = BenchmarkerPub.getSum();

        BenchmarkerPub.setSum(0L);

        return (sum / timeTrailSet.size());

    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
        buffer.putLong(x);
        return buffer.array();
    }

    public long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
        buffer.put(bytes);
        buffer.flip();// need flip
        return buffer.getLong();
    }

    public static String getBroker() {
        return broker;
    }

    public static void setBroker(String broker) {
        BenchmarkerPub.broker = broker;
    }

    // public static String getClientId() {
    // return clientId;
    // }
    //
    
    // public static void setClientId(String clientId) {
    // Benchmarker.clientId = clientId;
    // }

    public static Clock getClock1() {
        return clock1;
    }

    public static void setClock1(Clock clock1) {
        BenchmarkerPub.clock1 = clock1;
    }

    public static boolean isCleanSession() {
        return cleanSession;
    }

    public static void setCleanSession(boolean cleanSession) {
        BenchmarkerPub.cleanSession = cleanSession;
    }

    public static int getLoops() {
        return BenchmarkerPub.loops;
    }

    public static void setLoops(int loops) {
        BenchmarkerPub.loops = loops;
    }

    public static Long getSum() {
        return sum;
    }

    public static void setSum(Long sum) {
        BenchmarkerPub.sum = sum;
    }

    public static int getMaxNumClients() {
        return BenchmarkerPub.maxNumClientsMAX;
    }

    public static void setMaxNumClients(int clients) {
        BenchmarkerPub.maxNumClientsMAX = clients;
    }

    // public static String getMessage() {
    // return message;
    // }
    //
    // public static void setMessage(String message) {
    // Benchmarker.message = message;
    // }

    public static ArrayList<Thread> getPubThreads() {
        return pubThreads;
    }

    public static void setPubThreads(ArrayList<Thread> pubThreads) {
        BenchmarkerPub.pubThreads = pubThreads;
    }

    public static int getPort() {
        return BenchmarkerPub.port;
    }

    public static void setPort(int port) {
        BenchmarkerPub.port = port;
    }

    public static String getProtocol() {
        return protocol;
    }

    public static void setProtocol(String protocol) {
        BenchmarkerPub.protocol = protocol;
    }

    public static int getQos() {
        return qos;
    }

    public static void setQos(int qos) {
        BenchmarkerPub.qos = qos;
    }

    public static boolean isQuietMode() {
        return quietMode;
    }

    public static void setQuietMode(boolean quietMode) {

        BenchmarkerPub.quietMode = quietMode;
    }

    public static ConcurrentMap<Integer, Long> getTimeTrailAvgs() {
        return timeTrailAvgs;
    }

    public static void setTimeTrailAvgs(
            ConcurrentMap<Integer, Long> timeTrailAvgs) {
        BenchmarkerPub.timeTrailAvgs = timeTrailAvgs;
    }

    public static Map<Integer, Long> getTimeTrail() {
        return timeTrail;
    }

    public static void setTimeTrail(ConcurrentMap<Integer, Long> timeTrail) {
        BenchmarkerPub.timeTrail = timeTrail;
    }

    public static long getTimeOffSet() {
        return timeOffSet;
    }

    public static void setTimeOffSet(long t1, long t2) {
        BenchmarkerPub.timeOffSet = (t1 - t2);
    }

    public static String getTopic() {
        return topic;
    }

    public static void setTopic(String topic) {
        BenchmarkerPub.topic = topic;
    }

    /**
     * Credit to Brent Boyer - This attempts to restore the JVM to a pristine
     * state by aggressively performing object finalization and garbage
     * collection.
     */
    protected static void cleanJvm() {
        MemoryMeasurer.restoreJvm();
    }

    protected static String getSaltString() {
        String SALTCHARS = "0123456789";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 9) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();

        return saltStr;

    }

    protected static void setUpPubClient(String id, String protocol,
            String broker, boolean quiteMode) {
        
        PubClientRunnable pub = new PubClientRunnable();
        pub.setQos(getQos());
        pub.setProtocol(protocol);
        pub.setBroker(broker);
        pub.setClientId("pub_" + id);
        pub.setTopic(getTopic() + id);
        /**
         *  Do NOT sent message here. There is at least a 6500ms
         *  difference between now and when the message gets sent
         */
//        pub.setMessage(longToBytes(System.currentTimeMillis() + getTimeOffSet()));
        pub.setQuietMode(quiteMode);
        pubTests.add(pub);
        pubThreads
                .add(new Thread((Runnable) pubTests.get(pubTests.size() - 1)));
    }

    public static Long totalAvgTimeTrials() {

        BenchmarkerPub.getInstance();
        Set<?> timeAvgsSet = BenchmarkerPub.getTimeTrailAvgs().entrySet();

        Iterator<?> timeAvgsIterator = timeAvgsSet.iterator();

        Map.Entry thisAvgEntry;
        int thisTrialAvg;
        Long thisAvg = 0L, sum = 0L;

        while (timeAvgsIterator.hasNext()) {

            thisAvgEntry = (Map.Entry) timeAvgsIterator.next();
            thisTrialAvg = (int) thisAvgEntry.getKey();
            thisAvg = (Long) thisAvgEntry.getValue();

            sum = sum + thisAvg;
        }

        return (sum / timeAvgsSet.size());

    }

    /**
     * Credit to Brent Boyer. In order to give JVM Hotspot optimization a chance
     * to complete, the test is executed many times (with no recording of the
     * execution time). This phase lasts for 3 seconds
     * 
     * @throws Exception
     */
    protected static void warmupJvm() throws Exception {
        cleanJvm();

        System.out.print(
                "Performing many executions of task to fully warmup the JVM...");

        Long t = System.currentTimeMillis() + 3000;

        int i = 0;

        String id = "warmUp_";
        while (System.currentTimeMillis() < t) {

            id = id.concat(String.valueOf(i));

            setUpPubClient(id, getProtocol(), getBroker(), true);

            // Need to wait for each messages to be ready
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            System.out.print(".");

            i++;
        }

        Iterator<Thread> pubThreadIterator = pubThreads.iterator();
        while (pubThreadIterator.hasNext()) {
            pubThreadIterator.next().start();
        }

        System.out.println("\nWarmup done.");

    }

    public static int getWait() {
        return wait;
    }

    public static void setWait(int wait) {
        BenchmarkerPub.wait = wait;
    }

    /****************************************************************/
    /* End of Benchmarker methods */
    /****************************************************************/

    static void printHelp() {
        System.out.println("Syntax:\n\n"
                + "    Sample [-h] [-q <true|false>] [-t <topic>] [-m <1234>]\n"
                + "           [-l <1234>] [-s 0|1|2] -b <hostname|IP address>]\n"
                + "           [-p <brokerport>] [-c <true|false>]  [-w <1234>]\n\n"
                + "    -h  Print this help text and quit\n"
                + "    -q  Set logging reports to quiet mode (default is false)\n"
                + "    -t  Publish/subscribe to <topic> instead of the default\n"
                + "            (topic: \"bench/mark/test/\")\n"
                + "            Note: a final \"/\" is required for these tests\n"
                + "    -m  Use this max no. of clients - default (100)\n"
                + "    -l  Use this max no. of test loops - default (5)\n"
                + "    -s  Use this QoS instead of the default (2)\n"
                + "    -b  Use this name/IP address instead of the default (m2m.eclipse.org)\n"
                + "    -p  Use this port instead of the default (1883)\n"
                + "    -c  Connect to the server with a clean session (default is true)\n"
                + "            Warning: If clean session is false, you may get "
                + "                  \"Persistence already in use.\" Client Ids "
                + "                  must be unique. On the broker, to clear clients after one day"
                + "                  be sure to set - persistent_client_expiration 1d "
                + "                  in /etc/mosquitto/conf.d/mosquitto.conf "
                + "    -w  Use this wait between test loops period \n instead of the default (5000ms)\n\n"
                // + " \n\n Security Options \n" + " -u Username \n"
                // + " -z Password \n" + " \n\n SSL Options \n"
                // + " -v SSL enabled; true - (default is false) "
                // + " -k Use this JKS format key store to verify the client\n"
                // + " -w Passpharse to verify certificates in the keys store\n"
                // + " -r Use this JKS format keystore to verify the server\n"
                // + " If javax.net.ssl properties have been set only the -v
                // flag needs to be set\n"
                + "Delimit strings containing spaces with \"\"\n\n"
                + "Publishers transmit a single message then disconnect from the server.\n"
                + "Subscribers remain connected to the server and receive appropriate\n"
                + "messages until <enter> is pressed.\n\n");
    }

}
