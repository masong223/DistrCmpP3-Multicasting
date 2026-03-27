import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Scanner;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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
                switch (message.split(" ")[0]) {
                    case "register":
                        out.println("ACK");
                        userStatus.put(Integer.parseInt(message.split(" ")[3]), new Participant(Integer.parseInt(message.split(" ")[3]), clientSocket));
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
                        userStatus.forEach((userId, participant) -> {
                            if (participant.isConnected()) {
                                try (PrintWriter participantOut = new PrintWriter(participant.getSocket().getOutputStream(), true)) {
                                    participantOut.println("Message: " + text);
                                } catch (IOException e) {
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
        private Socket socket;

        public Participant(int userID, Socket socket) {
            this.userID = userID;
            this.socket = socket;
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
        
        public void reconnect() {
            this.isConnected = true;
            this.lastDisconnect = null;
        }

        public Socket getSocket() {
            return socket;
        }
    }
}