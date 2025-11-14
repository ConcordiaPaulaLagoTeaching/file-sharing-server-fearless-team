// package ca.concordia.filesystem;

// import ca.concordia.filesystem.datastructures.FEntry;

// import java.io.File;
// import java.io.IOException;
// import java.io.RandomAccessFile;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.locks.ReentrantLock;


// public class FileSystemManager {

//     // Limits from skeleton
//     private static final int MAXFILES = 5;
//     private static final int MAXBLOCKS = 10;
//     private static final int BLOCK_SIZE = 128;

//     // Singleton instance
//     private static FileSystemManager instance;

//     // Disk + concurrency
//     private final RandomAccessFile disk;
//     private final ReentrantLock globalLock = new ReentrantLock();

//     // In-memory structures
//     private final FEntry[] inodeTable;   // file entries
//     private final boolean[] freeBlockList;

//     // Private constructor (singleton)
//     private FileSystemManager(String filename, int totalSize) throws IOException {
//         File f = new File(filename);
//         this.disk = new RandomAccessFile(f, "rw");

//         // to make sure disk file has the right size
//         if (disk.length() < totalSize) {
//             disk.setLength(totalSize);
//         }

//         // Initialize inode table and free block list
//         inodeTable = new FEntry[MAXFILES];
//         freeBlockList = new boolean[MAXBLOCKS];

//         // For now: reserve block 0 for metadata, others are free
//         freeBlockList[0] = false;
//         for (int i = 1; i < MAXBLOCKS; i++) {
//             freeBlockList[i] = true;
//         }
//     }

//     // Public factory for singleton
//     public static synchronized FileSystemManager getInstance(String filename, int totalSize) throws IOException {
//         if (instance == null) {
//             instance = new FileSystemManager(filename, totalSize);
//         }
//         return instance;
//     }

//     // --------- Helper methods ---------

//     private int findFreeEntry() {
//         for (int i = 0; i < MAXFILES; i++) {
//             if (inodeTable[i] == null) {
//                 return i;
//             }
//         }
//         return -1;
//     }

//     private int lookupFile(String filename) {
//         for (int i = 0; i < MAXFILES; i++) {
//             if (inodeTable[i] != null && inodeTable[i].getFilename().equals(filename)) {
//                 return i;
//             }
//         }
//         return -1;
//     }

//     private int findFreeBlock() {
//         // skip block 0 (metadata)
//         for (int i = 1; i < MAXBLOCKS; i++) {
//             if (freeBlockList[i]) {
//                 return i;
//             }
//         }
//         return -1;
//     }

//     // ---------core methods ---------

//     public void createFile(String filename) {
//         globalLock.lock();
//         try {
//             if (filename.length() > 11) {
//                 throw new IllegalArgumentException("ERROR: filename too large");
//             }

//             if (lookupFile(filename) != -1) {
//                 throw new IllegalStateException("ERROR: file already exists");
//             }

//             int entryIndex = findFreeEntry();
//             if (entryIndex == -1) {
//                 throw new IllegalStateException("ERROR: maximum file count reached");
//             }

//             // Phase 1: create empty file (no blocks yet)
//             inodeTable[entryIndex] = new FEntry(filename, (short) 0, (short) -1);

//             System.out.println("Created file: " + filename);
//         } finally {
//             globalLock.unlock();
//         }
//     }

//     public void deleteFile(String filename) {
//         globalLock.lock();
//         try {
//             int idx = lookupFile(filename);
//             if (idx == -1) {
//                 throw new IllegalArgumentException("ERROR: file " + filename + " does not exist");
//             }

//             //FEntry entry = inodeTable[idx];


//             inodeTable[idx] = null;
//             System.out.println("Deleted file: " + filename);
//         } finally {
//             globalLock.unlock();
//         }
//     }

//     public String[] listFiles() {
//         List<String> files = new ArrayList<>();
//         for (FEntry e : inodeTable) {
//             if (e != null) {
//                 files.add(e.getFilename() + " (" + e.getFilesize() + " bytes)");
//             }
//         }
//         return files.toArray(new String[0]);
//     }

