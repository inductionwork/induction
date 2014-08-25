/**
 *
 */
package org.induction.pool;

import org.induction.thread.CacheThread;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import static org.induction.constantdata.Constants.PROXY_ERROR;
import static org.induction.utils.Transform.getPath;

/**
 * @author Andrei Ilyin
 */
public class ThreadPool {
    /*
     * Pool Map
     */
    private ConcurrentHashMap<CacheThread, Integer> threadPool = new ConcurrentHashMap<CacheThread, Integer>();
    /*
     * Singleton Instance
     */
    public static final ThreadPool INSTANCE = new ThreadPool();

    private ThreadPool() {

    }

    public static ThreadPool getInstance() {
        return INSTANCE;
    }

    public void addOrPut(Socket s) throws Exception {
        InputStream is; // входящий поток от сокета
        OutputStream os; // исходящий поток от сокета
        is = s.getInputStream();
        os = s.getOutputStream();

        byte buf[] = new byte[64 * 1024];
        int r = is.read(buf);

        String header = new String(buf, 0, r);
        if (header.indexOf("GET ", 0) == 0) {
            String path = getPath(header);
            if (path == null) {
                printError(os, "invalid request:\n" + header);
                return;
            }
        }

    }

    // печатает ошибку прокси
    protected void printError(OutputStream os, String err) throws Exception {
        os.write((new String(PROXY_ERROR + err)).getBytes());
    }
}
