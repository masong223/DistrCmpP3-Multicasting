import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Scanner;

public class Coordinator {
    public static void main(String[] args) {
        File configFile = new File(args[0]);
        int port = 0;
        int threshold = 0;
        HashMap<Integer, Boolean> userStatus = new HashMap<>(); // User ID to online/offline status

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
                new Thread(() -> handleClient(clientSocket, userStatus)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }).start();
    }


    private static void handleClient(Socket clientSocket, HashMap<Integer, Boolean> userStatus) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String message;
            while ((message = in.readLine()) != null) {
                switch (message.split(" ")[0]) {
                    case "register":
                        userStatus.put(Integer.parseInt(message.split(" ")[2]), true);
                        String responseReg = "ACK" + message.split("")[2];
                        out.println(responseReg);
                        break;
                    case "deregister":
                        userStatus.remove(Integer.parseInt(message.split(" ")[1]));
                        String responseDereg = "ACK" + message.split(" ")[1] ;
                        out.println(responseDereg);
                        break;
                    case "disconnect":
                        userStatus.put(Integer.parseInt(message.split(" ")[1]), false);
                        String responseDisc = "ACK" + message.split(" ")[1];
                        out.println(responseDisc);
                        break;
                    case "reconnect":
                        userStatus.put(Integer.parseInt(message.split(" ")[2]), true);
                        String responseReconnect = "ACK" + message.split(" ")[2];
                        out.println(responseReconnect);
                        break;
                    case "msend":
                        userStatus.forEach((userId, isOnline) -> {
                            if (isOnline) {
                                //Send
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

}