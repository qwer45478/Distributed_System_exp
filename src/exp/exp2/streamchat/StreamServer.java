package exp.exp2.streamchat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StreamServer {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final int port;
    private final Map<ClientHandler, String> clients = new ConcurrentHashMap<>();

    public StreamServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[TCP Stream Server] listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                MyStreamSocket streamSocket = new MyStreamSocket(socket);
                ClientHandler handler = new ClientHandler(this, streamSocket);
                new Thread(handler, "stream-client-" + socket.getPort()).start();
            }
        }
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }

    public void register(ClientHandler handler, String nickname) {
        clients.put(handler, nickname);
        broadcast("CHAT|" + now() + "|SYSTEM|" + nickname + " joined");
    }

    public void unregister(ClientHandler handler) {
        String nickname = clients.remove(handler);
        if (nickname != null) {
            broadcast("CHAT|" + now() + "|SYSTEM|" + nickname + " left");
        }
    }

    public void broadcast(String packet) {
        for (ClientHandler handler : clients.keySet()) {
            handler.send(packet);
        }
    }

    public void relayText(String nickname, String content) {
        broadcast("CHAT|" + now() + "|" + nickname + "|" + content);
    }

    public void relayBinary(String kind, String nickname, String fileName, String base64) {
        broadcast(kind + "|" + now() + "|" + nickname + "|" + fileName + "|" + base64);
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9100;
        new StreamServer(port).start();
    }

    private static class ClientHandler implements Runnable {
        private final StreamServer server;
        private final MyStreamSocket socket;
        private String nickname = "unknown";

        private ClientHandler(StreamServer server, MyStreamSocket socket) {
            this.server = server;
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String message = socket.receiveMessage();
                    String[] parts = message.split("\\|", 4);
                    if (parts.length < 2) {
                        continue;
                    }
                    String type = parts[0];
                    if ("JOIN".equals(type)) {
                        nickname = parts[1];
                        server.register(this, nickname);
                    } else if ("TEXT".equals(type) && parts.length == 3) {
                        server.relayText(parts[1], parts[2]);
                    } else if ("FILE".equals(type) || "IMG".equals(type)) {
                        String[] binaryParts = message.split("\\|", 4);
                        if (binaryParts.length == 4) {
                            server.relayBinary(binaryParts[0], binaryParts[1], binaryParts[2], binaryParts[3]);
                        }
                    } else if ("QUIT".equals(type)) {
                        break;
                    }
                }
            } catch (IOException ignored) {
            } finally {
                server.unregister(this);
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }

        private void send(String packet) {
            try {
                socket.sendMessage(packet);
            } catch (IOException ignored) {
            }
        }
    }
}
