package exp.exp3.daytime.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class DaytimeTcpClient {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java exp.exp3.daytime.tcp.DaytimeTcpClient <serverHost> <serverPort>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String serverTime = reader.readLine();
            if (serverTime == null || serverTime.isEmpty()) {
                System.out.println("Remote server time: (empty)");
            } else {
                System.out.println("Remote server time: " + serverTime);
            }
            System.out.println("(本实验仅打印时间，实际更新系统时间通常需管理员权限)");
        }
    }
}
