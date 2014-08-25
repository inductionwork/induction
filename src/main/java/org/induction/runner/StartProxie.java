/**
 *
 */
package org.induction.runner;


import org.apache.log4j.Logger;
import org.induction.thread.CacheThread;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author Andrei Ilyin
 */
public class StartProxie {
    static ServerSocket s;
    public static final Logger LOGGER = Logger.getLogger(StartProxie.class);

    /**
     * @param args
     * @throws java.io.IOException
     * @throws java.net.UnknownHostException
     */
    public static void main(String[] args) throws UnknownHostException,
            IOException {

        try {
            s = new ServerSocket(3132, 1000, InetAddress.getByName("localhost"));
//			System.out.println(InetAddress.getByName("localhost"));
//			System.out.println("proxy is started");
            LOGGER.info(InetAddress.getByName("localhost"));
            LOGGER.info("proxy is started");
            while (true) {
                Socket client = null;
                try {

                    client = s.accept();
                    new CacheThread(client);

                } catch (Exception ex) {
                    client.close();
                }

            }
        } catch (Exception e) {
            LOGGER.info("main init error: " + e);

        } // by socket binding error

    }

}
