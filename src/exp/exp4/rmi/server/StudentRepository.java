package exp.exp4.rmi.server;

import exp.exp4.rmi.model.StudentRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StudentRepository {
    private final Map<String, StudentRecord> data = new ConcurrentHashMap<>();
    private final Path dbFile;

    public StudentRepository(String dbPath) throws IOException {
        this.dbFile = Paths.get(dbPath);
        if (Files.exists(dbFile)) {
            load();
        } else {
            if (dbFile.getParent() != null) {
                Files.createDirectories(dbFile.getParent());
            }
            Files.createFile(dbFile);
        }
    }

    public synchronized boolean add(StudentRecord record) throws IOException {
        if (record == null || data.containsKey(record.getStudentId())) {
            return false;
        }
        data.put(record.getStudentId(), record);
        persist();
        return true;
    }

    public synchronized boolean delete(String studentId) throws IOException {
        StudentRecord removed = data.remove(studentId);
        if (removed == null) {
            return false;
        }
        persist();
        return true;
    }

    public synchronized boolean update(StudentRecord record) throws IOException {
        if (record == null || !data.containsKey(record.getStudentId())) {
            return false;
        }
        data.put(record.getStudentId(), record);
        persist();
        return true;
    }

    public StudentRecord query(String studentId) {
        return data.get(studentId);
    }

    public List<StudentRecord> list() {
        List<StudentRecord> records = new ArrayList<>(data.values());
        records.sort(Comparator.comparing(StudentRecord::getStudentId));
        return records;
    }

    private void load() throws IOException {
        List<String> lines = Files.readAllLines(dbFile, StandardCharsets.UTF_8);
        data.clear();
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split(",", -1);
            if (parts.length != 5) {
                continue;
            }
            StudentRecord record = new StudentRecord(
                    parts[0],
                    parts[1],
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Double.parseDouble(parts[4])
            );
            data.put(record.getStudentId(), record);
        }
    }

    private void persist() throws IOException {
        List<String> lines = new ArrayList<>();
        for (StudentRecord record : list()) {
            String line = String.join(",",
                    record.getStudentId(),
                    record.getName(),
                    String.valueOf(record.getChinese()),
                    String.valueOf(record.getMath()),
                    String.valueOf(record.getEnglish())
            );
            lines.add(line);
        }
        Files.write(dbFile, lines, StandardCharsets.UTF_8);
    }
}
