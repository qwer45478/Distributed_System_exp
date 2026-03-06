package exp.exp3.daytime.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DaytimeUdpServer {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9200;
        byte[] buffer = new byte[1024];
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("[Daytime UDP Server] listening on " + port);
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                String now = LocalDateTime.now().format(TIME_FORMAT);
                byte[] data = now.getBytes(StandardCharsets.UTF_8);
                DatagramPacket response = new DatagramPacket(
                        data,
                        data.length,
                        request.getAddress(),
                        request.getPort()
                );
                socket.send(response);
            }
        }
    }
}
