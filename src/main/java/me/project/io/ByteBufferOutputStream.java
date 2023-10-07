package me.project.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteBufferOutputStream extends OutputStream {

    private final int initSize;

    private final int maxCapacity;

    private byte[] buf;

    private int index;

    public ByteBufferOutputStream() {
        this(64, 1024 * 1024);
    }

    public ByteBufferOutputStream(int initSize, int maxCapacity) {
        this.initSize = initSize;
        this.buf = new byte[initSize];
        this.maxCapacity = maxCapacity;
    }

    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(buf, 0, index);
    }

    @Override
    public void flush() { }

    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        ensureSize(index + len);
        System.arraycopy(b, 0, buf, index, len);
        index += len;
    }

    @Override
    public void write(int b) {
        ensureSize(index + 1);
        buf[index] = (byte) b;
        index++;
    }

    private void ensureSize(int minCapacity) {
        int oldCapacity = buf.length;
        int minGrowth = minCapacity - oldCapacity;
        if (minGrowth > 0) {
            if (minCapacity > maxCapacity) {
                throw new IllegalArgumentException("Unable to increase the buffer for ByteBufferOutputStream beyond max of [" + maxCapacity + "]");
            }

            buf = Arrays.copyOf(buf, Math.max(minCapacity, this.initSize));
        }
    }
}
