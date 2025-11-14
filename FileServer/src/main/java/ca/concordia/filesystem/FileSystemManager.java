package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileSystemManager {
    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private static FileSystemManager instance = null;
    private final RandomAccessFile disk;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private static final int BLOCK_SIZE = 128;

    private FEntry[] fileEntries;  // Array of file entries
    private FNode[] fileNodes;     // Array of file nodes
    private int metadataBlocks;    // Number of blocks used for metadata

    // Private constructor for singleton
    private FileSystemManager(String filename, int totalSize) throws IOException {
        this.disk = new RandomAccessFile(filename, "rw");

        // Calculate metadata size
        int fentrySize = 15; // 11 bytes (filename) + 2 bytes (size) + 2 bytes (firstblock)
        int fnodeSize = 8;   // 4 bytes (blockindex) + 4 bytes (next)
        int metadataSize = (MAXFILES * fentrySize) + (MAXBLOCKS * fnodeSize);
        metadataBlocks = (int) Math.ceil((double) metadataSize / BLOCK_SIZE);

        // Initialize arrays
        fileEntries = new FEntry[MAXFILES];
        fileNodes = new FNode[MAXBLOCKS];

        // Initialize file system if file is empty or new
        if (disk.length() == 0) {
            initializeFileSystem(totalSize);
        } else {
            loadFileSystem();
        }
    }

    // Singleton getInstance method
    public static synchronized FileSystemManager getInstance(String filename, int totalSize) throws IOException {
        if (instance == null) {
            instance = new FileSystemManager(filename, totalSize);
        }
        return instance;
    }

    private void initializeFileSystem(int totalSize) throws IOException {
        // Set file size
        disk.setLength(totalSize);

        // Initialize file entries (all empty)
        for (int i = 0; i < MAXFILES; i++) {
            fileEntries[i] = new FEntry("", (short) 0, (short) -1);
        }

        // Initialize file nodes
        // First 'metadataBlocks' nodes are reserved for metadata
        for (int i = 0; i < MAXBLOCKS; i++) {
            if (i < metadataBlocks) {
                fileNodes[i] = new FNode(i, -1); // Mark metadata blocks as used
            } else {
                fileNodes[i] = new FNode(-i, -1); // Negative means free
            }
        }

        // Write metadata to disk
        saveMetadata();

        // Zero out all data blocks
        byte[] zeros = new byte[BLOCK_SIZE];
        for (int i = metadataBlocks; i < MAXBLOCKS; i++) {
            disk.seek((long) i * BLOCK_SIZE);
            disk.write(zeros);
        }
    }

    private void loadFileSystem() throws IOException {
        disk.seek(0);

        // Load file entries
        for (int i = 0; i < MAXFILES; i++) {
            byte[] nameBytes = new byte[11];
            disk.readFully(nameBytes);
            String filename = new String(nameBytes).trim().replace("\0", "");
            short filesize = disk.readShort();
            short firstBlock = disk.readShort();
            fileEntries[i] = new FEntry(filename.isEmpty() ? "" : filename, filesize, firstBlock);
        }

        // Load file nodes
        for (int i = 0; i < MAXBLOCKS; i++) {
            int blockIndex = disk.readInt();
            int next = disk.readInt();
            fileNodes[i] = new FNode(blockIndex, next);
        }
    }

    private void saveMetadata() throws IOException {
        disk.seek(0);

        // Save file entries
        for (int i = 0; i < MAXFILES; i++) {
            FEntry entry = fileEntries[i];
            byte[] nameBytes = new byte[11];
            if (entry.getFilename() != null && !entry.getFilename().isEmpty()) {
                byte[] name = entry.getFilename().getBytes();
                System.arraycopy(name, 0, nameBytes, 0, Math.min(name.length, 11));
            }
            disk.write(nameBytes);
            disk.writeShort(entry.getFilesize());
            disk.writeShort(entry.getFirstBlock());
        }

        // Save file nodes
        for (int i = 0; i < MAXBLOCKS; i++) {
            FNode node = fileNodes[i];
            disk.writeInt(node.getBlockIndex());
            disk.writeInt(node.getNext());
        }
    }

    public void createFile(String filename) throws Exception {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        if (filename.length() > 11) {
            throw new IllegalArgumentException("filename too large");
        }

        rwLock.writeLock().lock();
        try {
            // Check if file already exists
            for (int i = 0; i < MAXFILES; i++) {
                if (fileEntries[i].getFilename().equals(filename)) {
                    throw new IllegalArgumentException("File already exists");
                }
            }

            // Find free entry
            int freeEntryIndex = -1;
            for (int i = 0; i < MAXFILES; i++) {
                if (fileEntries[i].getFilename().isEmpty()) {
                    freeEntryIndex = i;
                    break;
                }
            }

            if (freeEntryIndex == -1) {
                throw new Exception("No free file entries available");
            }

            // Create empty file
            fileEntries[freeEntryIndex] = new FEntry(filename, (short) 0, (short) -1);

            // Save metadata
            saveMetadata();

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public boolean writeFile(String filename, String content) throws Exception {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        byte[] data = content.getBytes();

        rwLock.writeLock().lock();
        try {
            // Find file entry
            int entryIndex = -1;
            for (int i = 0; i < MAXFILES; i++) {
                if (fileEntries[i].getFilename().equals(filename)) {
                    entryIndex = i;
                    break;
                }
            }

            if (entryIndex == -1) {
                throw new IllegalArgumentException("file " + filename + " does not exist");
            }

            FEntry entry = fileEntries[entryIndex];

            // Calculate blocks needed
            int blocksNeeded = (int) Math.ceil((double) data.length / BLOCK_SIZE);

            // Count free blocks
            int freeBlocks = 0;
            for (int i = metadataBlocks; i < MAXBLOCKS; i++) {
                if (fileNodes[i].getBlockIndex() < 0) {
                    freeBlocks++;
                }
            }

            if (blocksNeeded > freeBlocks) {
                throw new Exception("file too large");
            }

            // Free old blocks if file had data
            if (entry.getFirstBlock() >= 0) {
                freeFileBlocks(entry.getFirstBlock());
            }

            // Allocate new blocks
            List<Integer> allocatedBlocks = new ArrayList<>();
            for (int i = metadataBlocks; i < MAXBLOCKS && allocatedBlocks.size() < blocksNeeded; i++) {
                if (fileNodes[i].getBlockIndex() < 0) {
                    allocatedBlocks.add(i);
                }
            }

            // Link blocks
            for (int i = 0; i < allocatedBlocks.size(); i++) {
                int blockIdx = allocatedBlocks.get(i);
                fileNodes[blockIdx].setBlockIndex(blockIdx); // Mark as used

                if (i < allocatedBlocks.size() - 1) {
                    fileNodes[blockIdx].setNext(allocatedBlocks.get(i + 1));
                } else {
                    fileNodes[blockIdx].setNext(-1); // Last block
                }
            }

            // Update file entry
            if (!allocatedBlocks.isEmpty()) {
                entry.setFirstBlock(allocatedBlocks.get(0).shortValue());
            }
            entry.setFilesize((short) data.length);

            // Write data to blocks
            int dataOffset = 0;
            for (int blockIdx : allocatedBlocks) {
                int bytesToWrite = Math.min(BLOCK_SIZE, data.length - dataOffset);
                byte[] blockData = new byte[BLOCK_SIZE];
                System.arraycopy(data, dataOffset, blockData, 0, bytesToWrite);

                disk.seek((long) blockIdx * BLOCK_SIZE);
                disk.write(blockData);

                dataOffset += bytesToWrite;
            }

            // Save metadata
            saveMetadata();

            return true;

        } catch (Exception e) {
            // Rollback on error - reload from disk
            loadFileSystem();
            throw e;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public String readFile(String filename) throws Exception {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        rwLock.readLock().lock();
        try {
            // Find file entry
            FEntry entry = null;
            for (int i = 0; i < MAXFILES; i++) {
                if (fileEntries[i].getFilename().equals(filename)) {
                    entry = fileEntries[i];
                    break;
                }
            }

            if (entry == null) {
                throw new IllegalArgumentException("file " + filename + " does not exist");
            }

            if (entry.getFilesize() == 0) {
                return "";
            }

            // Read data from blocks
            byte[] data = new byte[entry.getFilesize()];
            int dataOffset = 0;
            int currentBlock = entry.getFirstBlock();

            while (currentBlock >= 0 && dataOffset < entry.getFilesize()) {
                disk.seek((long) currentBlock * BLOCK_SIZE);
                int bytesToRead = Math.min(BLOCK_SIZE, entry.getFilesize() - dataOffset);
                disk.readFully(data, dataOffset, bytesToRead);

                dataOffset += bytesToRead;
                currentBlock = fileNodes[currentBlock].getNext();
            }

            return new String(data);

        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void deleteFile(String filename) throws Exception {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        rwLock.writeLock().lock();
        try {
            // Find file entry
            int entryIndex = -1;
            for (int i = 0; i < MAXFILES; i++) {
                if (fileEntries[i].getFilename().equals(filename)) {
                    entryIndex = i;
                    break;
                }
            }

            if (entryIndex == -1) {
                throw new IllegalArgumentException("file " + filename + " does not exist");
            }

            FEntry entry = fileEntries[entryIndex];

            // Free blocks and zero them out
            if (entry.getFirstBlock() >= 0) {
                int currentBlock = entry.getFirstBlock();
                byte[] zeros = new byte[BLOCK_SIZE];

                while (currentBlock >= 0) {
                    int nextBlock = fileNodes[currentBlock].getNext();

                    // Zero out the block
                    disk.seek((long) currentBlock * BLOCK_SIZE);
                    disk.write(zeros);

                    // Mark block as free
                    fileNodes[currentBlock].setBlockIndex(-currentBlock);
                    fileNodes[currentBlock].setNext(-1);

                    currentBlock = nextBlock;
                }
            }

            // Clear file entry
            fileEntries[entryIndex] = new FEntry("", (short) 0, (short) -1);

            // Save metadata
            saveMetadata();

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public String[] listFiles() throws Exception {
        rwLock.readLock().lock();
        try {
            List<String> files = new ArrayList<>();
            for (int i = 0; i < MAXFILES; i++) {
                if (!fileEntries[i].getFilename().isEmpty()) {
                    files.add(fileEntries[i].getFilename());
                }
            }
            return files.toArray(new String[0]);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private void freeFileBlocks(int firstBlock) throws IOException {
        int currentBlock = firstBlock;
        byte[] zeros = new byte[BLOCK_SIZE];

        while (currentBlock >= 0) {
            int nextBlock = fileNodes[currentBlock].getNext();

            // Zero out the block
            disk.seek((long) currentBlock * BLOCK_SIZE);
            disk.write(zeros);

            // Mark as free
            fileNodes[currentBlock].setBlockIndex(-currentBlock);
            fileNodes[currentBlock].setNext(-1);

            currentBlock = nextBlock;
        }
    }

    // Close the file system
    public void close() throws IOException {
        rwLock.writeLock().lock();
        try {
            if (disk != null) {
                disk.close();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
