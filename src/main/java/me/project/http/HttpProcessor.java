package me.project.http;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface HttpProcessor {

    long lastUsed();

    void read(ByteBuffer buffer) throws IOException;
}
