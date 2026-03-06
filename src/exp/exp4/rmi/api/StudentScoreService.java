package exp.exp4.rmi.api;

import exp.exp4.rmi.model.StudentRecord;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface StudentScoreService extends Remote {
    boolean addStudent(StudentRecord record) throws RemoteException;

    boolean deleteStudent(String studentId) throws RemoteException;

    boolean updateStudent(StudentRecord record) throws RemoteException;

    StudentRecord queryStudent(String studentId) throws RemoteException;

    List<StudentRecord> listStudents() throws RemoteException;
}
