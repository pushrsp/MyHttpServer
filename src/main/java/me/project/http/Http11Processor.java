package me.project.http;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Http11Processor implements HttpProcessor {

    private long lastUsed = System.currentTimeMillis();

    public void markUsed() {
        lastUsed = System.currentTimeMillis();
    }

    @Override
    public long lastUsed() {
        return 0;
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {

    }
}