//     public void writeFile(String filename, String content) throws IOException {
//         globalLock.lock();
//         try {
//             int idx = lookupFile(filename);
//             if (idx == -1) {
//                 throw new IllegalArgumentException("ERROR: file " + filename + " does not exist");
//             }

//             FEntry entry = inodeTable[idx];
//             byte[] data = content.getBytes();
//             int requiredBlocks = (int) Math.ceil((double) data.length / BLOCK_SIZE);

//             if (requiredBlocks > MAXBLOCKS - 1) {
//                 throw new IllegalStateException("ERROR: content too large for file system");
//             }

//             // Allocate blocks
//             List<Integer> blockIndices = new ArrayList<>();
//             for (int i = 0; i < requiredBlocks; i++) {
//                 int blockNum = findFreeBlock();
//                 if (blockNum == -1) {
//                     throw new IllegalStateException("ERROR: not enough free blocks");
//                 }
//                 blockIndices.add(blockNum);
//                 freeBlockList[blockNum] = false;
//             }

//             // Write data to disk
//             for (int i = 0; i < blockIndices.size(); i++) {
//                 int blockNum = blockIndices.get(i);
//                 long offset = (long) blockNum * BLOCK_SIZE;
//                 disk.seek(offset);

//                 int bytesToWrite = Math.min(BLOCK_SIZE, data.length - i * BLOCK_SIZE);
//                 disk.write(data, i * BLOCK_SIZE, bytesToWrite);
//             }

//             // Update entry
//             entry.setFilesize((short) data.length);
//             entry.setFirstBlock(blockIndices.get(0).shortValue());

//             System.out.println("Wrote " + data.length + " bytes to file: " + filename);
//         } finally {
//             globalLock.unlock();
//         }
//     }

//     public String readFile(String filename) throws IOException {
//         globalLock.lock();
//         try {
//             int idx = lookupFile(filename);
//             if (idx == -1) {
//                 throw new IllegalArgumentException("ERROR: file " + filename + " does not exist");
//             }

//             FEntry entry = inodeTable[idx];
//             if (entry.getFilesize() == 0) {
//                 return "";
//             }

//             short firstBlock = entry.getFirstBlock();
//             int filesize = entry.getFilesize();
//             byte[] data = new byte[filesize];

//             // Read from disk
//             long offset = (long) firstBlock * BLOCK_SIZE;
//             disk.seek(offset);
//             disk.readFully(data);

//             System.out.println("Read " + filesize + " bytes from file: " + filename);
//             return new String(data);
//         } finally {
//             globalLock.unlock();
//         }
//     }

//     // Debug helper for Phase 1 testing
//     public void printState() {
//         System.out.println("\n===== FILE SYSTEM STATE =====");
//         for (int i = 0; i < inodeTable.length; i++) {
//             if (inodeTable[i] == null) {
//                 System.out.println("[" + i + "] EMPTY");
//             } else {
//                 System.out.println("[" + i + "] " + inodeTable[i].getFilename());
//             }
//         }

//         System.out.print("Free blocks: ");
//         for (int i = 0; i < freeBlockList.length; i++) {
//             if (freeBlockList[i]) {
//                 System.out.print(i + " ");
//             }
//         }
//         System.out.println("\n=============================");
//     }
// }


