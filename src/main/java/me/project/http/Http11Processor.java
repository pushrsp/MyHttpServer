package me.project.http;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Http11Processor implements HttpProcessor {

    private final ByteBuffer preambleBuffer;

    private long lastUsed = System.currentTimeMillis();

    private volatile ProcessorState state;

    public Http11Processor(ByteBuffer preambleBuffer) {
        this.preambleBuffer = preambleBuffer;
        this.state = ProcessorState.Read;
    }

    public void markUsed(ByteBuffer preambleBuffer) {
        lastUsed = System.currentTimeMillis();
    }

    @Override
    public long lastUsed() {
        return this.lastUsed;
    }

    @Override
    public ProcessorState state() {
        return this.state;
    }

    private void markUsed() {
        this.lastUsed = System.currentTimeMillis();
    }

    @Override
    public ProcessorState read(ByteBuffer buffer) throws IOException {
        markUsed();

        return this.state;
    }

    @Override
    public ByteBuffer readBuffer() throws IOException {
        markUsed();

        ByteBuffer byteBuffer = this.preambleBuffer;
        return byteBuffer;
    }
}