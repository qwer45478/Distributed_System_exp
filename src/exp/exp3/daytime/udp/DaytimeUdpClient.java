package exp.exp3.daytime.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class DaytimeUdpClient {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java exp.exp3.daytime.udp.DaytimeUdpClient <serverHost> <serverPort>");
            return;
        }

        InetAddress host = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(3000);

            byte[] reqData = "TIME".getBytes(StandardCharsets.UTF_8);
            DatagramPacket reqPacket = new DatagramPacket(reqData, reqData.length, host, port);
            socket.send(reqPacket);

            byte[] respBuffer = new byte[1024];
            DatagramPacket respPacket = new DatagramPacket(respBuffer, respBuffer.length);
            try {
                socket.receive(respPacket);
                String serverTime = new String(respPacket.getData(), respPacket.getOffset(), respPacket.getLength(), StandardCharsets.UTF_8);
                System.out.println("Remote server time: " + serverTime);
                System.out.println("(本实验仅打印时间，实际更新系统时间通常需管理员权限)");
            } catch (SocketTimeoutException e) {
                System.out.println("No response from server (timeout).");
            }
        }
    }
}
