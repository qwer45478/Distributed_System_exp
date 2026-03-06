package exp.exp3.echo.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class EchoTcpClient {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java exp.exp3.echo.tcp.EchoTcpClient <serverHost> <serverPort>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket socket = new Socket(host, port);
             BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

            System.out.println("Input text, /quit to exit.");
            while (true) {
                String line = userIn.readLine();
                if (line == null || "/quit".equalsIgnoreCase(line.trim())) {
                    break;
                }
                serverOut.println(line);
                String echoed = serverIn.readLine();
                System.out.println("echo: " + echoed);
            }
        }
    }
}
