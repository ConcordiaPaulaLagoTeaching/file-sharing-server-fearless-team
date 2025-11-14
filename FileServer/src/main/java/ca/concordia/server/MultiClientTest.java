//package ca.concordia.server;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.concurrent.CountDownLatch;
//
//public class MultiClientTest {
//
//    private static final String HOST = "localhost";
//    private static final int PORT = 12345;
//
//    public static void main(String[] args) throws InterruptedException {
//        System.out.println("=== STARTING MULTI-CLIENT TEST ===");
//        System.out.println("Launching 5 clients simultaneously...\n");
//
//        int numClients = 5;
//        CountDownLatch latch = new CountDownLatch(numClients);
//
//        // Launch multiple clients at the same time
//        for (int i = 0; i < numClients; i++) {
//            final int clientId = i;
//            new Thread(() -> {
//                runClient(clientId, latch);
//            }).start();
//        }
//
//        latch.await();
//        System.out.println("\n=== ALL CLIENTS FINISHED ===");
//    }
//
//    private static void runClient(int clientId, CountDownLatch latch) {
//        try (Socket socket = new Socket(HOST, PORT);
//             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
//
//            System.out.println("[Client " + clientId + "] Connected to server");
//
//            // Each client creates its own file
//            String filename = "file" + clientId + ".txt";
//
//            out.println("CREATE " + filename);
//            String response = in.readLine();
//            System.out.println("[Client " + clientId + "] CREATE response: " + response);
//
//            // Small delay to let create finish
//            Thread.sleep(100);
//
//            // Write to the file
//            out.println("WRITE " + filename + " HelloFromClient" + clientId);
//            response = in.readLine();
//            System.out.println("[Client " + clientId + "] WRITE response: " + response);
//
//            Thread.sleep(100);
//
//            // Read back the file
//            out.println("READ " + filename);
//            response = in.readLine();
//            System.out.println("[Client " + clientId + "] READ response: " + response);
//
//            Thread.sleep(100);
//
//            // List all files
//            out.println("LIST");
//            response = in.readLine();
//            System.out.println("[Client " + clientId + "] LIST response: " + response);
//
//            // Exit
//            out.println("exit");
//            System.out.println("[Client " + clientId + "] Disconnected");
//
//        } catch (IOException | InterruptedException e) {
//            System.err.println("[Client " + clientId + "] Error: " + e.getMessage());
//        } finally {
//            latch.countDown();
//        }
//    }
//}
package ca.concordia.server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class MultiClientTest {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== STARTING MULTI-CLIENT TEST ===");

        // ADD THIS: Clean up old files first
        System.out.println("Cleaning up old files...");
        cleanupOldFiles();
        Thread.sleep(500);

        System.out.println("Launching 5 clients simultaneously...\n");

        int numClients = 5;
        CountDownLatch latch = new CountDownLatch(numClients);

        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            new Thread(() -> {
                runClient(clientId, latch);
            }).start();
        }

        latch.await();
        System.out.println("\n=== ALL CLIENTS FINISHED ===");
    }

    // ADD THIS NEW METHOD
    private static void cleanupOldFiles() {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Try to delete files that might exist from previous runs
            String[] possibleFiles = {"test.txt", "file0.txt", "file1.txt", "file2.txt",
                    "file3.txt", "file4.txt", "shared.txt", "conflict.txt"};

            for (String file : possibleFiles) {
                out.println("DELETE " + file);
                in.readLine(); // Read response (we don't care if it fails)
            }

            out.println("exit");
            System.out.println("Cleanup complete!\n");

        } catch (IOException e) {
            System.err.println("Cleanup error (probably OK): " + e.getMessage());
        }
    }

    private static void runClient(int clientId, CountDownLatch latch) {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("[Client " + clientId + "] Connected to server");

            String filename = "file" + clientId + ".txt";

            out.println("CREATE " + filename);
            String response = in.readLine();
            System.out.println("[Client " + clientId + "] CREATE response: " + response);

            Thread.sleep(100);

            out.println("WRITE " + filename + " HelloFromClient" + clientId);
            response = in.readLine();
            System.out.println("[Client " + clientId + "] WRITE response: " + response);

            Thread.sleep(100);

            out.println("READ " + filename);
            response = in.readLine();
            System.out.println("[Client " + clientId + "] READ response: " + response);

            Thread.sleep(100);

            out.println("LIST");
            response = in.readLine();
            System.out.println("[Client " + clientId + "] LIST response: " + response);

            out.println("exit");
            System.out.println("[Client " + clientId + "] Disconnected");

        } catch (IOException | InterruptedException e) {
            System.err.println("[Client " + clientId + "] Error: " + e.getMessage());
        } finally {
            latch.countDown();
        }
    }
}
