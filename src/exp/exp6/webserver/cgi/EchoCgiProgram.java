package exp.exp6.webserver.cgi;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class EchoCgiProgram {
    public static void main(String[] args) {
        String method = args.length > 0 ? args[0] : "GET";
        String payload = args.length > 1 ? args[1] : "";

        String body = "<html><head><meta charset='UTF-8'><title>CGI Result</title></head><body>"
                + "<h2>CGI executed successfully</h2>"
                + "<p><b>Method:</b> " + escape(method) + "</p>"
                + "<p><b>Payload:</b> " + escape(payload) + "</p>"
                + "<p><b>Server time:</b> " + LocalDateTime.now() + "</p>"
                + "</body></html>";

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        System.out.print("HTTP/1.1 200 OK\r\n");
        System.out.print("Content-Type: text/html; charset=UTF-8\r\n");
        System.out.print("Content-Length: " + bytes.length + "\r\n");
        System.out.print("Connection: close\r\n\r\n");
        System.out.print(body);
    }

    private static String escape(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
