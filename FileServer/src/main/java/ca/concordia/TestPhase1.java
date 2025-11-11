package ca.concordia;

import ca.concordia.filesystem.FileSystemManager;

public class TestPhase1 {
    public static void main(String[] args) {
        try {
            System.out.println("=== INITIALIZING FILE SYSTEM ===");
            FileSystemManager fs = FileSystemManager.getInstance("test_disk.dat", 1280);
            fs.printState();

            // Test 1: Create files
            System.out.println("\n=== TEST 1: CREATE FILES ===");
            fs.createFile("file1.txt");
            fs.createFile("file2.dat");
            fs.createFile("file3.bin");
            fs.printState();

            // Test 2: List files
            System.out.println("\n=== TEST 2: LIST FILES ===");
            System.out.println(fs.listFiles());

            // Test 3: Filename too long
            System.out.println("\n=== TEST 3: FILENAME TOO LONG ===");
            try {
                fs.createFile("verylongfilename.txt");
            } catch (Exception e) {
                System.out.println("✅ Caught error: " + e.getMessage());
            }

            // Test 4: File already exists
            System.out.println("\n=== TEST 4: FILE ALREADY EXISTS ===");
            try {
                fs.createFile("file1.txt");
            } catch (Exception e) {
                System.out.println("✅ Caught error: " + e.getMessage());
            }

            // Test 5: Delete file
            System.out.println("\n=== TEST 5: DELETE FILE ===");
            fs.deleteFile("file2.dat");
            fs.printState();

            // Test 6: Delete missing
            System.out.println("\n=== TEST 6: DELETE NON-EXISTENT FILE ===");
            try {
                fs.deleteFile("missing.txt");
            } catch (Exception e) {
                System.out.println("✅ Caught error: " + e.getMessage());
            }

            // Test 7: Fill to MAXFILES
            System.out.println("\n=== TEST 7: MAX FILES ===");
            fs.createFile("file4.txt");
            fs.createFile("file5.txt");
            fs.createFile("file6.txt");   // fills last free slot
            fs.printState();

            // Test 8: Exceed MAXFILES
            System.out.println("\n=== TEST 8: EXCEED MAX FILES ===");
            try {
                fs.createFile("file7.txt");  //gets failed
            } catch (Exception e) {
                System.out.println("✅ Caught error: " + e.getMessage());
            }

            System.out.println("\n=== ALL TESTS COMPLETED ===");
        } catch (Exception e) {
            System.err.println("❌ TEST FAILED");
            e.printStackTrace();
        }
    }
}
