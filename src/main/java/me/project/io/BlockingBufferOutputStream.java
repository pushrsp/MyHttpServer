package me.project.io;

import me.project.exception.io.ConnectionClosedException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockingBufferOutputStream extends OutputStream {

    private final int bufferSize;

    private final BlockingQueue<ByteBuffer> buffers;

    private volatile boolean used;

    private volatile boolean closed;

    private ByteBuffer currentBuffer;

    public BlockingBufferOutputStream(int bufferSize, int maximumQueueSize) {
        this.buffers = new LinkedBlockingQueue<>(maximumQueueSize);
        this.bufferSize = bufferSize;
    }

    public boolean hasReadableBuffer() {
        return this.buffers.peek() != null;
    }

    public boolean isClosed() {
        return this.buffers.isEmpty() && this.closed;
    }

    public boolean isEmpty() {
        return !this.used;
    }

    public ByteBuffer readableBuffer() {
        return this.buffers.poll();
    }

    @Override
    public void close() {
        // DO NOT TOUCH THIS ORDER!
        if(this.currentBuffer != null) {
            addBuffers(true);
        }

        this.closed = true;

        // TODO: notify
    }

    @Override
    public void write(int b) throws IOException {
        if(closed) {
            throw new IllegalStateException("It is already closed");
        }

        this.used = true;
        initBuffer(this.bufferSize);
        this.currentBuffer.put((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if(closed) {
            throw new IllegalStateException("It is already closed");
        }

        this.used = true;
        initBuffer(this.bufferSize);

        int length = Math.min(this.currentBuffer.remaining(), len);
        this.currentBuffer.put(b, off, length);

        if(length < len) {
            addBuffers(true);

            int newLength = Math.max(this.bufferSize, len - length);
            this.currentBuffer = ByteBuffer.allocate(newLength);
            this.currentBuffer.put(b, off + length, len - length);

            if(!this.currentBuffer.hasRemaining()) {
                addBuffers(true);
            }
        }
    }

    /**
     * Flushes the current stream contents if the current ByteBuffer has less than 10% remaining space. It flushes by putting the current
     * ByteBuffer into the Queue that the reader thread is reading from. Then it sets the current ByteBuffer to null so that a new one is
     * created. And finally, this notifies the selector to wake up.
     */
    public void flush() {
        if(this.currentBuffer != null && this.currentBuffer.remaining() < (currentBuffer.capacity() / 10)) {
            addBuffers(true);
        }
    }

    public void clear() {
        this.currentBuffer = null;
        this.buffers.clear();
    }

    private void addBuffers(boolean notify) {
        this.currentBuffer.flip();

        try {
            this.buffers.put(this.currentBuffer);
        } catch (InterruptedException ex) {
            this.currentBuffer = null;
            this.buffers.clear();
            throw new ConnectionClosedException(ex);
        }

        this.currentBuffer = null;
    }

    private void initBuffer(int length) {
        if(this.currentBuffer == null) {
            this.currentBuffer = ByteBuffer.allocate(length);
        } else if(!this.currentBuffer.hasRemaining()) {
            addBuffers(true);
            this.currentBuffer = ByteBuffer.allocate(length);
        }
    }
}
