package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

    // Limits from skeleton
    private static final int MAXFILES = 5;
    private static final int MAXBLOCKS = 10;
    private static final int BLOCK_SIZE = 128;

    // Singleton instance
    private static FileSystemManager instance;

    // Disk + concurrency
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    // In-memory structures
    private final FEntry[] inodeTable;   // file entries
    private final boolean[] freeBlockList;

    // Private constructor (singleton)
    private FileSystemManager(String filename, int totalSize) throws IOException {
        File f = new File(filename);
        this.disk = new RandomAccessFile(f, "rw");

        // to make sure disk file has the right size
        if (disk.length() < totalSize) {
            disk.setLength(totalSize);
        }

        // Initialize inode table and free block list
        inodeTable = new FEntry[MAXFILES];
        freeBlockList = new boolean[MAXBLOCKS];

        // For now: reserve block 0 for metadata, others are free
        freeBlockList[0] = false;
        for (int i = 1; i < MAXBLOCKS; i++) {
            freeBlockList[i] = true;
        }
    }

    // Public factory for singleton
    public static synchronized FileSystemManager getInstance(String filename, int totalSize) throws IOException {
        if (instance == null) {
            instance = new FileSystemManager(filename, totalSize);
        }
        return instance;
    }

    // --------- Helper methods ---------

    private int findFreeEntry() {
        for (int i = 0; i < MAXFILES; i++) {
            if (inodeTable[i] == null) {
                return i;
            }
        }
        return -1;
    }

    private int lookupFile(String filename) {
        for (int i = 0; i < MAXFILES; i++) {
            if (inodeTable[i] != null && inodeTable[i].getFilename().equals(filename)) {
                return i;
            }
        }
        return -1;
    }

    private int findFreeBlock() {
        // skip block 0 (metadata)
        for (int i = 1; i < MAXBLOCKS; i++) {
            if (freeBlockList[i]) {
                return i;
            }
        }
        return -1;
    }

    // ---------core methods ---------

    public void createFile(String filename) {
        globalLock.lock();
        try {
            if (filename.length() > 11) {
                throw new IllegalArgumentException("ERROR: filename too large");
            }

            if (lookupFile(filename) != -1) {
                throw new IllegalStateException("ERROR: file already exists");
            }

            int entryIndex = findFreeEntry();
            if (entryIndex == -1) {
                throw new IllegalStateException("ERROR: maximum file count reached");
            }

            // Phase 1: create empty file (no blocks yet)
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
                throw new IllegalArgumentException("ERROR: file " + filename + " does not exist");
            }

            //FEntry entry = inodeTable[idx];


            inodeTable[idx] = null;
            System.out.println("Deleted file: " + filename);
        } finally {
            globalLock.unlock();
        }
    }

    public List<String> listFiles() {
        List<String> files = new ArrayList<>();
        for (FEntry e : inodeTable) {
            if (e != null) {
                files.add(e.getFilename() + " (" + e.getFilesize() + " bytes)");
            }
        }
        return files;
    }

    // Debug helper for Phase 1 testing
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

    // Later we can add: readFile, writeFile, etc.
}
