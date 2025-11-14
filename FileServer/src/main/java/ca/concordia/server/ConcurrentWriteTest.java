package ca.concordia.server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class ConcurrentWriteTest {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;
    private static final String SHARED_FILE = "shared.txt";

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== CONCURRENT WRITE TEST ===");
        System.out.println("Testing multiple clients writing to the same file...\n");

        // RESET FILESYSTEM FIRST
        resetFilesystem();

        // Create the shared file
        System.out.println("Creating shared file: " + SHARED_FILE);
        createSharedFile();
        Thread.sleep(500);

        // Launch multiple clients to write to it
        int numClients = 3;
        CountDownLatch latch = new CountDownLatch(numClients);

        System.out.println("\nLaunching " + numClients + " clients to write simultaneously...\n");

        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            new Thread(() -> {
                writeToSharedFile(clientId, latch);
            }).start();
        }

        latch.await();

        // Read the final result
        Thread.sleep(500);
        System.out.println("\nReading final content of shared file...");
        readSharedFile();

        System.out.println("\n=== CONCURRENT WRITE TEST COMPLETED ===");
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

    private static void createSharedFile() {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("CREATE " + SHARED_FILE);
            String response = in.readLine();
            System.out.println("Create response: " + response);

            out.println("exit");

        } catch (IOException e) {
            System.err.println("Error creating shared file: " + e.getMessage());
        }
    }

    private static void writeToSharedFile(int clientId, CountDownLatch latch) {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("[Client " + clientId + "] Connected");

            String content = "Data_from_Client_" + clientId;
            out.println("WRITE " + SHARED_FILE + " " + content);
            String response = in.readLine();
            System.out.println("[Client " + clientId + "] WRITE response: " + response);

            out.println("exit");

        } catch (IOException e) {
            System.err.println("[Client " + clientId + "] Error: " + e.getMessage());
        } finally {
            latch.countDown();
        }
    }

    private static void readSharedFile() {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("READ " + SHARED_FILE);
            String response = in.readLine();
            System.out.println("Final content: " + response);

            out.println("exit");

        } catch (IOException e) {
            System.err.println("Error reading shared file: " + e.getMessage());
        }
    }
}
