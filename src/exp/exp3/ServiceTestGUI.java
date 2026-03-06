package exp.exp3;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Daytime / Echo 协议测试 GUI：上半部分 Daytime，下半部分 Echo，各含 UDP/TCP 两种
 * Usage: java exp.exp3.ServiceTestGUI
 */
public class ServiceTestGUI {
    private JTextArea resultArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServiceTestGUI().show());
    }

    private void show() {
        JFrame frame = new JFrame("Daytime & Echo 测试面板");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(700, 560);
        frame.setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        JTextField hostField = new JTextField("127.0.0.1", 14);
        JTextField daytimeUdpPort = new JTextField("9200", 6);
        JTextField daytimeTcpPort = new JTextField("9201", 6);
        JTextField echoUdpPort = new JTextField("9300", 6);
        JTextField echoTcpPort = new JTextField("9301", 6);
        JTextField echoInput = new JTextField("Hello Distributed System!", 20);

        Font labelFont = new Font("Microsoft YaHei UI", Font.PLAIN, 13);
        Font fieldFont = new Font("Consolas", Font.PLAIN, 13);
        for (JTextField tf : new JTextField[]{hostField, daytimeUdpPort, daytimeTcpPort, echoUdpPort, echoTcpPort, echoInput}) {
            tf.setFont(fieldFont);
        }

        int row = 0;
        addRow(form, c, row++, "服务器地址:", hostField, labelFont);

        // --- Daytime section ---
        c.gridx = 0; c.gridy = row++; c.gridwidth = 4;
        JLabel dtLabel = new JLabel("─── Daytime ───");
        dtLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        form.add(dtLabel, c); c.gridwidth = 1;

        addRow(form, c, row++, "UDP 端口:", daytimeUdpPort, labelFont);
        JButton dtUdpBtn = new JButton("获取时间 (UDP)");
        c.gridx = 2; c.gridy = row - 1; form.add(dtUdpBtn, c);

        addRow(form, c, row++, "TCP 端口:", daytimeTcpPort, labelFont);
        JButton dtTcpBtn = new JButton("获取时间 (TCP)");
        c.gridx = 2; c.gridy = row - 1; form.add(dtTcpBtn, c);

        // --- Echo section ---
        c.gridx = 0; c.gridy = row++; c.gridwidth = 4;
        JLabel ecLabel = new JLabel("─── Echo ───");
        ecLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        form.add(ecLabel, c); c.gridwidth = 1;

        addRow(form, c, row++, "发送内容:", echoInput, labelFont);
        addRow(form, c, row++, "UDP 端口:", echoUdpPort, labelFont);
        JButton ecUdpBtn = new JButton("Echo (UDP)");
        c.gridx = 2; c.gridy = row - 1; form.add(ecUdpBtn, c);

        addRow(form, c, row++, "TCP 端口:", echoTcpPort, labelFont);
        JButton ecTcpBtn = new JButton("Echo (TCP)");
        c.gridx = 2; c.gridy = row - 1; form.add(ecTcpBtn, c);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(resultArea);
        scroll.setBorder(BorderFactory.createTitledBorder("结果"));

        frame.add(form, BorderLayout.NORTH);
        frame.add(scroll, BorderLayout.CENTER);

        // actions
        dtUdpBtn.addActionListener(e -> asyncRun(() -> daytimeUdp(hostField.getText(), intVal(daytimeUdpPort))));
        dtTcpBtn.addActionListener(e -> asyncRun(() -> daytimeTcp(hostField.getText(), intVal(daytimeTcpPort))));
        ecUdpBtn.addActionListener(e -> asyncRun(() -> echoUdp(hostField.getText(), intVal(echoUdpPort), echoInput.getText())));
        ecTcpBtn.addActionListener(e -> asyncRun(() -> echoTcp(hostField.getText(), intVal(echoTcpPort), echoInput.getText())));

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, JComponent comp, Font font) {
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        JLabel l = new JLabel(label); l.setFont(font);
        panel.add(l, c);
        c.gridx = 1; c.weightx = 1;
        panel.add(comp, c);
        c.weightx = 0;
    }

    private int intVal(JTextField f) { return Integer.parseInt(f.getText().trim()); }

    private void asyncRun(Runnable task) {
        new Thread(task, "service-test").start();
    }

    // ---- Daytime UDP ----
    private void daytimeUdp(String host, int port) {
        log(">>> Daytime UDP → " + host + ":" + port);
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(3000);
            byte[] req = "TIME".getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(req, req.length, InetAddress.getByName(host), port));
            byte[] buf = new byte[1024];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            socket.receive(resp);
            String time = new String(resp.getData(), resp.getOffset(), resp.getLength(), StandardCharsets.UTF_8);
            log("远程时间(UDP): " + time);
        } catch (Exception e) { log("[ERROR] " + e.getMessage()); }
    }

    // ---- Daytime TCP ----
    private void daytimeTcp(String host, int port) {
        log(">>> Daytime TCP → " + host + ":" + port);
        try (Socket s = new Socket(host, port);
             BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {
            String time = r.readLine();
            log("远程时间(TCP): " + time);
        } catch (Exception e) { log("[ERROR] " + e.getMessage()); }
    }

    // ---- Echo UDP ----
    private void echoUdp(String host, int port, String text) {
        log(">>> Echo UDP → " + host + ":" + port + "  发送: " + text);
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(3000);
            byte[] req = text.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(req, req.length, InetAddress.getByName(host), port));
            byte[] buf = new byte[4096];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            socket.receive(resp);
            String echo = new String(resp.getData(), resp.getOffset(), resp.getLength(), StandardCharsets.UTF_8);
            log("回显(UDP): " + echo);
        } catch (Exception e) { log("[ERROR] " + e.getMessage()); }
    }

    // ---- Echo TCP ----
    private void echoTcp(String host, int port, String text) {
        log(">>> Echo TCP → " + host + ":" + port + "  发送: " + text);
        try (Socket s = new Socket(host, port);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {
            out.println(text);
            String echo = in.readLine();
            log("回显(TCP): " + echo);
        } catch (Exception e) { log("[ERROR] " + e.getMessage()); }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            resultArea.append(msg + "\n");
            resultArea.setCaretPosition(resultArea.getDocument().getLength());
        });
    }
}
