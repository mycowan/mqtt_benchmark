package com.yourdomian;

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
import jp.sanix.TimeSynch;

import bb.util.MemoryMeasurer;

public class BenchmarkerSub {

    private static int maxNumClientsMAX = 100;
    private static int wait             = 5000;
    private static int loops            = 5;

    private static ArrayList<SubClientRunnable> subTests   = new ArrayList<SubClientRunnable>();
    private static ArrayList<Thread>            subThreads = new ArrayList<Thread>();

    private static ConcurrentMap<Integer, Long> timeTrail     = new ConcurrentHashMap<Integer, Long>();
    private static ConcurrentMap<Integer, Long> timeTrailAvgs = new ConcurrentHashMap<Integer, Long>();

    private static Long sum = 0L;

    // Server supported by Paho MQTT
    private static String broker = "m2m.eclipse.org";
    // private static String broker = "localhost";
    // private static String broker = "12.34.56.78";

    static String protocol = "tcp://";
    // static String protocol = "ssl://";

    public static String log    = "MQTT_Sub_stdout.log";
    public static String errlog = "MQTT_Sub_stderr.log";

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
    private static long  timeOffSet = 0L;

    // CONSTRUCTOR
    private BenchmarkerSub() {

    }

    // Creates a Singleton of Benchmarker
    private static class BenchmarkerHelper {
        private static final BenchmarkerSub INSTANCE = new BenchmarkerSub();

    }

