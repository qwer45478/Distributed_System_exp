package exp.exp6.webserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SimpleWebServer {
    private final int port;
    private final Path webRoot;

    public SimpleWebServer(int port, Path webRoot) {
        this.port = port;
        this.webRoot = webRoot;
    }

    public void start() throws IOException {
        Files.createDirectories(webRoot);
        ensureDefaultIndex();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SimpleWebServer] listening at http://127.0.0.1:" + port);
            System.out.println("Web root: " + webRoot.toAbsolutePath());
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handle(socket), "web-client-" + socket.getPort()).start();
            }
        }
    }

    private void handle(Socket socket) {
        try (Socket client = socket;
             InputStream in = client.getInputStream();
             OutputStream out = client.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(out, false, StandardCharsets.UTF_8)) {

            HttpRequest req = HttpRequest.readFrom(reader);
            if (req == null) {
                return;
            }

            if (req.path.startsWith("/cgi/")) {
                processCgi(req, out, writer);
                return;
            }

            if (!"GET".equals(req.method)) {
                writeText(writer, "405 Method Not Allowed", "Only GET is allowed for static files.");
                return;
            }

            serveStatic(req.path, out, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processCgi(HttpRequest req, OutputStream out, PrintWriter writer) throws IOException {
        if (!"GET".equals(req.method) && !"POST".equals(req.method)) {
            writeText(writer, "405 Method Not Allowed", "CGI supports GET/POST.");
            return;
        }

        String payload = "GET".equals(req.method) ? req.query : req.body;
        if (payload == null) {
            payload = "";
        }
        payload = URLDecoder.decode(payload, StandardCharsets.UTF_8.name());

        String javaExec = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");

        Process process = new ProcessBuilder(
                javaExec,
                "-cp",
                classpath,
                "exp.exp6.webserver.cgi.EchoCgiProgram",
                req.method,
                payload
        ).start();

        byte[] cgiOutput = process.getInputStream().readAllBytes();
        byte[] cgiErr = process.getErrorStream().readAllBytes();

        try {
            int code = process.waitFor();
            if (code != 0) {
                writeText(writer, "500 Internal Server Error", "CGI failed: " + new String(cgiErr, StandardCharsets.UTF_8));
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeText(writer, "500 Internal Server Error", "CGI interrupted");
            return;
        }

        out.write(cgiOutput);
        out.flush();
    }

    private void serveStatic(String rawPath, OutputStream out, PrintWriter writer) throws IOException {
        String path = rawPath;
        int queryAt = path.indexOf('?');
        if (queryAt >= 0) {
            path = path.substring(0, queryAt);
        }

        if ("/".equals(path)) {
            path = "/index.html";
        }

        Path filePath = webRoot.resolve(path.substring(1)).normalize();
        if (!filePath.startsWith(webRoot)) {
            writeText(writer, "403 Forbidden", "Invalid path.");
            return;
        }

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            writeText(writer, "404 Not Found", "File not found: " + path);
            return;
        }

        String mime = contentType(filePath);
        byte[] content = Files.readAllBytes(filePath);

        writer.print("HTTP/1.1 200 OK\r\n");
        writer.print("Server: DS-SimpleWebServer/1.0\r\n");
        writer.print("Content-Type: " + mime + "\r\n");
        writer.print("Content-Length: " + content.length + "\r\n");
        writer.print("Connection: close\r\n\r\n");
        writer.flush();

        out.write(content);
        out.flush();
    }

    private String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (name.endsWith(".css")) return "text/css; charset=UTF-8";
        if (name.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (name.endsWith(".txt")) return "text/plain; charset=UTF-8";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }

    private void writeText(PrintWriter writer, String status, String body) {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        writer.print("HTTP/1.1 " + status + "\r\n");
        writer.print("Server: DS-SimpleWebServer/1.0\r\n");
        writer.print("Content-Type: text/plain; charset=UTF-8\r\n");
        writer.print("Content-Length: " + bodyBytes.length + "\r\n");
        writer.print("Connection: close\r\n\r\n");
        writer.print(body);
        writer.flush();
    }

    private void ensureDefaultIndex() throws IOException {
        Path index = webRoot.resolve("index.html");
        if (Files.exists(index)) {
            return;
        }
        String html = "<!doctype html><html><head><meta charset='UTF-8'><title>Simple Web Server</title>"
                + "<style>body{font-family:Segoe UI,Arial;margin:40px;}code{background:#f3f3f3;padding:2px 6px;border-radius:4px;}"
                + "a{display:block;margin:8px 0;}</style></head><body>"
                + "<h1>Experiment 6 - Simple Web Server</h1>"
                + "<a href='/cgi/echo?name=alice&score=95'>CGI GET Demo</a>"
                + "<p>Use POST to <code>/cgi/echo</code> with body like <code>name=bob&score=100</code>.</p>"
                + "</body></html>";
        Files.writeString(index, html, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path root = args.length > 1 ? Paths.get(args[1]) : Paths.get("wwwroot");
        new SimpleWebServer(port, root).start();
    }

    private static class HttpRequest {
        private String method;
        private String path;
        private String query;
        private String body;
        private Map<String, String> headers;

        static HttpRequest readFrom(BufferedReader reader) throws IOException {
            String line = reader.readLine();
            if (line == null || line.isEmpty()) {
                return null;
            }

            String[] start = line.split(" ");
            if (start.length < 2) {
                return null;
            }

            HttpRequest req = new HttpRequest();
            req.method = start[0].trim();
            String fullPath = start[1].trim();
            int q = fullPath.indexOf('?');
            if (q >= 0) {
                req.path = fullPath.substring(0, q);
                req.query = fullPath.substring(q + 1);
            } else {
                req.path = fullPath;
                req.query = "";
            }

            req.headers = new HashMap<>();
            while (true) {
                String headerLine = reader.readLine();
                if (headerLine == null || headerLine.isEmpty()) {
                    break;
                }
                int colon = headerLine.indexOf(':');
                if (colon > 0) {
                    String key = headerLine.substring(0, colon).trim().toLowerCase();
                    String value = headerLine.substring(colon + 1).trim();
                    req.headers.put(key, value);
                }
            }

            int contentLength = 0;
            String contentLengthStr = req.headers.get("content-length");
            if (contentLengthStr != null) {
                try {
                    contentLength = Integer.parseInt(contentLengthStr);
                } catch (NumberFormatException ignored) {
                }
            }

            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int n = reader.read(bodyChars, read, contentLength - read);
                    if (n == -1) {
                        break;
                    }
                    read += n;
                }
                req.body = new String(bodyChars, 0, read);
            } else {
                req.body = "";
            }
            return req;
        }
    }
}
