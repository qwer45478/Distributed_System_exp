package exp.exp5.httpclient;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

public class HttpSocketBrowserUI {
    private final SimpleHttpSocketClient client = new SimpleHttpSocketClient();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HttpSocketBrowserUI().show());
    }

    private void show() {
        JFrame frame = new JFrame("Socket HTTP Browser - Experiment 5");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JComboBox<String> method = new JComboBox<>(new String[]{"GET", "POST"});
        JTextField urlField = new JTextField("http://example.com", 40);
        JButton sendButton = new JButton("Send");
        controls.add(new JLabel("Method"));
        controls.add(method);
        controls.add(new JLabel("URL"));
        controls.add(urlField);
        controls.add(sendButton);

        JTextArea postBody = new JTextArea("name=alice&score=100", 4, 80);
        postBody.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane postPane = new JScrollPane(postBody);
        postPane.setBorder(BorderFactory.createTitledBorder("POST Form Body (x-www-form-urlencoded)"));

        top.add(controls, BorderLayout.NORTH);
        top.add(postPane, BorderLayout.CENTER);

        JTextArea headerArea = new JTextArea();
        headerArea.setEditable(false);
        headerArea.setFont(new Font("Consolas", Font.PLAIN, 13));

        JTextArea bodyArea = new JTextArea();
        bodyArea.setEditable(false);
        bodyArea.setFont(new Font("Consolas", Font.PLAIN, 13));

        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(headerArea),
                new JScrollPane(bodyArea)
        );
        split.setResizeWeight(0.35);
        split.setBorder(BorderFactory.createTitledBorder("Response"));

        frame.add(top, BorderLayout.NORTH);
        frame.add(split, BorderLayout.CENTER);

        sendButton.addActionListener(e -> {
            sendButton.setEnabled(false);
            new Thread(() -> {
                try {
                    String chosen = String.valueOf(method.getSelectedItem());
                    String url = urlField.getText().trim();
                    String raw;
                    if ("POST".equals(chosen)) {
                        raw = client.sendPost(url, postBody.getText());
                    } else {
                        raw = client.sendGet(url);
                    }

                    HttpResponseView parsed = HttpResponseView.parse(raw);
                    SwingUtilities.invokeLater(() -> {
                        headerArea.setText(parsed.getHeaders());
                        bodyArea.setText(parsed.getBody());
                        bodyArea.setCaretPosition(0);
                        sendButton.setEnabled(true);
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        headerArea.setText("Request failed");
                        bodyArea.setText(ex.toString());
                        sendButton.setEnabled(true);
                    });
                }
            }, "http-request-thread").start();
        });

        frame.setSize(new Dimension(1100, 760));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
