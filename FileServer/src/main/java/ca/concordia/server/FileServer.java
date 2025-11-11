package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private final FileSystemManager fsManager;
    private final int port;

    public FileServer(int port, String fileSystemName, int totalSize) throws IOException {
        this.port = port;
        this.fsManager = FileSystemManager.getInstance(fileSystemName, totalSize);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Handling Client: " + clientSocket);

                // Spawn a new handler thread for each client
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received from client: " + line);
                String[] parts = line.split(" ");
                String command = parts[0].toUpperCase();

                switch (command) {
                    case "CREATE":
                        if (parts.length < 2) {
                            writer.println("ERROR: Missing filename");
                        } else {
                            try {
                                fsManager.createFile(parts[1]);
                                writer.println("OK: Created " + parts[1]);
                            } catch (Exception e) {
                                writer.println("ERROR: " + e.getMessage());
                            }
                        }
                        break;

                    case "DELETE":
                        if (parts.length < 2) {
                            writer.println("ERROR: Missing filename");
                        } else {
                            try {
                                fsManager.deleteFile(parts[1]);
                                writer.println("OK: Deleted " + parts[1]);
                            } catch (Exception e) {
                                writer.println("ERROR: " + e.getMessage());
                            }
                        }
                        break;

                    case "LIST":
                        writer.println(fsManager.listFiles());
                        break;

                    default:
                        writer.println("ERROR: Unknown command");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Main method to start the server
    public static void main(String[] args) throws IOException {
        FileServer server = new FileServer(12345, "disk.dat", 1280);
        server.start();
    }
}
