package exp.exp4.rmi.server;

import exp.exp4.rmi.api.StudentScoreService;
import exp.exp4.rmi.model.StudentRecord;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.List;

public class StudentScoreServiceImpl extends UnicastRemoteObject implements StudentScoreService {
    private final StudentRepository repository;

    public StudentScoreServiceImpl(StudentRepository repository) throws RemoteException {
        super();
        this.repository = repository;
    }

    @Override
    public synchronized boolean addStudent(StudentRecord record) throws RemoteException {
        try {
            return repository.add(record);
        } catch (IOException e) {
            throw new RemoteException("Failed to add student", e);
        }
    }

    @Override
    public synchronized boolean deleteStudent(String studentId) throws RemoteException {
        try {
            return repository.delete(studentId);
        } catch (IOException e) {
            throw new RemoteException("Failed to delete student", e);
        }
    }

    @Override
    public synchronized boolean updateStudent(StudentRecord record) throws RemoteException {
        try {
            return repository.update(record);
        } catch (IOException e) {
            throw new RemoteException("Failed to update student", e);
        }
    }

    @Override
    public StudentRecord queryStudent(String studentId) {
        return repository.query(studentId);
    }

    @Override
    public List<StudentRecord> listStudents() {
        List<StudentRecord> records = repository.list();
        return Collections.unmodifiableList(records);
    }
}
