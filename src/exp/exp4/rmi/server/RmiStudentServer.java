package exp.exp4.rmi.server;

import exp.exp4.rmi.api.StudentScoreService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiStudentServer {
    public static final String SERVICE_NAME = "StudentScoreService";

    public static void main(String[] args) {
        int registryPort = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
        String dbPath = args.length > 1 ? args[1] : "data/student_scores.csv";

        try {
            try {
                LocateRegistry.createRegistry(registryPort);
            } catch (Exception ignored) {
            }

            StudentRepository repository = new StudentRepository(dbPath);
            StudentScoreService service = new StudentScoreServiceImpl(repository);

            Registry registry = LocateRegistry.getRegistry(registryPort);
            registry.rebind(SERVICE_NAME, service);

            System.out.println("RMI Student server ready on port " + registryPort);
            System.out.println("Service name: " + SERVICE_NAME);
            System.out.println("Database file: " + dbPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
