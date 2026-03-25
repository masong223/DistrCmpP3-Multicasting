import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public static void main(String[] args) {
   File configFile = new File(args[0]);
   int userId;
   File logFile;
   String[] coordinatorInfo = new String[2]; //machine name, port number

    
   try (Scanner configReader = new Scanner(configFile)) {
        for (int i = 0; i < 3; i++) {
            if (configReader.hasNextLine()) {
                String line = configReader.nextLine();
                String[] parts = line.split(" ");
                if (i == 0) {
                    userId = Integer.parseInt(parts[0]);
                }
                if (i == 1) {
                    logFile = new File(parts[0]);
                }
                if (i == 2) {
                    coordinatorInfo[0] = parts[0];
                    coordinatorInfo[1] = parts[1];
                }
            }
        }

   } catch (FileNotFoundException e) {
       System.out.println("Configuration file not found: " + args[0]);
   }

}