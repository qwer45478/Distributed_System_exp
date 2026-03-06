package exp.exp1.udpchat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * UDP 聊天客户端 GUI 版本：消息列表 + 输入框 + 发送按钮
 * Usage: java exp.exp1.udpchat.UdpChatGUI <serverHost> <serverPort> <nickname> <localPort>
 */
public class UdpChatGUI {
    private DatagramSocket socket;
    private InetAddress serverHost;
    private int serverPort;
    private String nickname;

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9000;
        String nick = args.length > 2 ? args[2] : "User";
        int localPort = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        SwingUtilities.invokeLater(() -> new UdpChatGUI().start(host, port, nick, localPort));
    }

    private void start(String host, int port, String nick, int localPort) {
        this.nickname = nick;
        this.serverPort = port;

        frame = new JFrame("UDP Chat - " + nick);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(520, 480);
        frame.setLayout(new BorderLayout(6, 6));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        inputField = new JTextField();
        inputField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        JButton sendBtn = new JButton("发送");
        sendBtn.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));

        JPanel bottom = new JPanel(new BorderLayout(4, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);

        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);

        Runnable doSend = () -> {
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;
            inputField.setText("");
            try {
                sendRaw("MSG|" + nickname + "|" + text);
            } catch (IOException ex) {
                appendChat("[ERROR] " + ex.getMessage());
            }
        };
        sendBtn.addActionListener(e -> doSend.run());
        inputField.addActionListener(e -> doSend.run());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try { sendRaw("QUIT|" + nickname); } catch (IOException ignored) {}
                if (socket != null && !socket.isClosed()) socket.close();
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // 后台连接
        new Thread(() -> {
            try {
                serverHost = InetAddress.getByName(host);
                socket = new DatagramSocket(localPort);
                socket.setSoTimeout(800);
                sendRaw("JOIN|" + nickname);
                appendChat("[系统] 已连接到 " + host + ":" + port + "，昵称 " + nickname);
                receiveLoop();
            } catch (IOException ex) {
                appendChat("[ERROR] 无法连接: " + ex.getMessage());
            }
        }, "udp-gui-connect").start();
    }

    private void receiveLoop() {
        byte[] buffer = new byte[8192];
        while (!socket.isClosed()) {
            DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(pkt);
                String msg = new String(pkt.getData(), pkt.getOffset(), pkt.getLength(), StandardCharsets.UTF_8);
                String[] parts = msg.split("\\|", 4);
                if (parts.length == 4 && "CHAT".equals(parts[0])) {
                    appendChat("[" + parts[1] + "] " + parts[2] + ": " + parts[3]);
                } else {
                    appendChat(msg);
                }
            } catch (SocketTimeoutException ignored) {
            } catch (IOException e) {
                if (!socket.isClosed()) appendChat("[ERROR] " + e.getMessage());
            }
        }
    }

    private void sendRaw(String payload) throws IOException {
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(data, data.length, serverHost, serverPort));
    }

    private void appendChat(String line) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(line + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
}
