package exp.exp2.streamchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class StreamClient {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final MyStreamSocket socket;
    private final String nickname;
    private final Path saveDir;

    public StreamClient(String host, int port, String nickname, Path saveDir) throws IOException {
        this.socket = new MyStreamSocket(host, port);
        this.nickname = nickname;
        this.saveDir = saveDir;
        Files.createDirectories(saveDir);
    }

    public void start() throws IOException {
        socket.sendMessage("JOIN|" + nickname);

        Thread receiver = new Thread(this::receiveLoop, "stream-receiver");
        receiver.setDaemon(true);
        receiver.start();

        System.out.println("[Stream Client] login as " + nickname);
        System.out.println("Commands: /img <path>, /file <path>, /quit");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if ("/quit".equalsIgnoreCase(line.trim())) {
                    socket.sendMessage("QUIT|" + nickname);
                    break;
                }
                if (line.startsWith("/img ")) {
                    sendBinary("IMG", line.substring(5).trim());
                    continue;
                }
                if (line.startsWith("/file ")) {
                    sendBinary("FILE", line.substring(6).trim());
                    continue;
                }
                socket.sendMessage("TEXT|" + nickname + "|" + line);
            }
        } finally {
            socket.close();
        }
    }

    private void receiveLoop() {
        while (true) {
            try {
                String packet = socket.receiveMessage();
                handlePacket(packet);
            } catch (IOException e) {
                System.out.println("Connection closed.");
                return;
            }
        }
    }

    private void handlePacket(String packet) throws IOException {
        if (packet.startsWith("CHAT|")) {
            String[] parts = packet.split("\\|", 4);
            if (parts.length == 4) {
                System.out.println("[" + parts[1] + "] " + parts[2] + ": " + parts[3]);
            }
            return;
        }

        if (packet.startsWith("FILE|") || packet.startsWith("IMG|")) {
            String[] parts = packet.split("\\|", 5);
            if (parts.length != 5) {
                return;
            }
            String type = parts[0];
            String time = parts[1];
            String sender = parts[2];
            String originalName = parts[3];
            byte[] bytes = Base64.getDecoder().decode(parts[4]);

            String targetName = FILE_TIME.format(LocalDateTime.now()) + "_" + sender + "_" + originalName;
            Path target = saveDir.resolve(targetName);
            Files.write(target, bytes);
            System.out.println("[" + time + "] " + sender + " sent " + type + " -> saved: " + target.toAbsolutePath());
        }
    }

    private void sendBinary(String kind, String pathText) throws IOException {
        Path path = Paths.get(pathText);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            System.out.println("File not found: " + path);
            return;
        }

        byte[] bytes = Files.readAllBytes(path);
        String base64 = Base64.getEncoder().encodeToString(bytes);
        socket.sendMessage(kind + "|" + nickname + "|" + path.getFileName() + "|" + base64);
        System.out.println("Sent " + kind + ": " + path.getFileName());
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage: java exp.exp2.streamchat.StreamClient <host> <port> <nickname> [saveDir]");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String nickname = args[2];
        Path saveDir = args.length >= 4 ? Paths.get(args[3]) : Paths.get("downloads");
        new StreamClient(host, port, nickname, saveDir).start();
    }
}
