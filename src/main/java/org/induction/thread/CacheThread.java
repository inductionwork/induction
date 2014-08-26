/**
 *
 */
package org.induction.thread;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
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

public class CacheThread implements Runnable {

    public static final Logger log = Logger.getLogger(CacheThread.class);

    Socket s; // сокет подключения
    InputStream is2; // входящий поток от сокета
    OutputStream os; // исходящий поток от сокета

    String method;
    String path;
    boolean fromCache;
    String error;

    public CacheThread(Socket s) {
        log.info("New Thread created!");
        this.s = s;
        //setDaemon(true);
        //setPriority(NORM_PRIORITY);
    }

    // загружает из сети страничку с одновременным кэшированием её на диск
    // странички в кэше храняться прямо с HTTP заголовком
//    @SuppressWarnings("deprecation")
    protected void from_net(String header, String host, int port, String path) throws Exception {
        log.info("New socked (host, port):" + host + ":" + port + "\n");
        Socket requestSocket = new Socket(host, port);

        log.info("Request Header: \n" + header);
        requestSocket.getOutputStream().write(header.getBytes("ASCII"));

//        log.info("Sended from from_net");
        InputStream responseIs = requestSocket.getInputStream();

//        File f = new File((new File(path)).getParent());
//        if (!f.exists())
//            f.mkdirs();
//        FileOutputStream fos = new FileOutputStream(path);


        try {
            //get header
//            StringBuilder responseHeaderSB = new StringBuilder();
            ArrayList<Byte> arrayList = new ArrayList(2048);
            int buf;
            while ((buf = responseIs.read()) != -1) {
//                responseHeaderSB.append(new String(new byte[]{(byte) buf}, "ASCII"));
//                System.out.printf(new String(new byte[]{(byte)buf}), "ASCII");
                arrayList.add((byte) buf);
                if (13 == buf) {
                    buf = responseIs.read();
                    arrayList.add((byte) buf);
                    if (10 == buf) {
                        buf = responseIs.read();
                        arrayList.add((byte) buf);
                        if (13 == buf) {
                            buf = responseIs.read();
                            arrayList.add((byte) buf);
                            if (10 == buf) {
                                break;
                            }
                        }
                    }
                }


//                if (responseHeaderSB.toString().endsWith("\n\n") || responseHeaderSB.toString().endsWith("\r\n\r\n")) {
//                    break;
//                }
            }

            byte[] resposebHeader = new byte[arrayList.size()];
            for (int i = 0; i < arrayList.size(); i++) {
                resposebHeader[i] = arrayList.get(i);
            }

            String responseHeader = new String(resposebHeader, "ASCII");
            log.info("Response Header\n" + responseHeader);

            os.write(resposebHeader);
            if (responseHeader.startsWith("HTTP/1.0 304 Not Modified") || responseHeader.startsWith("HTTP/1.0 302 Moved Temporarily")) {
                log.info("302 or 304");
                return;
            }
//            HTTP/1.0 304 Not Modified

            //get content Length
            String[] requestHeaders = responseHeader.split("\\n");
            long contentLength = 0;
            for (String requestHeader1 : requestHeaders) {
                if (requestHeader1.startsWith("Content-Length:")) {
                    log.info(requestHeader1);
                    String cl = requestHeader1.substring(requestHeader1.indexOf(":") + 1).trim();
                    log.info(cl);
                    contentLength = Long.parseLong(cl);
                    break;
                }
            }
            log.info("Content Length: " + contentLength);

            if (contentLength != 0) {
                for (long i = 0; i < contentLength; i++) {
                    //fos.write(responseIs.read());
                    os.write(responseIs.read());

                }
            } else {
                byte reply[] = new byte[1024];
                int bytes_read;
                while ((bytes_read = responseIs.read(reply)) != -1) {
                    //fos.write(reply, 0, bytes_read);
                    os.write(reply, 0, bytes_read);

                }
            }


//            while ((bytes_read = is.read(reply)) != 0) {
//                System.out.printf(new String(reply, 0, bytes_read, "ASCII"));
////                fos.write(reply, 0, bytes_read);
//                os.write(reply, 0, bytes_read);
//            }

        } catch (SocketTimeoutException e) {
            log.error(e.getMessage(), e);
//            log.error("SocketTimeoutException: Read timed out in socked (host, port):" + host + ":" + port + "\n");
//            log.error("HEADER:" + header);
        } finally {
//            fos.close();
//            os.close();
            responseIs.close();
            requestSocket.close();
        }

    }

    // вытаскивает из HTTP заголовка хост, порт соединения и путь до файла кэша,
    // после чего вызывает ф-ию загрузки из сети

    protected void from_net(String header) throws Exception {
        log.info("Try from net!");
        String host = extractFromHeader(header, "Host:", "\n"), path = getPath(header);
        if ((host == null) || (path == null)) {
            printError("invalid request:\n" + header);
            return;
        }
        log.info("Transform path: " + path);

        int port = host.indexOf(":", 0);
        if (port < 0)
            port = 80;
        else {
            port = Integer.parseInt(host.substring(port + 1));
            host = host.substring(0, port);
        }
        log.info("From net Host+port: " + host + ":" + port);
        from_net(header, host, port, path);
    }

    // загружает из кэша файл и выдаёт его
    // если во входящем HTTP заголовке стоит "Pragma: no-cache"
    // или такого файла в кэше нет, то вызывается ф-ия загрузки из сети
    protected void from_cache(String header) throws Exception {
        log.info("Try from cache!");
        this.method = "GET";
        String path = getPath(header);
        log.info("Transform path: " + path);
        if (path == null) {
            this.error = "invalid request:\n" + header;
            log.info(this);
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
            log.info("File is exists!");
            this.fromCache = true;
            FileInputStream fis = new FileInputStream(path);
            byte buf[] = new byte[64 * 1024];
            int r = 1;
            log.info(this);
            while (r > 0) {
                r = fis.read(buf);
                if (r > 0)
                    os.write(buf, 0, r);
            }
            fis.close();
            os.flush();
            os.close();
        } else {
            log.info("File is NOT exists!");
            from_net(header);
        }
    }

    // обработка подключения "в потоке"
    // получает HTTP запрос от браузера
    // если запрос начинается с GET пытается взять файл из кэша
    // иначе - грузит из сети
    public void run() {
        log.info("Process request\n");
        int r = 0;
        try {
            is2 = s.getInputStream();
            os = s.getOutputStream();

            byte buf[] = new byte[10 * 1024];
            r = is2.read(buf);
            if (r > 0) {
                String header = new String(buf, 0, r, "ASCII");
                //Хидер GET только из кеша
//                if (header.indexOf("GET ", 0) == 0)
//                    from_cache(header);
//                    //Иные из интернета
//                else
//                    log.info("REQUEST main:\n" + header);
                from_net(header);
            }

        } catch (Exception e) {
            try {
                log.error(e.getMessage(), e);
//                e.printStackTrace();
//                printError("exception:\n" + e);
            } catch (Exception ex) {
            }
        } finally {
            try {
//                if (is2 != null) is2.close();
//                if (os != null) os.close();
                if (s != null) s.close();
                log.info("Thread finished!");
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


        log.info("\n Accepted encoding: " + charset);
        return charset;
    }


}