    public static BenchmarkerSub getInstance() {
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

            setSubThreads(new ArrayList<Thread>());

            setTimeTrail(new ConcurrentHashMap<Integer, Long>());

            System.out.print("Setting up subscription clients ...");

            String id = "";

            for (int i = 0; i < maxNumClientsMAX; i++) {
                id = String.valueOf(looper).concat(String.valueOf(i));
                setupSubClient(id, getProtocol(), getBroker(), isQuietMode());
                System.out.print(".");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

            }

            System.out
                    .println("\nWaiting for sub clients to contact broker ...");

            Iterator<Thread> subThreadIterator = getSubThreads().iterator();
            while (subThreadIterator.hasNext()) {
                subThreadIterator.next().start();

            }

            // Need to wait for all the messages to be received
            try {
                Thread.sleep(getWait());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // Continue waiting for messages until the Enter is pressed
            System.out.println("Press <Enter> when ready to get loop "
                    + (looper + 1) + " results");
            try {
                System.in.read();
            } catch (IOException e) {
                // If we can't read we'll just exit
            }

            System.out.println("Average time for loop " + (looper + 1) + " : "
                    + averageTimeTrial(looper) + "ms");

        }

        System.out.println("\n================================");
        System.out.println("Total Average time for all " + loops + " loops : "
                + totalAvgTimeTrials() + "ms");

        try {
            cleanJvm();
        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    public static Long averageTimeTrial(int loop) {

        BenchmarkerSub.getInstance();
        Set<?> timeTrailSet = BenchmarkerSub.getTimeTrail().entrySet();

        Iterator<?> timeTrialIterator = timeTrailSet.iterator();

        Map.Entry thisTimeEntry;
        int thisTimeTrial;
        Long thisMessageTime;

        while (timeTrialIterator.hasNext()) {

            thisTimeEntry = (Map.Entry) timeTrialIterator.next();
            thisTimeTrial = (int) thisTimeEntry.getKey();
            thisMessageTime = (Long) thisTimeEntry.getValue();

            BenchmarkerSub.setSum(BenchmarkerSub.getSum() + thisMessageTime);
        }

        double msg_success = 0.0;

        if ((double) timeTrailSet.size() > 0) {
            msg_success = ((double) timeTrailSet.size()
                    / (double) maxNumClientsMAX) * 100;
        }

        System.out.println(
                "Messages successfully sent from pubClient to subClient via MQTT "
                        + msg_success + "%");

        BenchmarkerSub.getTimeTrailAvgs().put(loop,
                (BenchmarkerSub.getSum() / timeTrailSet.size()));

        Long sum = BenchmarkerSub.getSum();

        BenchmarkerSub.setSum(0L);

        if (timeTrailSet.size() != 0)

        {
            return (sum / timeTrailSet.size());
        }

        // There are no success time trails
        return 0L;

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
        BenchmarkerSub.broker = broker;
    }

    // public static String getClientId() {
    // return clientId;
    // }
    //
    // public static void setClientId(String clientId) {
    // Benchmarker.clientId = clientId;
    // }

    public static boolean isCleanSession() {
        return cleanSession;
    }

    public static void setCleanSession(boolean cleanSession) {
        BenchmarkerSub.cleanSession = cleanSession;
    }
    
    public static Clock getClock1() {
        return clock1;
    }

    public static void setClock1(Clock clock1) {
        BenchmarkerSub.clock1 = clock1;
    }

    public static int getLoops() {
        return BenchmarkerSub.loops;
    }

    public static void setLoops(int loops) {
        BenchmarkerSub.loops = loops;
    }

    public static ArrayList<Thread> getSubThreads() {
        return subThreads;
    }

    public static void setSubThreads(ArrayList<Thread> subThreads) {
        BenchmarkerSub.subThreads = subThreads;
    }

    public static Long getSum() {
        return sum;
    }

    public static void setSum(Long sum) {
        BenchmarkerSub.sum = sum;
    }

    public static int getMaxNumClients() {
        return BenchmarkerSub.maxNumClientsMAX;
    }

    public static void setMaxNumClients(int clients) {
        BenchmarkerSub.maxNumClientsMAX = clients;
    }

    // public static String getMessage() {
    // return message;
    // }
    //
    // public static void setMessage(String message) {
    // Benchmarker.message = message;
    // }

    public static int getPort() {
        return BenchmarkerSub.port;
    }

    public static void setPort(int port) {
        BenchmarkerSub.port = port;
    }

    public static String getProtocol() {
        return protocol;
    }

    public static void setProtocol(String protocol) {
        BenchmarkerSub.protocol = protocol;
    }

    public static int getQos() {
        return qos;
    }

    public static void setQos(int qos) {
        BenchmarkerSub.qos = qos;
    }

    public static boolean isQuietMode() {
        return quietMode;
    }

    public static void setQuietMode(boolean quietMode) {

        BenchmarkerSub.quietMode = quietMode;
    }

    public static ConcurrentMap<Integer, Long> getTimeTrailAvgs() {
        return timeTrailAvgs;
    }

    public static void setTimeTrailAvgs(
            ConcurrentMap<Integer, Long> timeTrailAvgs) {
        BenchmarkerSub.timeTrailAvgs = timeTrailAvgs;
    }

    public static Map<Integer, Long> getTimeTrail() {
        return timeTrail;
    }
    
    public static long getTimeOffSet() {
        return timeOffSet;
    }

    public static void setTimeOffSet(long t1, long t2) {
        BenchmarkerSub.timeOffSet = (t1 - t2);
    }

    public static void setTimeTrail(ConcurrentMap<Integer, Long> timeTrail) {
        BenchmarkerSub.timeTrail = timeTrail;
    }

    public static String getTopic() {
        return topic;
    }

    public static void setTopic(String topic) {
        BenchmarkerSub.topic = topic;
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

    protected static void setupSubClient(String id, String protocol,
            String broker, boolean quiteMode) {

        SubClientRunnable sub = new SubClientRunnable();
        sub.setQos(getQos());
        sub.setProtocol(protocol);
        sub.setBroker(broker);
        sub.setClientId("sub_" + id);
        sub.setTopic(getTopic() + id);
        sub.setCleanSession(isCleanSession());
        sub.setQuietMode(quietMode);
        subTests.add(sub);
        subThreads
                .add(new Thread((Runnable) subTests.get(subTests.size() - 1)));

    }

    public static Long totalAvgTimeTrials() {

        BenchmarkerSub.getInstance();
        Set<?> timeAvgsSet = BenchmarkerSub.getTimeTrailAvgs().entrySet();

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
     * Credit to Brent Boyer.
     * 
     * In order to give JVM Hotspot optimization a chance to complete, the test
     * is executed many times (with no recording of the execution time). This
     * phase lasts for 3 seconds
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

            setupSubClient(id, getProtocol(), getBroker(), true);

            // Need to wait for each messages to be ready
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            System.out.print(".");

            i++;
        }

        Iterator<Thread> subThreadIterator = subThreads.iterator();
        while (subThreadIterator.hasNext()) {
            subThreadIterator.next().start();

        }

        System.out.println("\nWarmup done.");

    }

    public static int getWait() {
        return wait;
    }

    public static void setWait(int wait) {
        BenchmarkerSub.wait = wait;
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
                + "    -q  Set logging reports to quiet mode (default is true\n"
                + "            Note: In logging mode the test doesn't work well "
                + "                  There's an overlapping of System.in.read() requests "
                + "                  You\'ll have to hit <Enter> many times."
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
