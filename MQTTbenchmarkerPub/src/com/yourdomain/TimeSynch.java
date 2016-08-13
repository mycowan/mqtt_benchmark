package com.yourdomain;

/**
 * @author Mark Cowan
 * https://github.com/mycowan/mqtt_benchmark.git 
 */

import org.apache.commons.net.ntp.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;

public class TimeSynch {

    /**
     * "All users should ensure that their software NEVER queries a server more
     * frequently than once every 4 seconds."
     * http://tf.nist.gov/tf-cgi/servers.cgi
     * 
     * Note: time-a.nist.gov & time-b.nist.gov are usually busy
     * and   time-d.nist.gov - All services via IPV6. PV_Sensor has only IPV4
     */
    private static String       TIME_SERVER = "time-d.nist.gov";
    // private String TIME_SERVER = "jp.pool.ntp.org"
    private static NTPUDPClient timeClient  = new NTPUDPClient();

    public static long getSynchedTime() {
        InetAddress inetAddress;
//        Date time = null;
        NtpV3Packet message = null;
        long serverTime = 0L;
        long benchMarkTime = 0;
        try {
            inetAddress = InetAddress.getByName(TIME_SERVER);
            TimeInfo timeInfo = timeClient.getTime(inetAddress);

            // compute offset/delay
            timeInfo.computeDetails();

            Long offSet = (timeInfo.getOffset() == null) ? 0L
                    : timeInfo.getOffset();
            Long delay = (timeInfo.getDelay() == null) ? 0L
                    : timeInfo.getDelay();
            message = timeInfo.getMessage();
            serverTime = message.getTransmitTimeStamp().getTime();

            benchMarkTime = (serverTime + offSet) - delay;

//            time = new Date(benchMarkTime);

//            System.out.println("Time from " + TIME_SERVER + " " + time + " : "
//                    + benchMarkTime);
//            System.out.println("Time here " + System.currentTimeMillis());
//
//            System.out.println("A difference of "
//                    + (benchMarkTime - System.currentTimeMillis()));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return benchMarkTime;
    }

}
