/**
 *
 */
package org.induction.thread;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.induction.constantdata.Constants.PROXY_ERROR;
import static org.induction.utils.Transform.extractFromHeader;
import static org.induction.utils.Transform.getPath;

/**
 * пытается подключиться как сервер на адрес localhost порт 3128 // после чего
 * сидит и ждёт подключений от браузера // каждое новое подключение передаёт в
 * обработку отдельному вычислительному // потоку
 *
 * @author Andrei Ilyin
 */

public class CacheThread extends Thread {

    public static final Logger LOGGER = Logger.getLogger(CacheThread.class);

    Socket s; // сокет подключения
    InputStream is2; // входящий поток от сокета
    OutputStream os; // исходящий поток от сокета

    String method;
    String path;
    boolean fromCache;
    String error;

    public CacheThread(Socket s) {
        System.out.println("\nNew Thread created!");
        this.s = s;
        setDaemon(true);
        setPriority(NORM_PRIORITY);
        start();
    }

    // загружает из сети страничку с одновременным кэшированием её на диск
    // странички в кэше храняться прямо с HTTP заголовком
    @SuppressWarnings("deprecation")
    protected void from_net(String header, String host, int port, String path)
            throws Exception {
        LOGGER.info("new socked (host, port):" + host + ":" + port + "\n");
        Socket sc = new Socket(host, port);
        sc.setSoTimeout(5000);
        sc.getOutputStream().write(header.getBytes());

        LOGGER.info("Sended from from_net");
        InputStream is = sc.getInputStream();

        File f = new File((new File(path)).getParent());
        if (!f.exists())
            f.mkdirs();

        FileOutputStream fos = new FileOutputStream(path);


        byte reply[] = new byte[64 * 1024];
        int bytes_read;
        try {
            while ((bytes_read = is.read(reply)) != -1) {
                fos.write(reply, 0, bytes_read);
                os.write(reply, 0, bytes_read);
                os.flush();
            }
        } catch (SocketTimeoutException e) {
            LOGGER.info("SocketTimeoutException: Read timed out in socked (host, port):" + host + ":" + port
                    + "\n");
            LOGGER.info("HEADER:" + header);
        } finally {
            fos.close();
            os.close();
            is.close();
            sc.close();
        }

    }

    // вытаскивает из HTTP заголовка хост, порт соединения и путь до файла кэша,
    // после чего вызывает ф-ию загрузки из сети
    protected void from_net(String header) throws Exception {
        System.out.println("Try from net!");
        String host = extractFromHeader(header, "Host:", "\n"), path = getPath(header);
        if ((host == null) || (path == null)) {
            printError("invalid request:\n" + header);
            return;
        }
        LOGGER.info("Transform path: " + path);

        int port = host.indexOf(":", 0);
        if (port < 0)
            port = 80;
        else {
            port = Integer.parseInt(host.substring(port + 1));
            host = host.substring(0, port);
        }
        LOGGER.info("From net Host+port: " + host + ":" + port);
        from_net(header, host, port, path);
    }

    // загружает из кэша файл и выдаёт его
    // если во входящем HTTP заголовке стоит "Pragma: no-cache"
    // или такого файла в кэше нет, то вызывается ф-ия загрузки из сети
    protected void from_cache(String header) throws Exception {
        LOGGER.info("Try from cache!");
        this.method = "GET";
        String path = getPath(header);
        LOGGER.info("Transform path: " + path);
        if (path == null) {
            this.error = "invalid request:\n" + header;
            LOGGER.info(this);
            printError("invalid request:\n" + header);
            return;
        }

        // except "Pragma: no-cache"
    /*
     * String pragma = extractFromHeader(header, "Pragma:", "\n"); if
	 * (pragma != null) if (pragma.toLowerCase().equals("no-cache")) {
	 * System.out.println("Pragma no-cache!"); from_net(header); return; }
	 */
        this.path = path;
        if ((new File(path)).exists()) {
            LOGGER.info("File is exists!");
            this.fromCache = true;
            FileInputStream fis = new FileInputStream(path);
            byte buf[] = new byte[64 * 1024];
            int r = 1;
            System.out.println(this);
            while (r > 0) {
                r = fis.read(buf);
                if (r > 0)
                    os.write(buf, 0, r);
            }
            fis.close();
            os.flush();
            os.close();
        } else {
            LOGGER.info("File is NOT exists!");
            from_net(header);
        }
    }

    // обработка подключения "в потоке"
    // получает HTTP запрос от браузера
    // если запрос начинается с GET пытается взять файл из кэша
    // иначе - грузит из сети
    public void run() {
        LOGGER.info("and start\n");
        int r = 0;
        try {
            is2 = s.getInputStream();
            os = s.getOutputStream();

            byte buf[] = new byte[4 * 1024];
            r = is2.read(buf);
            if (r > 0) {
                String header = new String(buf, 0, r);
                //Хидер GET только из кеша
                if (header.indexOf("GET ", 0) == 0)
                    from_cache(header);
                    //Иные из интернета
                else
                    LOGGER.info("REQUEST main:\n" + header);
                from_net(header);
            }

        } catch (Exception e) {
            try {
                e.printStackTrace();
                printError("exception:\n" + e);
            } catch (Exception ex) {
            }
        } finally {
            try {
                if (s != null) s.close();
                if (is2 != null) is2.close();
                if (os != null) os.close();
                LOGGER.info("\nThread finished!\n");
            } catch (IOException e) {
            }
        }

    }

    // печатает ошибку прокси
    private void printError(String err) throws Exception {
        os.write((new String(PROXY_ERROR + err)).getBytes());
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "CacheThread [method=" + method + ", path=" + path
                + ", fromCache=" + fromCache + ", error=" + error + "]";
    }


    static String getPageCharsetFromHeaders(Map<String, List<String>> headers) {

        String contentType = Arrays.toString(headers.get("Content-Type").toArray()).replaceAll("\\[|\\]", "").replaceAll(", ",
                "\t");
        String[] values = contentType.split(";"); //The values.length must be equal to 2...
        String charset = "";

        for (String value : values) {
            value = value.trim();

            if (value.toLowerCase().startsWith("charset=")) {
                charset = value.substring("charset=".length());
            }
        }

        if ("".equals(charset)) {
            charset = "UTF-8"; //f**k ups...lol, Default encoding accepted!
        }


        System.out.println("\n Accepted encoding: " + charset);
        return charset;
    }


}
