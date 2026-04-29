import java.io.*;
import java.net.*;
import java.util.Scanner;

public class client {
    public static void main(String[] args) {
        File configFile = new File(args[0]);
        int[] userIdTemp = { 0 }; // Client id array so I can avoid the issue of making vars in local scope
        String logFilePath = ""; // For storing messages
        String[] coordinatorInfo = new String[2]; // machine name, port number

        // Gather configuration information from the file
        try (Scanner configReader = new Scanner(configFile)) {
            for (int i = 0; i < 3; i++) {
                if (configReader.hasNextLine()) {
                    String line = configReader.nextLine();
                    String[] parts = line.split(" ");
                    if (i == 0) {
                        userIdTemp[0] = Integer.parseInt(parts[0]); // update with the real userId
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

        // Escaped local scope issues
        final File logFile = new File(logFilePath);
        final int userId = userIdTemp[0];

        System.out.println("User ID: " + userId);
        System.out.println("Log file: " + logFile.getAbsolutePath());
        System.out.println("Coordinator machine: " + coordinatorInfo[0]);
        System.out.println("Coordinator port: " + coordinatorInfo[1]);

        // Thread for sending messages to the server (Thread A)
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            ThreadB threadInit = null;
            while (true) {
                String inputToServer = scanner.nextLine();
                String[] parts = inputToServer.split(" ");
                // Command, port #, IP, UID

                if (parts[0].equals("register")) {
                    // SERVER PARAM CONFIG FOR REGISTER: "register, port #, ip, userId"
                    // Start ThreadB for listening from server
                    int bListenPort = Integer.parseInt(parts[1]);
                    threadInit = new ThreadB(userId, logFile, bListenPort);
                    Thread listenerThread = new Thread(threadInit);
                    listenerThread.start();

                    // Get local IP
                    try {

                        Socket coordSocket = new Socket(coordinatorInfo[0], Integer.parseInt(coordinatorInfo[1])); //Trying to connect to coordinator before getting IP
                        String ip = coordSocket.getLocalAddress().getHostAddress(); // Get IP from socket instead of from message

                        // Start up connection to server
                        
                        PrintWriter clientOut = new PrintWriter(coordSocket.getOutputStream(), true);
                        BufferedReader clientIn = new BufferedReader(new InputStreamReader(coordSocket.getInputStream()));
                        

                        // Send command
                        clientOut.println("register " + bListenPort + " " + ip + " " + userId);

                        // Wait for ACK from server
                        String serverResponse = clientIn.readLine();
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
                    // SERVER PARAMETER CONFIG FOR DEREGISTER: "deregister, userId"
                    try {

                        Socket coordSocket = new Socket(coordinatorInfo[0], Integer.parseInt(coordinatorInfo[1]));
                        PrintWriter clientOut = new PrintWriter(coordSocket.getOutputStream(), true);
                        BufferedReader clientIn = new BufferedReader(new InputStreamReader(coordSocket.getInputStream()));
                        

                        clientOut.println("deregister " + userId);
                        
                        String serverResponse = clientIn.readLine();
                        if (serverResponse != null && serverResponse.contains("ACK")) {
                            System.out.println("Successfully deregistered");
                        }

                        // If thread is running (it should be), stop the listener thread (thread b)
                        if (threadInit != null) {
                            threadInit.stopThreadB();
                            threadInit = null;
                        }
                    } catch (IOException e) {

                    }
                } else if (parts[0].equals("disconnect")) {
                    // SERVER PARAMETER CONFIG FOR DISCONNECT: "disconnect, userId"
                    try {
                        // Open connection to coordinator
                        Socket coordSocket = new Socket(coordinatorInfo[0], Integer.parseInt(coordinatorInfo[1]));
                        PrintWriter clientOut = new PrintWriter(coordSocket.getOutputStream(), true);
                        BufferedReader clientIn = new BufferedReader(new InputStreamReader(coordSocket.getInputStream()));


                        clientOut.println("disconnect " + userId);

                        // Wait for the ACK
                        String serverResponse = clientIn.readLine();
                        if (serverResponse.contains("ACK")) {
                            System.out.println("Successfully disconnected if already registered");
                        }

                        clientOut.close();
                        clientIn.close();
                        coordSocket.close();

                        // Stop thread b
                        if (threadInit != null) {
                            threadInit.stopThreadB();
                            threadInit = null;
                        }

                    } catch (IOException e) {
                        System.err.println("Error disconnecting");
                    }
                } else if (parts[0].equals("reconnect")) {
                    // SERVER PARAM CONFIG FOR RECONNECT: "reconnect, port #, ip, userId"
                    if (parts.length < 2) {
                        System.out.println("Command requires a port number");
                        continue;
                    }

                    int bListenPort = Integer.parseInt(parts[1]);
                    threadInit = new ThreadB(userId, logFile, bListenPort);
                    Thread listenerThread = new Thread(threadInit);
                    listenerThread.start();



                    try {
                        Socket coordSocket = new Socket(coordinatorInfo[0], Integer.parseInt(coordinatorInfo[1])); //Set up socket first
                        String ip = coordSocket.getLocalAddress().getHostAddress(); // Then get IP from socket instead from message

                        // Start up connection to server
                        
                        PrintWriter clientOut = new PrintWriter(coordSocket.getOutputStream(), true);
                        BufferedReader clientIn = new BufferedReader(new InputStreamReader(coordSocket.getInputStream()));

                        // Send command
                        clientOut.println("reconnect " + bListenPort + " " + ip + " " + userId);

                        // Wait for ACK from server
                        String serverResponse = clientIn.readLine();
                        if (serverResponse.contains("ACK")) {
                            System.out.println("Successfully reconnected if already registered");
                        }

                        clientOut.close();
                        clientIn.close();
                        coordSocket.close();
                    } catch (IOException e) {
                        System.err.println("Error connecting to server/obtaining IP");
                    }

                } else if (parts[0].equals("msend")) {
                    // SERVER PARAM CONFIG FOR MSEND: "msend `message` userId"
                    try {
                        Socket coordSocket = new Socket(coordinatorInfo[0], Integer.parseInt(coordinatorInfo[1]));
                        PrintWriter clientOut = new PrintWriter(coordSocket.getOutputStream(), true);
                        BufferedReader clientIn = new BufferedReader(new InputStreamReader(coordSocket.getInputStream()));

                        // Send command. Messages cannot contain spaces.
                        clientOut.println("msend " + inputToServer.substring(inputToServer.indexOf(" ") + 1) + " " + userId);

                        // Wait for ACK from server
                        String serverResponse = clientIn.readLine().trim();
                        if (serverResponse.equals("ACK e")) {
                            clientOut.close();
                            clientIn.close();
                            coordSocket.close();
                            System.out.println("Message not sent, not registered or disconnected.");
                        } else if (serverResponse.equals("ACK")) {
                            System.out.println("Message sent");
                        

                        clientOut.close();
                        clientIn.close();
                        coordSocket.close();
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }).start();

    }
}

// Thread B, listening to Coordinator
class ThreadB implements Runnable {
    int userID;
    File logFile;
    // String[] coordinatorInfo;
    int bListenPort;
    public volatile boolean isRunning = true;
    ServerSocket serverSocket;

    public ThreadB(int userID, File logFile, int bListenPort) {
        this.userID = userID;
        this.logFile = logFile;
        this.bListenPort = bListenPort;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(bListenPort); // Serversocket to listen from server

            FileWriter appender = new FileWriter(logFile, true); // Append mode = true
            PrintWriter writer = new PrintWriter(appender, true); // AutoFlush = true
            System.out.println("Thread B started, listening for messages from coordinator on port " + bListenPort);
            while (isRunning) {
                try {
                    Socket coordinatorSocket = serverSocket.accept(); // Bind to coordinator
                    Scanner coordIn = new Scanner(coordinatorSocket.getInputStream());
                    System.out.println("Thread B connected to coordinator for incoming message");
                    while (isRunning && coordIn.hasNextLine()) {
                        String message = coordIn.nextLine();
                        writer.println(message); // append message to file

                        // Clean up sockets when task is finished
                        
                    }
                        coordIn.close();
                        coordinatorSocket.close();
                } catch (IOException e) {
                    // Error in message reception or socket acceptance
                }

            }

        } catch (IOException e) {
            System.err.print("Error in thread B " + e);
        }
    }

    // Will stop thread B actions by closing the socket.
    public void stopThreadB() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}