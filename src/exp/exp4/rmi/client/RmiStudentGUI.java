package exp.exp4.rmi.client;

import exp.exp4.rmi.api.StudentScoreService;
import exp.exp4.rmi.model.StudentRecord;
import exp.exp4.rmi.server.RmiStudentServer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

/**
 * RMI 学生成绩管理 GUI：表格展示 + 增删改查表单
 * Usage: java exp.exp4.rmi.client.RmiStudentGUI [host] [port]
 */
public class RmiStudentGUI {
    private StudentScoreService service;
    private DefaultTableModel tableModel;
    private JTextField idField, nameField, chineseField, mathField, englishField;
    private JLabel statusLabel;
    private JFrame frame;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 1099;
        SwingUtilities.invokeLater(() -> new RmiStudentGUI().show(host, port));
    }

    private void show(String host, int port) {
        frame = new JFrame("RMI 学生成绩管理 - " + host + ":" + port);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(780, 540);
        frame.setLayout(new BorderLayout(8, 8));

        // 连接
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            service = (StudentScoreService) registry.lookup(RmiStudentServer.SERVICE_NAME);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "无法连接 RMI 服务: " + e.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 表格
        String[] cols = {"学号", "姓名", "语文", "数学", "英语", "平均分"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        table.setRowHeight(24);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int r = table.getSelectedRow();
                idField.setText(str(tableModel.getValueAt(r, 0)));
                nameField.setText(str(tableModel.getValueAt(r, 1)));
                chineseField.setText(str(tableModel.getValueAt(r, 2)));
                mathField.setText(str(tableModel.getValueAt(r, 3)));
                englishField.setText(str(tableModel.getValueAt(r, 4)));
            }
        });

        // 表单
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("操作"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        Font f = new Font("Microsoft YaHei UI", Font.PLAIN, 13);

        idField = addFormRow(form, c, 0, "学号:", f);
        nameField = addFormRow(form, c, 1, "姓名:", f);
        chineseField = addFormRow(form, c, 2, "语文:", f);
        mathField = addFormRow(form, c, 3, "数学:", f);
        englishField = addFormRow(form, c, 4, "英语:", f);

        JPanel btns = new JPanel(new GridLayout(2, 3, 6, 6));
        JButton addBtn = new JButton("添加");
        JButton updateBtn = new JButton("修改");
        JButton deleteBtn = new JButton("删除");
        JButton queryBtn = new JButton("查询");
        JButton refreshBtn = new JButton("刷新列表");
        JButton clearBtn = new JButton("清空表单");
        for (JButton b : new JButton[]{addBtn, updateBtn, deleteBtn, queryBtn, refreshBtn, clearBtn}) {
            b.setFont(f);
        }
        btns.add(addBtn); btns.add(updateBtn); btns.add(deleteBtn);
        btns.add(queryBtn); btns.add(refreshBtn); btns.add(clearBtn);

        c.gridx = 0; c.gridy = 5; c.gridwidth = 2;
        form.add(btns, c);

        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(form, BorderLayout.EAST);
        frame.add(statusLabel, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> doAdd());
        updateBtn.addActionListener(e -> doUpdate());
        deleteBtn.addActionListener(e -> doDelete());
        queryBtn.addActionListener(e -> doQuery());
        refreshBtn.addActionListener(e -> doRefresh());
        clearBtn.addActionListener(e -> clearForm());

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        doRefresh();
    }

    private JTextField addFormRow(JPanel panel, GridBagConstraints c, int row, String label, Font f) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        JLabel l = new JLabel(label); l.setFont(f);
        panel.add(l, c);
        c.gridx = 1; c.weightx = 1;
        JTextField tf = new JTextField(12);
        tf.setFont(new Font("Consolas", Font.PLAIN, 13));
        panel.add(tf, c);
        return tf;
    }

    private void doRefresh() {
        new Thread(() -> {
            try {
                List<StudentRecord> records = service.listStudents();
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (StudentRecord r : records) {
                        tableModel.addRow(new Object[]{
                                r.getStudentId(), r.getName(),
                                r.getChinese(), r.getMath(), r.getEnglish(),
                                String.format("%.2f", r.average())
                        });
                    }
                    status("已刷新，共 " + records.size() + " 条记录");
                });
            } catch (Exception ex) { status("刷新失败: " + ex.getMessage()); }
        }, "rmi-refresh").start();
    }

    private void doAdd() {
        new Thread(() -> {
            try {
                StudentRecord rec = buildRecord();
                boolean ok = service.addStudent(rec);
                status(ok ? "添加成功" : "添加失败(学号已存在?)");
                if (ok) doRefresh();
            } catch (Exception ex) { status("添加失败: " + ex.getMessage()); }
        }, "rmi-add").start();
    }

    private void doUpdate() {
        new Thread(() -> {
            try {
                StudentRecord rec = buildRecord();
                boolean ok = service.updateStudent(rec);
                status(ok ? "修改成功" : "修改失败(学号不存在?)");
                if (ok) doRefresh();
            } catch (Exception ex) { status("修改失败: " + ex.getMessage()); }
        }, "rmi-update").start();
    }

    private void doDelete() {
        new Thread(() -> {
            try {
                String id = idField.getText().trim();
                boolean ok = service.deleteStudent(id);
                status(ok ? "删除成功" : "删除失败(学号不存在?)");
                if (ok) doRefresh();
            } catch (Exception ex) { status("删除失败: " + ex.getMessage()); }
        }, "rmi-delete").start();
    }

    private void doQuery() {
        new Thread(() -> {
            try {
                String id = idField.getText().trim();
                StudentRecord r = service.queryStudent(id);
                if (r == null) {
                    status("未找到学号: " + id);
                } else {
                    SwingUtilities.invokeLater(() -> {
                        nameField.setText(r.getName());
                        chineseField.setText(String.valueOf(r.getChinese()));
                        mathField.setText(String.valueOf(r.getMath()));
                        englishField.setText(String.valueOf(r.getEnglish()));
                    });
                    status("查询到: " + r.getName());
                }
            } catch (Exception ex) { status("查询失败: " + ex.getMessage()); }
        }, "rmi-query").start();
    }

    private StudentRecord buildRecord() {
        return new StudentRecord(
                idField.getText().trim(),
                nameField.getText().trim(),
                Double.parseDouble(chineseField.getText().trim()),
                Double.parseDouble(mathField.getText().trim()),
                Double.parseDouble(englishField.getText().trim())
        );
    }

    private void clearForm() {
        idField.setText(""); nameField.setText("");
        chineseField.setText(""); mathField.setText(""); englishField.setText("");
    }

    private void status(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }

    private String str(Object o) { return o == null ? "" : o.toString(); }
}
