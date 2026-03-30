import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class coordinator {
    private static ConcurrentHashMap<Integer, Participant> userStatus = new ConcurrentHashMap<>();
    public static void main(String[] args) {
        File configFile = new File(args[0]);
        int port = 0;
        int threshold = 0;

        try (Scanner configReader = new Scanner(configFile)) {
            for (int i = 0; i < 2; i++) {
                if (configReader.hasNextLine()) {
                    String line = configReader.nextLine();
                    if (i == 0) {
                        port = Integer.parseInt(line);
                    }
                    if (i == 1) {
                        threshold = Integer.parseInt(line);
                    }
                }
            }
            configReader.close();
    } catch (FileNotFoundException e) {
        System.out.println("Configuration file not found: " + args[0]);
    }
    int listenerPort = port;
    new Thread(() -> {
        try (ServerSocket serverSocket = new ServerSocket(listenerPort)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }).start();
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String message;
            while ((message = in.readLine()) != null) {
                String[] parts = message.split(" "); //UPDATE SUBSEQUENT METHODS WITH THIS ARRAY LATER
                switch (message.split(" ")[0]) {
                    case "register":
                        out.println("ACK");
                        String userIp = clientSocket.getInetAddress().getHostAddress();
                        String realIp = clientSocket.getInetAddress().getHostAddress();
                        int userId = Integer.parseInt(parts[3]);
                        int bPort = Integer.parseInt(parts[1]);
                        
                        userStatus.put(userId, new Participant(userId, bPort, realIp));
                        break;
                    case "deregister":
                        out.println("ACK");
                        userStatus.remove(Integer.parseInt(message.split(" ")[1]));
                        break;
                    case "disconnect":
                        out.println("ACK");
                        userStatus.get(Integer.parseInt(message.split(" ")[1])).disconnect();
                        break;
                    case "reconnect":
                        out.println("ACK");
                        userStatus.get(Integer.parseInt(message.split(" ")[1])).reconnect();
                        break;
                    case "msend":
                        out.println("ACK");
                        Path pwd = Path.of("log.txt");
                        try {
                            if (!Files.exists(pwd)) {
                                Files.createFile(pwd);
                            }
                        } catch (IOException e) {
                            System.out.println("Error: " + e.getMessage());
                        }
                        String text = message.substring(6, message.length());
                        try {
                            Files.writeString(pwd, message + " " + Instant.now() + "\n", StandardOpenOption.APPEND);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        userStatus.forEach((id, participant) -> {
                            if (participant.isConnected() && participant.outStream != null) {
                                participant.outStream.println("msend " + text);
                            }
                        });
                        break;
                    default:
                        out.println("Unknown command");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Participant {
        private int userID = 0;
        private boolean isConnected = true;
        private Instant lastDisconnect;
        private Socket socket;
        private PrintWriter outStream;

        public Participant(int userID, int port, String ip) {
            this.userID = userID;
            makeConnection(port, ip);
            this.isConnected = true;
        }
        public void makeConnection (int port, String ip){
            
            try {
                this.socket = new Socket(ip, port);
                this.outStream = new PrintWriter(socket.getOutputStream(), true);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Could not connect to client B thread for user " + userID);
            }
        }
        public int getUserID() {
            return userID;
        }

        public boolean isConnected() {
            return isConnected;
        }

        public void disconnect() {
            this.isConnected = false;
            this.lastDisconnect = Instant.now();
        }
        
        public void reconnect() {
            this.isConnected = true;
            this.lastDisconnect = null;
        }

        public Socket getSocket() {
            return socket;
        }
    }
}