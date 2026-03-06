package exp.gui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExperimentDashboard {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Map<String, ExperimentView> data = new LinkedHashMap<>();
    private final List<Process> running = new ArrayList<>();

    private JTextArea introArea;
    private JTextArea commandArea;
    private JTextArea logArea;
    private JPanel actionPanel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ExperimentDashboard().show());
    }

    private void show() {
        buildData();

        JFrame frame = new JFrame("Distributed System Experiments Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopAllProcesses();
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel(), rightPanel());
        splitPane.setResizeWeight(0.23);

        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(bottomPanel(), BorderLayout.SOUTH);

        frame.setSize(new Dimension(1280, 840));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel leftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Experiments"));

        DefaultListModel<String> model = new DefaultListModel<>();
        for (String name : data.keySet()) {
            model.addElement(name);
        }

        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = list.getSelectedValue();
                if (selected != null) {
                    renderExperiment(selected);
                }
            }
        });

        panel.add(new JScrollPane(list), BorderLayout.CENTER);

        if (model.getSize() > 0) {
            SwingUtilities.invokeLater(() -> list.setSelectedIndex(0));
        }

        return panel;
    }

    private JPanel rightPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 8));

        introArea = new JTextArea();
        introArea.setEditable(false);
        introArea.setLineWrap(true);
        introArea.setWrapStyleWord(true);
        introArea.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));

        commandArea = new JTextArea();
        commandArea.setEditable(false);
        commandArea.setFont(new Font("Consolas", Font.PLAIN, 13));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));

        JPanel textPanel = new JPanel(new GridLayout(3, 1, 8, 8));

        JScrollPane introScroll = new JScrollPane(introArea);
        introScroll.setBorder(BorderFactory.createTitledBorder("Overview"));
        JScrollPane commandScroll = new JScrollPane(commandArea);
        commandScroll.setBorder(BorderFactory.createTitledBorder("Commands"));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Live Logs"));

        textPanel.add(introScroll);
        textPanel.add(commandScroll);
        textPanel.add(logScroll);

        actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setBorder(BorderFactory.createTitledBorder("Actions"));

        panel.add(textPanel, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel bottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        JLabel hint = new JLabel("提示: 启动服务端后，再启动对应客户端；关闭窗口会自动终止已启动子进程。", JLabel.LEFT);
        JButton stopAll = new JButton("Stop All Processes");
        stopAll.addActionListener(e -> stopAllProcesses());

        panel.add(hint, BorderLayout.CENTER);
        panel.add(stopAll, BorderLayout.EAST);
        return panel;
    }

    private void renderExperiment(String name) {
        ExperimentView view = data.get(name);
        if (view == null) {
            return;
        }

        introArea.setText(view.description);
        commandArea.setText(String.join("\n", view.commandDescriptions));

        actionPanel.removeAll();
        for (CommandAction action : view.actions) {
            String label = action.label;
            JButton button;
            if (label.startsWith("BROWSE|")) {
                String url = label.substring(7);
                button = new JButton("打开浏览器: " + url);
                button.setMaximumSize(new Dimension(400, 34));
                button.addActionListener(e -> openBrowser(url));
            } else {
                button = new JButton(label);
                button.setMaximumSize(new Dimension(400, 34));
                button.addActionListener(e -> runAction(action));
            }
            actionPanel.add(button);
            actionPanel.add(new JLabel(" "));
        }
        actionPanel.revalidate();
        actionPanel.repaint();
    }

    private void runAction(CommandAction action) {
        appendLog("[" + now() + "] starting: " + action.label);
        appendLog("[" + now() + "] command: " + String.join(" ", action.command));

        try {
            ProcessBuilder builder = new ProcessBuilder(action.command);
            builder.directory(new File(System.getProperty("user.dir")));
            builder.redirectErrorStream(true);
            Process process = builder.start();
            synchronized (running) {
                running.add(process);
            }

            Thread t = new Thread(() -> readProcessOutput(action.label, process), "proc-" + action.label);
            t.setDaemon(true);
            t.start();
        } catch (Exception ex) {
            appendLog("[" + now() + "] failed: " + ex.getMessage());
        }
    }

    private void readProcessOutput(String label, Process process) {
        try (InputStream in = process.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                appendLog("[" + now() + "] [" + label + "] " + line);
            }
            int code = process.waitFor();
            appendLog("[" + now() + "] [" + label + "] exited with code " + code);
        } catch (Exception e) {
            appendLog("[" + now() + "] [" + label + "] output error: " + e.getMessage());
        } finally {
            synchronized (running) {
                running.remove(process);
            }
        }
    }

    private void openBrowser(String url) {
        appendLog("[" + now() + "] opening browser: " + url);
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            appendLog("[" + now() + "] failed to open browser: " + ex.getMessage());
        }
    }

    private void stopAllProcesses() {
        synchronized (running) {
            for (Process process : running) {
                process.destroy();
            }
            running.clear();
        }
        appendLog("[" + now() + "] all processes stopped");
    }

    private void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }

    private void buildData() {
        data.put("实验一 UDP 聊天", new ExperimentView(
                "目标:\n1) 理解 UDP 数据报通信\n2) 完成多人聊天\n3) 通过接收线程+发送线程实现全双工\n\n可视化重点:\n- 发送者昵称/IP\n- 发送时间\n- 消息内容\n\n操作：先启动服务器，再点击客户端按钮打开聊天窗口",
                list(
                        "Server: java -cp out exp.exp1.udpchat.UdpChatServer 9000",
                        "Client(GUI): java -cp out exp.exp1.udpchat.UdpChatGUI 127.0.0.1 9000 Alice 10001"
                ),
                listActions(
                        new CommandAction("启动 UDP 聊天服务器", command("java", "-cp", "out", "exp.exp1.udpchat.UdpChatServer", "9000")),
                        new CommandAction("打开聊天窗口 Alice", command("java", "-cp", "out", "exp.exp1.udpchat.UdpChatGUI", "127.0.0.1", "9000", "Alice", "10001")),
                        new CommandAction("打开聊天窗口 Bob", command("java", "-cp", "out", "exp.exp1.udpchat.UdpChatGUI", "127.0.0.1", "9000", "Bob", "10002"))
                )
        ));

        data.put("实验二 TCP 流式聊天", new ExperimentView(
                "目标:\n1) 使用 MyStreamSocket 封装 sendMessage/receiveMessage\n2) 支持文本消息\n3) 支持图片和文件传输并落盘\n\n操作：先启动服务器，再打开聊天窗口（含发送图片/文件按钮）",
                list(
                        "Server: java -cp out exp.exp2.streamchat.StreamServer 9100",
                        "Client(GUI): java -cp out exp.exp2.streamchat.StreamChatGUI 127.0.0.1 9100 Alice"
                ),
                listActions(
                        new CommandAction("启动 TCP 聊天服务器", command("java", "-cp", "out", "exp.exp2.streamchat.StreamServer", "9100")),
                        new CommandAction("打开聊天窗口 Alice", command("java", "-cp", "out", "exp.exp2.streamchat.StreamChatGUI", "127.0.0.1", "9100", "Alice")),
                        new CommandAction("打开聊天窗口 Bob", command("java", "-cp", "out", "exp.exp2.streamchat.StreamChatGUI", "127.0.0.1", "9100", "Bob"))
                )
        ));

        data.put("实验三 Daytime/Echo", new ExperimentView(
                "目标:\n1) 实现 Daytime 协议 (UDP/TCP)\n2) 实现 Echo 协议 (UDP/TCP)\n\n操作：先启动4个服务端，再打开测试面板一键测试",
                list(
                        "UDP Daytime Server: java -cp out exp.exp3.daytime.udp.DaytimeUdpServer 9200",
                        "TCP Daytime Server: java -cp out exp.exp3.daytime.tcp.DaytimeTcpServer 9201",
                        "UDP Echo Server: java -cp out exp.exp3.echo.udp.EchoUdpServer 9300",
                        "TCP Echo Server: java -cp out exp.exp3.echo.tcp.EchoTcpServer 9301",
                        "Test GUI: java -cp out exp.exp3.ServiceTestGUI"
                ),
                listActions(
                        new CommandAction("启动 UDP Daytime 服务端", command("java", "-cp", "out", "exp.exp3.daytime.udp.DaytimeUdpServer", "9200")),
                        new CommandAction("启动 TCP Daytime 服务端", command("java", "-cp", "out", "exp.exp3.daytime.tcp.DaytimeTcpServer", "9201")),
                        new CommandAction("启动 UDP Echo 服务端", command("java", "-cp", "out", "exp.exp3.echo.udp.EchoUdpServer", "9300")),
                        new CommandAction("启动 TCP Echo 服务端", command("java", "-cp", "out", "exp.exp3.echo.tcp.EchoTcpServer", "9301")),
                        new CommandAction("打开测试面板", command("java", "-cp", "out", "exp.exp3.ServiceTestGUI"))
                )
        ));

        data.put("实验四 RMI API", new ExperimentView(
                "目标:\n1) 理解 RMI 架构和 Registry\n2) 实现学生成绩 CRUD\n3) 支持多客户端并发访问\n\n操作：先启动服务端，再打开成绩管理界面进行增删改查",
                list(
                        "Server: java -cp out exp.exp4.rmi.server.RmiStudentServer 1099 data/student_scores.csv",
                        "Client(GUI): java -cp out exp.exp4.rmi.client.RmiStudentGUI 127.0.0.1 1099"
                ),
                listActions(
                        new CommandAction("启动 RMI 服务端", command("java", "-cp", "out", "exp.exp4.rmi.server.RmiStudentServer", "1099", "data/student_scores.csv")),
                        new CommandAction("打开成绩管理界面", command("java", "-cp", "out", "exp.exp4.rmi.client.RmiStudentGUI", "127.0.0.1", "1099"))
                )
        ));

        data.put("实验五 HTTP 客户端", new ExperimentView(
                "目标:\n1) 用 Socket 手写 HTTP 请求\n2) 支持 GET/POST\n3) 在界面中展示响应头和响应体",
                list(
                        "UI: java -cp out exp.exp5.httpclient.HttpSocketBrowserUI"
                ),
                listActions(
                        new CommandAction("启动 HTTP 浏览器 GUI", command("java", "-cp", "out", "exp.exp5.httpclient.HttpSocketBrowserUI"))
                )
        ));

        data.put("实验六 Web 服务器", new ExperimentView(
                "目标:\n1) 实现基本 HTTP 服务器\n2) 响应静态页面 GET\n3) 响应 CGI GET/POST\n\n操作：启动服务器后点击\"打开浏览器\"即可访问",
                list(
                        "Server: java -cp out exp.exp6.webserver.SimpleWebServer 8080 wwwroot",
                        "Visit: http://127.0.0.1:8080/",
                        "CGI GET: http://127.0.0.1:8080/cgi/echo?name=alice&score=95"
                ),
                listActions(
                        new CommandAction("启动 Web 服务器", command("java", "-cp", "out", "exp.exp6.webserver.SimpleWebServer", "8080", "wwwroot")),
                        new CommandAction("BROWSE|http://127.0.0.1:8080/", null),
                        new CommandAction("BROWSE|http://127.0.0.1:8080/cgi/echo?name=alice&score=95", null)
                )
        ));
    }

    private static List<String> list(String... items) {
        List<String> values = new ArrayList<>();
        for (String item : items) {
            values.add(item);
        }
        return values;
    }

    private static List<CommandAction> listActions(CommandAction... actions) {
        List<CommandAction> values = new ArrayList<>();
        for (CommandAction action : actions) {
            values.add(action);
        }
        return values;
    }

    private static List<String> command(String... args) {
        List<String> cmd = new ArrayList<>();
        for (String arg : args) {
            cmd.add(arg);
        }
        return cmd;
    }

    private static class ExperimentView {
        private final String description;
        private final List<String> commandDescriptions;
        private final List<CommandAction> actions;

        private ExperimentView(String description, List<String> commandDescriptions, List<CommandAction> actions) {
            this.description = description;
            this.commandDescriptions = commandDescriptions;
            this.actions = actions;
        }
    }

    private static class CommandAction {
        private final String label;
        private final List<String> command;

        private CommandAction(String label, List<String> command) {
            this.label = label;
            this.command = command;
        }
    }
}
