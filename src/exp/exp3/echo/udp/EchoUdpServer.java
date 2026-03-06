package exp.exp3.echo.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class EchoUdpServer {
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9300;
        byte[] buffer = new byte[4096];
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("[Echo UDP Server] listening on " + port);
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                DatagramPacket response = new DatagramPacket(
                        request.getData(),
                        request.getLength(),
                        request.getAddress(),
                        request.getPort()
                );
                socket.send(response);
            }
        }
    }
}
