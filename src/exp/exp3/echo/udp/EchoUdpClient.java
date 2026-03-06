package exp.exp3.echo.udp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class EchoUdpClient {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java exp.exp3.echo.udp.EchoUdpClient <serverHost> <serverPort>");
            return;
        }

        InetAddress host = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);

        try (DatagramSocket socket = new DatagramSocket();
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            socket.setSoTimeout(3000);
            System.out.println("Input text, /quit to exit.");
            while (true) {
                String line = reader.readLine();
                if (line == null || "/quit".equalsIgnoreCase(line.trim())) {
                    break;
                }
                byte[] sendData = line.getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(sendData, sendData.length, host, port));

                byte[] recvBuffer = new byte[4096];
                DatagramPacket response = new DatagramPacket(recvBuffer, recvBuffer.length);
                try {
                    socket.receive(response);
                    String echoed = new String(response.getData(), response.getOffset(), response.getLength(), StandardCharsets.UTF_8);
                    System.out.println("echo: " + echoed);
                } catch (SocketTimeoutException e) {
                    System.out.println("No response from server (timeout)");
                }
            }
        }
    }
}
