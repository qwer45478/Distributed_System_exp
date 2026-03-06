package exp.exp4.rmi.model;

import java.io.Serializable;

public class StudentRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String name;
    private double chinese;
    private double math;
    private double english;

    public StudentRecord() {
    }

    public StudentRecord(String studentId, String name, double chinese, double math, double english) {
        this.studentId = studentId;
        this.name = name;
        this.chinese = chinese;
        this.math = math;
        this.english = english;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getChinese() {
        return chinese;
    }

    public void setChinese(double chinese) {
        this.chinese = chinese;
    }

    public double getMath() {
        return math;
    }

    public void setMath(double math) {
        this.math = math;
    }

    public double getEnglish() {
        return english;
    }

    public void setEnglish(double english) {
        this.english = english;
    }

    public double average() {
        return (chinese + math + english) / 3.0;
    }

    @Override
    public String toString() {
        return "StudentRecord{" +
                "studentId='" + studentId + '\'' +
                ", name='" + name + '\'' +
                ", chinese=" + chinese +
                ", math=" + math +
                ", english=" + english +
                ", avg=" + String.format("%.2f", average()) +
                '}';
    }
}
