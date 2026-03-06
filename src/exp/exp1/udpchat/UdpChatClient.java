package exp.exp1.udpchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class UdpChatClient {
    private final DatagramSocket socket;
    private final InetAddress serverHost;
    private final int serverPort;
    private final String nickname;

    public UdpChatClient(String host, int serverPort, String nickname, int localPort) throws IOException {
        this.socket = new DatagramSocket(localPort);
        this.socket.setSoTimeout(1000);
        this.serverHost = InetAddress.getByName(host);
        this.serverPort = serverPort;
        this.nickname = nickname;
    }

    public void start() throws IOException {
        sendRaw("JOIN|" + nickname);

        Thread receiver = new Thread(this::receiveLoop, "udp-receiver");
        receiver.setDaemon(true);
        receiver.start();

        System.out.println("[UDP Chat Client] connected as " + nickname + ". Type /quit to exit.");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if ("/quit".equalsIgnoreCase(line.trim())) {
                    sendRaw("QUIT|" + nickname);
                    break;
                }
                sendRaw("MSG|" + nickname + "|" + line);
            }
        } finally {
            socket.close();
        }
    }

    private void receiveLoop() {
        byte[] buffer = new byte[8192];
        while (!socket.isClosed()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                String message = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                printServerMessage(message);
            } catch (SocketTimeoutException ignored) {
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.err.println("Receive error: " + e.getMessage());
                }
            }
        }
    }

    private void printServerMessage(String message) {
        String[] parts = message.split("\\|", 4);
        if (parts.length == 4 && "CHAT".equals(parts[0])) {
            System.out.println("[" + parts[1] + "] " + parts[2] + ": " + parts[3]);
        } else {
            System.out.println(message);
        }
    }

    private void sendRaw(String payload) throws IOException {
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, serverHost, serverPort);
        socket.send(packet);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println("Usage: java exp.exp1.udpchat.UdpChatClient <serverHost> <serverPort> <nickname> <localPort>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String nickname = args[2];
        int localPort = Integer.parseInt(args[3]);
        new UdpChatClient(host, port, nickname, localPort).start();
    }
}
