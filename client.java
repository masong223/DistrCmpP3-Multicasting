import java.io.*;
import java.net.*;
import java.util.Scanner;

public class client {
    public static void main(String[] args) {
        File configFile = new File(args[0]);
        int[] userIdTemp = {0}; // Client id array so I can avoid the issue of making vars in local scope
        String logFilePath = ""; // For storing messages
        String[] coordinatorInfo = new String[2]; // machine name, port number

        // Gather configuration information from the file
        try (Scanner configReader = new Scanner(configFile)) {
            for (int i = 0; i < 3; i++) {
                if (configReader.hasNextLine()) {
                    String line = configReader.nextLine();
                    String[] parts = line.split(" ");
                    if (i == 0) {
                        userIdTemp[0] = Integer.parseInt(parts[0]); //update with the real userId
                    }
                    if (i == 1) {
                        logFilePath = parts[0]; // Update client log location
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

        //Escaped local scope issues
        final File logFile = new File(logFilePath);
        final int userId = userIdTemp[0];

        System.out.println("User ID: " + userId);
        System.out.println("Log file: " + logFile.getAbsolutePath());
        System.out.println("Coordinator machine: " + coordinatorInfo[0]);
        System.out.println("Coordinator port: " + coordinatorInfo[1]);

        // Thread for sending messages to the server (Thread A)
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String inputToServer = scanner.nextLine();
                String[] parts = inputToServer.split(" ");
                //Command, port #, IP, UID

                if (parts[0].equals("register")) {

                    //Start ThreadB for listening from server
                    int bListenPort = Integer.parseInt(parts[1]);
                    ThreadB threadInit = new ThreadB(userId, logFile, bListenPort);
                    Thread listenerThread = new Thread(threadInit);
                    listenerThread.start();

                    //Get local IP
                    try {
                        String ip = InetAddress.getLocalHost().getHostAddress(); //Get IP

                    
                        //Start up connection to server
                        Socket coordSocket = new Socket(coordinatorInfo[0], Integer.parseInt(coordinatorInfo[1]));
                        PrintWriter clientOut = new PrintWriter(coordSocket.getOutputStream(), true);
                        InputStream cIn = coordSocket.getInputStream();
                        DataInputStream clientIn = new DataInputStream(cIn);

                        //Send command
                        clientOut.println("register " + bListenPort + " " + ip + " " + userId);

                        //Wait for ACK from server
                        String serverResponse = clientIn.readUTF();
                        if (serverResponse.contains("ACK")) {
                            System.out.println("Successfully registered");
                        }


                        clientOut.close();
                        clientIn.close();
                        coordSocket.close();
                    } catch (IOException e) {
                        System.err.println("Error connecting to server/obtaining IP");
                    }
                    

                } else if (parts[0].equals("deregister")) {

                } else if (parts[0].equals("disconnect")) {

                } else if (parts[0].equals("reconnect")) {

                } else if (parts[0].equals("msend")) {

                }
            }
        }).start();

    }
}
//Thread B, listening to Coordinator
class ThreadB implements Runnable {
    int userID;
    File logFile;
    //String[] coordinatorInfo;
    int bListenPort;

    public ThreadB(int userID, File logFile, int bListenPort) {
        this.userID = userID;
        this.logFile = logFile;
        this.bListenPort = bListenPort;
    }

    @Override
    public void run() {
        try {
            ServerSocket serversocket = new ServerSocket(bListenPort); // Serversocket to listen from server

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