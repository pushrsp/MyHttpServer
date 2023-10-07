package me.project.http;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface HttpProcessor {

    long lastUsed();

    ProcessorState state();

    ProcessorState cancel(boolean endOfStream);

    ProcessorState read(ByteBuffer buffer) throws IOException;

    ByteBuffer readBuffer() throws IOException;

    ByteBuffer[] writeBuffers() throws IOException;

    ProcessorState wrote(long num) throws IOException;

    long readThroughput();
    long writeThroughput();

    ProcessorState close(boolean endOfStream);

    void failure(Throwable t);
}
