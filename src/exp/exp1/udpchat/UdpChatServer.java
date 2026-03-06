package exp.exp1.udpchat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UdpChatServer {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DatagramSocket socket;
    private final Map<InetSocketAddress, String> clients = new ConcurrentHashMap<>();

    public UdpChatServer(int port) throws IOException {
        this.socket = new DatagramSocket(port);
        this.socket.setSoTimeout(1000);
    }

    public void start() throws IOException {
        System.out.println("[UDP Chat Server] listening on port " + socket.getLocalPort());
        byte[] buffer = new byte[8192];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
            } catch (SocketTimeoutException timeoutException) {
                continue;
            }

            InetSocketAddress sender = new InetSocketAddress(packet.getAddress(), packet.getPort());
            String message = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
            handleMessage(sender, message);
        }
    }

    private void handleMessage(InetSocketAddress sender, String rawMessage) throws IOException {
        String[] parts = rawMessage.split("\\|", 3);
        if (parts.length < 2) {
            return;
        }

        String type = parts[0];
        String nickname = parts[1];

        if ("JOIN".equalsIgnoreCase(type)) {
            clients.put(sender, nickname);
            broadcast(systemMessage(nickname + " joined from " + sender));
            return;
        }

        if ("MSG".equalsIgnoreCase(type) && parts.length == 3) {
            clients.putIfAbsent(sender, nickname);
            String content = parts[2];
            String fullMessage = "CHAT|" + now() + "|" + nickname + "|" + content;
            broadcast(fullMessage);
            return;
        }

        if ("QUIT".equalsIgnoreCase(type)) {
            String removed = clients.remove(sender);
            if (removed != null) {
                broadcast(systemMessage(removed + " left chat"));
            }
        }
    }

    private String systemMessage(String content) {
        return "CHAT|" + now() + "|SYSTEM|" + content;
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }

    private void broadcast(String payload) throws IOException {
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        for (InetSocketAddress client : clients.keySet()) {
            InetAddress address = client.getAddress();
            int port = client.getPort();
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9000;
        new UdpChatServer(port).start();
    }
}
