package exp.exp2.streamchat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * TCP 流式聊天客户端 GUI 版本：消息列表 + 输入框 + 发送图片/文件按钮
 * Usage: java exp.exp2.streamchat.StreamChatGUI <host> <port> <nickname>
 */
public class StreamChatGUI {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private MyStreamSocket socket;
    private String nickname;
    private Path saveDir;

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9100;
        String nick = args.length > 2 ? args[2] : "User";
        SwingUtilities.invokeLater(() -> new StreamChatGUI().start(host, port, nick));
    }

    private void start(String host, int port, String nick) {
        this.nickname = nick;
        this.saveDir = Paths.get("downloads");

        frame = new JFrame("TCP Chat - " + nick);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(580, 520);
        frame.setLayout(new BorderLayout(6, 6));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        inputField = new JTextField();
        inputField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));

        JButton sendBtn = new JButton("发送");
        JButton imgBtn = new JButton("图片");
        JButton fileBtn = new JButton("文件");
        for (JButton b : new JButton[]{sendBtn, imgBtn, fileBtn}) {
            b.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        }

        JPanel bottom = new JPanel(new BorderLayout(4, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));
        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 4, 0));
        btnPanel.add(sendBtn);
        btnPanel.add(imgBtn);
        btnPanel.add(fileBtn);
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(btnPanel, BorderLayout.EAST);

        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);

        Runnable doSend = () -> {
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;
            inputField.setText("");
            sendAsync("TEXT|" + nickname + "|" + text);
        };
        sendBtn.addActionListener(e -> doSend.run());
        inputField.addActionListener(e -> doSend.run());
        imgBtn.addActionListener(e -> chooseBinary("IMG"));
        fileBtn.addActionListener(e -> chooseBinary("FILE"));

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sendAsync("QUIT|" + nickname);
                closeSocket();
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new Thread(() -> connect(host, port), "tcp-gui-connect").start();
    }

    private void connect(String host, int port) {
        try {
            Files.createDirectories(saveDir);
            socket = new MyStreamSocket(host, port);
            socket.sendMessage("JOIN|" + nickname);
            appendChat("[系统] 已连接到 " + host + ":" + port + "，昵称 " + nickname);
            receiveLoop();
        } catch (IOException ex) {
            appendChat("[ERROR] 无法连接: " + ex.getMessage());
        }
    }

    private void receiveLoop() {
        while (true) {
            try {
                String packet = socket.receiveMessage();
                handlePacket(packet);
            } catch (IOException e) {
                appendChat("[系统] 连接已断开");
                SwingUtilities.invokeLater(() -> frame.dispose());
                return;
            }
        }
    }

    private void handlePacket(String packet) {
        if (packet.startsWith("CHAT|")) {
            String[] parts = packet.split("\\|", 4);
            if (parts.length == 4) {
                appendChat("[" + parts[1] + "] " + parts[2] + ": " + parts[3]);
            }
            return;
        }
        if (packet.startsWith("FILE|") || packet.startsWith("IMG|")) {
            String[] parts = packet.split("\\|", 5);
            if (parts.length != 5) return;
            String type = parts[0], time = parts[1], sender = parts[2], originalName = parts[3];
            byte[] bytes = Base64.getDecoder().decode(parts[4]);
            String targetName = FILE_TIME.format(LocalDateTime.now()) + "_" + sender + "_" + originalName;
            Path target = saveDir.resolve(targetName);
            try {
                Files.write(target, bytes);
                appendChat("[" + time + "] " + sender + " 发送了 " + type + " -> 已保存: " + target.toAbsolutePath());
                if ("IMG".equals(type)) {
                    showImage(target);
                }
            } catch (IOException e) {
                appendChat("[ERROR] 保存文件失败: " + e.getMessage());
            }
        }
    }

    private void showImage(Path imagePath) {
        SwingUtilities.invokeLater(() -> {
            ImageIcon icon = new ImageIcon(imagePath.toString());
            Image img = icon.getImage();
            int w = Math.min(icon.getIconWidth(), 400);
            int h = (int) ((double) w / icon.getIconWidth() * icon.getIconHeight());
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            JOptionPane.showMessageDialog(frame, new JLabel(new ImageIcon(scaled)),
                    "收到图片", JOptionPane.PLAIN_MESSAGE);
        });
    }

    private void chooseBinary(String kind) {
        if (socket == null) {
            appendChat("[ERROR] 尚未连接到服务器");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("选择" + ("IMG".equals(kind) ? "图片" : "文件"));
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        Path path = fc.getSelectedFile().toPath();

        new Thread(() -> {
            try {
                byte[] bytes = Files.readAllBytes(path);
                String base64 = Base64.getEncoder().encodeToString(bytes);
                socket.sendMessage(kind + "|" + nickname + "|" + path.getFileName() + "|" + base64);
                appendChat("[本地] 已发送 " + kind + ": " + path.getFileName());
            } catch (IOException ex) {
                appendChat("[ERROR] 发送失败: " + ex.getMessage());
            }
        }, "send-binary").start();
    }

    private void sendAsync(String msg) {
        if (socket == null) {
            appendChat("[ERROR] 尚未连接到服务器");
            return;
        }
        new Thread(() -> {
            try {
                socket.sendMessage(msg);
            } catch (IOException ex) {
                appendChat("[ERROR] 发送失败: " + ex.getMessage());
            }
        }, "send-msg").start();
    }

    private void closeSocket() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private void appendChat(String line) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(line + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
}
