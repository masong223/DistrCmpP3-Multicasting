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
        int finalThreshold = threshold;
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(listenerPort)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket, finalThreshold)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void handleClient(Socket clientSocket, int threshold) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String message;
            while ((message = in.readLine()) != null) {
                String[] parts = message.split(" "); // UPDATE SUBSEQUENT METHODS WITH THIS ARRAY LATER
                switch (message.split(" ")[0]) {
                    case "register":
                        out.println("ACK");
                        String userIp = clientSocket.getInetAddress().getHostAddress();
                        System.out.println("Received registration from user " + parts[3] + " at IP " + userIp);
                        int userId = Integer.parseInt(parts[3]);
                        int bPort = Integer.parseInt(parts[1]);

                        userStatus.put(userId, new Participant(userId, bPort, userIp));
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
                        // Get info from message
                        int newPort = Integer.parseInt(parts[1]);
                        String newIp = clientSocket.getInetAddress().getHostAddress();
                        int uid = Integer.parseInt(parts[3]);
                        // Update user info and reconnect
                        Participant user = userStatus.get(uid);
                        user.updateConnection(newPort, newIp);
                        user.reconnect();
                        try {
                            BufferedReader logReader = new BufferedReader(new FileReader("log.txt"));
                            String logLine;
                            while ((logLine = logReader.readLine()) != null) {
                               if (Instant.parse(logLine.split(" ")[1]).isAfter(user.lastDisconnect) && Instant.parse(logLine.split(" ")[1]).isAfter(Instant.now().minusSeconds(threshold))) {
                                    out.println(logLine);
                                }
                            }
                        } 
                        catch (IOException e) {
                            System.out.println("Error reading log file: " + e.getMessage());
                            e.printStackTrace();
                        }
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
                            Files.writeString(pwd, text + " " + Instant.now() + "\n", StandardOpenOption.APPEND);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        userStatus.forEach((id, participant) -> {
                            if (participant.isConnected()) {
                                try {
                                    Socket socket = new Socket(participant.getIp(), participant.getPort());
                                    PrintWriter participantOut = new PrintWriter(socket.getOutputStream(), true);
                                    participantOut.println(text);
                                    System.out.println("Sent message to user " + id);
                                    socket.close();
                                } catch (IOException e) {
                                    System.out.println("Error sending message to user " + id + ": " + e.getMessage());
                                    e.printStackTrace();
                                }
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
        private String ip;
        private int port;

        public Participant(int userID, int port, String ip) {
            this.userID = userID;
            this.port = port;
            this.ip = ip;
            this.isConnected = true;
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

        public void updateConnection(int port, String ip) {
            this.port = port;
            this.ip = ip;
        }

        public void reconnect() {
            this.isConnected = true;
            this.lastDisconnect = null;
        }

        public String getIp() {
            return ip;
        }

        public int getPort() {
            return port;
        }
    }
}