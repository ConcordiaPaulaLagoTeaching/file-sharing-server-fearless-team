package ca.concordia.server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class SameFileConflictTest {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;
    private static final String CONFLICT_FILE = "dupl.txt"; // CHANGED: shortened to 8 chars

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SAME FILE CONFLICT TEST ===");
        System.out.println("Testing what happens when multiple clients try to create the same file...\n");

        // RESET FILESYSTEM FIRST
        resetFilesystem();

        int numClients = 3;
        CountDownLatch latch = new CountDownLatch(numClients);

        // All clients try to create the same file at the same time
        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            new Thread(() -> {
                tryCreateSameFile(clientId, latch);
            }).start();
        }

        latch.await();

        System.out.println("\n=== EXPECTED: One OK, others ERROR (file already exists) ===");
        System.out.println("=== SAME FILE CONFLICT TEST COMPLETED ===");
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

    private static void tryCreateSameFile(int clientId, CountDownLatch latch) {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("[Client " + clientId + "] Attempting to create: " + CONFLICT_FILE);

            out.println("CREATE " + CONFLICT_FILE);
            String response = in.readLine();

            if (response.startsWith("OK")) {
                System.out.println("[Client " + clientId + "] ✓ SUCCESS: " + response);
            } else {
                System.out.println("[Client " + clientId + "] ✗ FAILED (expected): " + response);
            }

            out.println("exit");

        } catch (IOException e) {
            System.err.println("[Client " + clientId + "] Error: " + e.getMessage());
        } finally {
            latch.countDown();
        }
    }
}
