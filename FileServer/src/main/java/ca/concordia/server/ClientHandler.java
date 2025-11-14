package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                System.out.println("Received from client: " + line);

                if (line.equalsIgnoreCase("exit")) {
                    break;
                }

                String response = processCommand(line);
                out.println(response);
                System.out.println("Sent to client: " + response);
            }

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            closeResources();
        }
    }

    private String processCommand(String line) {
        String[] parts = line.split(" ", 3);
        String command = parts[0].toUpperCase();

        try {
            switch (command) {
                case "CREATE":
                    return handleCreate(parts);
                case "WRITE":
                    return handleWrite(parts);
                case "READ":
                    return handleRead(parts);
                case "DELETE":
                    return handleDelete(parts);
                case "LIST":
                    return handleList(parts);
                default:
                    return "ERROR: Unknown command: " + command;
            }
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private String handleCreate(String[] parts) {
        if (parts.length < 2) {
            return "ERROR: CREATE requires a filename";
        }

        try {
            FileSystemManager fsm = FileSystemManager.getInstance("filesystem.dat", 1280);
            fsm.createFile(parts[1]);
            return "OK: File created";
        } catch (IllegalArgumentException | IllegalStateException e) {
            return "ERROR: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR: Could not create file";
        }
    }

    private String handleWrite(String[] parts) {
        if (parts.length < 3) {
            return "ERROR: WRITE requires filename and content";
        }

        try {
            FileSystemManager fsm = FileSystemManager.getInstance("filesystem.dat", 1280);
            boolean success = fsm.writeFile(parts[1], parts[2]);
            return success ? "OK: Content written" : "ERROR: Could not write to file";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private String handleRead(String[] parts) {
        if (parts.length < 2) {
            return "ERROR: READ requires a filename";
        }

        try {
            FileSystemManager fsm = FileSystemManager.getInstance("filesystem.dat", 1280);
            String content = fsm.readFile(parts[1]);
            return (content != null) ? content : "ERROR: Could not read file";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private String handleDelete(String[] parts) {
        if (parts.length < 2) {
            return "ERROR: DELETE requires a filename";
        }

        try {
            FileSystemManager fsm = FileSystemManager.getInstance("filesystem.dat", 1280);
            fsm.deleteFile(parts[1]);
            return "OK: File deleted";
        } catch (IllegalArgumentException e) {
            return "ERROR: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR: Could not delete file";
        }
    }

    private String handleList(String[] parts) {
        try {
            FileSystemManager fsm = FileSystemManager.getInstance("filesystem.dat", 1280);
            String[] files = fsm.listFiles();

            if (files == null || files.length == 0) {
                return "No files";
            }

            return String.join(", ", files);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
            System.out.println("Client disconnected");
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }
}
