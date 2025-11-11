package ca.concordia.filesystem.datastructures;

public class FNode {

    private int blockIndex;
    private int next;

    public FNode(int blockIndex, int next) {
        this.blockIndex = blockIndex;
        this.next = next;
    }

    // Optional convenience constructor
    public FNode(int blockIndex) {
        this(blockIndex, -1);
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }

    public int getNext() {
        return next;
    }

    public void setNext(int next) {
        this.next = next;
    }
}
