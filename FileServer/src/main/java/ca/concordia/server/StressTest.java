package ca.concordia.server;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class StressTest {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;
    private static final int NUM_CLIENTS = 10;
    private static final String[] OPERATIONS = {"CREATE", "WRITE", "READ", "DELETE", "LIST"};

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== STRESS TEST ===");
        System.out.println("Launching " + NUM_CLIENTS + " clients performing random operations...\n");

        // RESET FILESYSTEM FIRST
        resetFilesystem();

        CountDownLatch latch = new CountDownLatch(NUM_CLIENTS);

        for (int i = 0; i < NUM_CLIENTS; i++) {
            final int clientId = i;
            new Thread(() -> {
                stressClient(clientId, latch);
            }).start();

            // Stagger the starts slightly
            Thread.sleep(50);
        }

        latch.await();

        System.out.println("\n=== STRESS TEST COMPLETED ===");
        System.out.println("Check for crashes, deadlocks, or unexpected errors above");
    }

    private static void resetFilesystem() {
        System.out.println("Resetting filesystem...");

        File diskFile = new File("disk.dat");
        if (diskFile.exists()) {
            diskFile.delete();
            System.out.println("✓ Deleted disk.dat");
        }

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Filesystem reset complete\n");
    }

    private static void stressClient(int clientId, CountDownLatch latch) {
        Random random = new Random(clientId);

        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("[Client " + clientId + "] Connected");

            String myFile = "str" + clientId + ".txt"; // CHANGED: shortened filename

            // Perform 10 random operations
            for (int i = 0; i < 10; i++) {
                int opIndex = random.nextInt(OPERATIONS.length);
                String operation = OPERATIONS[opIndex];
                String command = "";

                switch (operation) {
                    case "CREATE":
                        command = "CREATE " + myFile;
                        break;
                    case "WRITE":
                        command = "WRITE " + myFile + " RandomData" + random.nextInt(1000);
                        break;
                    case "READ":
                        command = "READ " + myFile;
                        break;
                    case "DELETE":
                        command = "DELETE " + myFile;
                        break;
                    case "LIST":
                        command = "LIST";
                        break;
                }

                out.println(command);
                String response = in.readLine();
                System.out.println("[Client " + clientId + "] " + operation + " -> " +
                        (response.startsWith("OK") || response.startsWith("ERROR") ?
                                response.substring(0, Math.min(30, response.length())) : "Response"));

                // Random delay between operations
                Thread.sleep(random.nextInt(100) + 50);
            }

            out.println("exit");
            System.out.println("[Client " + clientId + "] Finished all operations");

        } catch (IOException | InterruptedException e) {
            System.err.println("[Client " + clientId + "] Error: " + e.getMessage());
        } finally {
            latch.countDown();
        }
    }
}
