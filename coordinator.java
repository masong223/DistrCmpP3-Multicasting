public static void main(String[] args) {
    File fileConfig = args[0];
    int port;
    int timeThreshold; 

    try (Scanner configReader = new Scanner(fileConfig)) {
        for (int i = 0; i < 2; i++) {
            if (configReader.hasNextLine()) {
                String line = configReader.nextLine();
                

            }


        }

    }

}