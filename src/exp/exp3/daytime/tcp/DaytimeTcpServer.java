package exp.exp3.daytime.tcp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DaytimeTcpServer {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9201;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Daytime TCP Server] listening on " + port);
            while (true) {
                try (Socket client = serverSocket.accept()) {
                    String time = LocalDateTime.now().format(TIME_FORMAT);
                    OutputStream out = client.getOutputStream();
                    out.write(time.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
            }
        }
    }
}
