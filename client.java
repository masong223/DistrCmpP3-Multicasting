import java.io.*;
import java.net.*;
import java.util.Scanner;

public class client {
    public static void main(String[] args) {
        File configFile = new File(args[0]);
        int userId = 0; // Client id
        File logFile = new File(""); // For storing messages
        String[] coordinatorInfo = new String[2]; // machine name, port number

        // Gather configuration information from the file
        try (Scanner configReader = new Scanner(configFile)) {
            for (int i = 0; i < 3; i++) {
                if (configReader.hasNextLine()) {
                    String line = configReader.nextLine();
                    String[] parts = line.split(" ");
                    if (i == 0) {
                        userId = Integer.parseInt(parts[0]); // Update client ID
                    }
                    if (i == 1) {
                        logFile = new File(parts[0]); // Update client log location
                    }
                    if (i == 2) {
                        coordinatorInfo[0] = parts[0]; // Coordinator machine name
                        coordinatorInfo[1] = parts[1]; // Coordinator port
                    }
                }
            }
            configReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Config file not found: " + args[0]);
            System.exit(0);
        }

        System.out.println("User ID: " + userId);
        System.out.println("Log file: " + logFile.getAbsolutePath());
        System.out.println("Coordinator machine: " + coordinatorInfo[0]);
        System.out.println("Coordinator port: " + coordinatorInfo[1]);

        // Thread for receiving messages from the user
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String inputToServer = scanner.nextLine();
                String[] parts = inputToServer.split(" ");

                if (parts[0].equals("register")) {

                } else if (parts[0].equals("deregister")) {

                } else if (parts[0].equals("disconnect")) {

                } else if (parts[0].equals("reconnect")) {

                } else if (parts[0].equals("msend")) {

                }
            }
        }).start();
        // Thread for receiving messages from the coordinator

    }
}

class ThreadB implements Runnable {
    int userID;
    File logFile;
    String[] coordinatorInfo;

    public ThreadB(int userID, File logFile, String[] coordinatorInfo) {
        this.userID = userID;
        this.logFile = logFile;
        this.coordinatorInfo = coordinatorInfo;
    }

    @Override
    public void run() {
        try {
            ServerSocket serversocket = new ServerSocket(Integer.parseInt(coordinatorInfo[1])); // Serversocket to
                                                                                                // listen from server

            FileWriter appender = new FileWriter(logFile, true); //Append mode = true
            PrintWriter writer = new PrintWriter(appender, true); //AutoFlush = true

            while (true) {
                try {
                    Socket coordinatorSocket = serversocket.accept(); // Bind to coordinator
                    Scanner coordIn = new Scanner(coordinatorSocket.getInputStream());
                    if (coordIn.hasNextLine()) {
                        String message = coordIn.nextLine();
                        writer.println(message); //append message to file

                        // Clean up sockets when task is finished
                        coordIn.close();
                        coordinatorSocket.close();
                    }
                } catch (IOException e) {
                    // Error in message reception or socket acceptance
                }

            }
            
        } catch (IOException e) {
            System.err.print("Error in thread B " + e);
        }
    }
}