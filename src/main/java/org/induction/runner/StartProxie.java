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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Andrei Ilyin
 */
public class StartProxie {
    //    static ServerSocket s;
    public static final Logger LOGGER = Logger.getLogger(StartProxie.class);

    /**
     * @param args
     * @throws java.io.IOException
     * @throws java.net.UnknownHostException
     */
    public static void main(String[] args) throws UnknownHostException,
            IOException {

        try {
            ServerSocket serverSocket = new ServerSocket(3132, 50, InetAddress.getByName("localhost"));
//            ServerSocket serverSocket = new ServerSocket(3132, 1000, InetAddress.getByName("192.168.117.121"));
//            ServerSocket serverSocket = new ServerSocket(3132);

//            serverSocket.setSoTimeout(10 * 1000);
//			System.out.println(InetAddress.getByName("localhost"));
//			System.out.println("proxy is started");
            ExecutorService executorService = Executors.newFixedThreadPool(10);


            LOGGER.info(InetAddress.getByName("localhost"));
            LOGGER.info("proxy is started");
            while (true) {
                Socket client = null;
                try {
                    client = serverSocket.accept();
                    LOGGER.info("Got new request" + client.getInetAddress().toString());

                    executorService.submit(new CacheThread(client));

//                    CacheThread cacheThread = new CacheThread(client);
//                    cacheThread.start();
//                    cacheThread.join();
                    LOGGER.info("Complete request: " + client.getInetAddress().toString());
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                    client.close();
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);

        } // by socket binding error

    }

}
