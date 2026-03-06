package exp.exp5.httpclient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class SimpleHttpSocketClient {
    public String sendGet(String url) throws IOException {
        URI uri = URI.create(url);
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 80 : uri.getPort();
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
            path += "?" + uri.getRawQuery();
        }

        String request = "GET " + path + " HTTP/1.1\r\n" +
                "Host: " + host + "\r\n" +
                "Connection: close\r\n" +
                "User-Agent: DSExp-SocketClient/1.0\r\n" +
                "Accept: text/html, text/plain, */*\r\n\r\n";

        return sendRaw(host, port, request);
    }

    public String sendPost(String url, String formBody) throws IOException {
        URI uri = URI.create(url);
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 80 : uri.getPort();
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        byte[] body = formBody.getBytes(StandardCharsets.UTF_8);

        String request = "POST " + path + " HTTP/1.1\r\n" +
                "Host: " + host + "\r\n" +
                "Connection: close\r\n" +
                "User-Agent: DSExp-SocketClient/1.0\r\n" +
                "Content-Type: application/x-www-form-urlencoded; charset=UTF-8\r\n" +
                "Content-Length: " + body.length + "\r\n\r\n" +
                formBody;

        return sendRaw(host, port, request);
    }

    private String sendRaw(String host, int port, String request) throws IOException {
        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int len;
            while ((len = in.read(data)) != -1) {
                buffer.write(data, 0, len);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        }
    }
}
