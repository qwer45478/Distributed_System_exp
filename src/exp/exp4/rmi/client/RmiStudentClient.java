package exp.exp4.rmi.client;

import exp.exp4.rmi.api.StudentScoreService;
import exp.exp4.rmi.model.StudentRecord;
import exp.exp4.rmi.server.RmiStudentServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class RmiStudentClient {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 1099;

        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            StudentScoreService service = (StudentScoreService) registry.lookup(RmiStudentServer.SERVICE_NAME);
            runConsole(service);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runConsole(StudentScoreService service) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("RMI Student Client started.");
        printHelp();

        while (true) {
            System.out.print("rmi> ");
            String line = scanner.nextLine();
            if (line == null) {
                break;
            }
            String[] parts = line.trim().split("\\s+");
            if (parts.length == 0 || parts[0].isEmpty()) {
                continue;
            }

            String cmd = parts[0].toLowerCase();
            switch (cmd) {
                case "help":
                    printHelp();
                    break;
                case "add":
                    if (parts.length != 6) {
                        System.out.println("Usage: add <id> <name> <chinese> <math> <english>");
                        break;
                    }
                    StudentRecord add = parseRecord(parts);
                    System.out.println(service.addStudent(add) ? "ADD OK" : "ADD FAIL (id exists?)");
                    break;
                case "update":
                    if (parts.length != 6) {
                        System.out.println("Usage: update <id> <name> <chinese> <math> <english>");
                        break;
                    }
                    StudentRecord update = parseRecord(parts);
                    System.out.println(service.updateStudent(update) ? "UPDATE OK" : "UPDATE FAIL (id not found?)");
                    break;
                case "delete":
                    if (parts.length != 2) {
                        System.out.println("Usage: delete <id>");
                        break;
                    }
                    System.out.println(service.deleteStudent(parts[1]) ? "DELETE OK" : "DELETE FAIL (id not found?)");
                    break;
                case "get":
                    if (parts.length != 2) {
                        System.out.println("Usage: get <id>");
                        break;
                    }
                    StudentRecord queried = service.queryStudent(parts[1]);
                    System.out.println(queried == null ? "NOT FOUND" : queried);
                    break;
                case "list":
                    List<StudentRecord> all = service.listStudents();
                    if (all.isEmpty()) {
                        System.out.println("EMPTY");
                    } else {
                        for (StudentRecord record : all) {
                            System.out.println(record);
                        }
                    }
                    break;
                case "quit":
                case "exit":
                    System.out.println("Bye.");
                    return;
                default:
                    System.out.println("Unknown command. type help");
            }
        }
    }

    private static StudentRecord parseRecord(String[] parts) {
        return new StudentRecord(
                parts[1],
                parts[2],
                Double.parseDouble(parts[3]),
                Double.parseDouble(parts[4]),
                Double.parseDouble(parts[5])
        );
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  add <id> <name> <chinese> <math> <english>");
        System.out.println("  update <id> <name> <chinese> <math> <english>");
        System.out.println("  delete <id>");
        System.out.println("  get <id>");
        System.out.println("  list");
        System.out.println("  help");
        System.out.println("  quit");
    }
}
