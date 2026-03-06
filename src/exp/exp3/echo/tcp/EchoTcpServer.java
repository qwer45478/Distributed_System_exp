package exp.exp3.echo.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class EchoTcpServer {
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9301;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Echo TCP Server] listening on " + port);
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client), "echo-tcp-" + client.getPort()).start();
            }
        }
    }

    private static void handleClient(Socket client) {
        try (Socket socket = client;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                out.println(line);
            }
        } catch (IOException ignored) {
        }
    }
}
