package me.project.http;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface HttpProcessor {

    long lastUsed();

    ProcessorState state();

    ProcessorState cancel(boolean endOfStream);

    ProcessorState read(ByteBuffer buffer) throws IOException;

    ByteBuffer readBuffer() throws IOException;
}