package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

    private static final int MAXFILES = 5;
    private static final int MAXBLOCKS = 10;
    private static final int BLOCK_SIZE = 128;

    private static FileSystemManager instance;

    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    // inode table stores metadata for each file
    private final FEntry[] inodeTable;
    // tracks which blocks are available
    private final boolean[] freeBlockList;

    private FileSystemManager(String filename, int totalSize) throws IOException {
        File f = new File(filename);
        this.disk = new RandomAccessFile(f, "rw");

        // ensure the disk file is big enough
        if (disk.length() < totalSize) {
            disk.setLength(totalSize);
        }

        inodeTable = new FEntry[MAXFILES];
        freeBlockList = new boolean[MAXBLOCKS];

        // block 0 is reserved for metadata
        freeBlockList[0] = false;
        for (int i = 1; i < MAXBLOCKS; i++) {
            freeBlockList[i] = true;
        }
    }

    public static synchronized FileSystemManager getInstance(String filename, int totalSize) throws IOException {
        if (instance == null) {
            instance = new FileSystemManager(filename, totalSize);
        }
        return instance;
    }

    // finds the first available slot in the inode table
    private int findFreeEntry() {
        for (int i = 0; i < MAXFILES; i++) {
            if (inodeTable[i] == null) {
                return i;
            }
        }
        return -1;
    }

    // searches for a file by name, returns its index or -1
    private int lookupFile(String filename) {
        for (int i = 0; i < MAXFILES; i++) {
            if (inodeTable[i] != null && inodeTable[i].getFilename().equals(filename)) {
                return i;
            }
        }
        return -1;
    }

    private int findFreeBlock() {
        for (int i = 1; i < MAXBLOCKS; i++) {  // start at 1, skip metadata block
            if (freeBlockList[i]) {
                return i;
            }
        }
        return -1;
    }

    public void createFile(String filename) {
        globalLock.lock();
        try {
            // check filename length
            if (filename.length() > 11) {
                throw new IllegalArgumentException("filename too large");
            }

            // make sure file doesn't already exist
            if (lookupFile(filename) != -1) {
                throw new IllegalStateException("file already exists");
            }

            // find a free spot in the inode table
            int entryIndex = findFreeEntry();
            if (entryIndex == -1) {
                throw new IllegalStateException("maximum file count reached");
            }

            // create the file entry (empty for now, no data blocks allocated)
            inodeTable[entryIndex] = new FEntry(filename, (short) 0, (short) -1);

            System.out.println("Created file: " + filename);
        } finally {
            globalLock.unlock();
        }
    }

    public void deleteFile(String filename) {
        globalLock.lock();
        try {
            int idx = lookupFile(filename);
            if (idx == -1) {
                throw new IllegalArgumentException("file " + filename + " does not exist");
            }

            // TODO: need to free up any allocated blocks when we implement block allocation

            inodeTable[idx] = null;
            System.out.println("Deleted file: " + filename);
        } finally {
            globalLock.unlock();
        }
    }

    public boolean writeFile(String filename, String content) {
        globalLock.lock();
        try {
            int idx = lookupFile(filename);
            if (idx == -1) {
                System.err.println("ERROR: file does not exist");
                return false;
            }

            // TODO: implement actual writing to disk blocks
            // For now, just return true as placeholder
            System.out.println("Writing to file: " + filename);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            globalLock.unlock();
        }
    }

    public String readFile(String filename) {
        globalLock.lock();
        try {
            int idx = lookupFile(filename);
            if (idx == -1) {
                System.err.println("ERROR: file does not exist");
                return null;
            }

            // TODO: implement actual reading from disk blocks
            // For now, return placeholder
            return "Content of " + filename;
        } catch (Exception e) {
            return null;
        } finally {
            globalLock.unlock();
        }
    }

    public String[] listFiles() {
        globalLock.lock();
        try {
            List<String> fileList = new ArrayList<>();
            for (int i = 0; i < inodeTable.length; i++) {
                if (inodeTable[i] != null) {
                    fileList.add(inodeTable[i].getFilename() + " (" + inodeTable[i].getFilesize() + " bytes)");
                }
            }
            return fileList.toArray(new String[0]);
        } finally {
            globalLock.unlock();
        }
    }

    // useful for debugging - prints out the current state
    public void printState() {
        System.out.println("\n===== FILE SYSTEM STATE =====");
        for (int i = 0; i < inodeTable.length; i++) {
            if (inodeTable[i] == null) {
                System.out.println("[" + i + "] EMPTY");
            } else {
                System.out.println("[" + i + "] " + inodeTable[i].getFilename());
            }
        }

        System.out.print("Free blocks: ");
        for (int i = 0; i < freeBlockList.length; i++) {
            if (freeBlockList[i]) {
                System.out.print(i + " ");
            }
        }
        System.out.println("\n=============================");
    }
}
