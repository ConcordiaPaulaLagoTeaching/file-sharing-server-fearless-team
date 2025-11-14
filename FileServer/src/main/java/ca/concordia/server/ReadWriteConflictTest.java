package ca.concordia.server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class ReadWriteConflictTest {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;
    private static final String TEST_FILE = "conf.txt"; // CHANGED: shortened to 8 chars

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== READ-WRITE CONFLICT TEST ===");
        System.out.println("Testing read operations while write operations are happening...\n");

        // RESET FILESYSTEM FIRST
        resetFilesystem();

        // Create the test file
        createTestFile();
        Thread.sleep(500);

        CountDownLatch latch = new CountDownLatch(2);

        // Start writer thread
        new Thread(() -> {
            writerClient(latch);
        }).start();

        // Small delay then start reader thread
        Thread.sleep(100);

        new Thread(() -> {
            readerClient(latch);
        }).start();

        latch.await();

        System.out.println("\n=== READ-WRITE CONFLICT TEST COMPLETED ===");
        System.out.println("Check above for any deadlocks or errors");
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

    private static void createTestFile() {
        System.out.println("Creating test file: " + TEST_FILE);
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("CREATE " + TEST_FILE);
            String response = in.readLine();
            System.out.println("Create response: " + response);

            out.println("WRITE " + TEST_FILE + " InitialContent");
            response = in.readLine();
            System.out.println("Initial write response: " + response);

            out.println("exit");

        } catch (IOException e) {
            System.err.println("Error creating test file: " + e.getMessage());
        }
    }

    private static void writerClient(CountDownLatch latch) {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("[WRITER] Started");

            // Perform multiple writes
            for (int i = 0; i < 5; i++) {
                out.println("WRITE " + TEST_FILE + " WriterData_" + i);
                String response = in.readLine();
                System.out.println("[WRITER] Write " + i + " response: " + response);
                Thread.sleep(200);
            }

            out.println("exit");
            System.out.println("[WRITER] Finished");

        } catch (IOException | InterruptedException e) {
            System.err.println("[WRITER] Error: " + e.getMessage());
        } finally {
            latch.countDown();
        }
    }

    private static void readerClient(CountDownLatch latch) {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("[READER] Started");

            // Perform multiple reads while writer is writing
            for (int i = 0; i < 5; i++) {
                out.println("READ " + TEST_FILE);
                String response = in.readLine();
                System.out.println("[READER] Read " + i + " response: " + response);
                Thread.sleep(200);
            }

            out.println("exit");
            System.out.println("[READER] Finished");

        } catch (IOException | InterruptedException e) {
            System.err.println("[READER] Error: " + e.getMessage());
        } finally {
            latch.countDown();
        }
    }
}
