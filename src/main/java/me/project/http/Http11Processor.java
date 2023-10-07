package me.project.http;

import me.project.pool.JapressThreadPool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

public class Http11Processor implements HttpProcessor {

    private Future<?> future;

    private final JapressThreadPool threadPool;

    private final ByteBuffer preambleBuffer;

    private final HttpHandler rootHandler;

    private final HttpRequest request;

    private final HttpRequestProcessor requestProcessor;

    private final HttpResponse response;

    private final HttpResponseProcessor responseProcessor;

    private long lastUsed = System.currentTimeMillis();

    private long bytesRead;

    private long bytesWritten;

    private long firstByteReadInstant = -1;

    private long firstByteWroteInstant = -1;

    private long lastByteReadInstant = -1;

    private volatile ProcessorState state;

    public Http11Processor(HttpHandler rootHandler, JapressThreadPool threadPool, ByteBuffer preambleBuffer, int port) {
        this.rootHandler = rootHandler;
        this.threadPool = threadPool;
        this.preambleBuffer = preambleBuffer;
        this.state = ProcessorState.Read;

        this.request = new HttpRequest(port);
        this.requestProcessor = new HttpRequestProcessor(this.request);

        this.response = new HttpResponse();
        this.responseProcessor = new HttpResponseProcessor();
    }

    @Override
    public long lastUsed() {
        return this.lastUsed;
    }

    @Override
    public ProcessorState state() {
        return this.state;
    }

    @Override
    public ProcessorState cancel(boolean endOfStream) {
        if(this.future != null) {
            future.cancel(true);
        }

        this.state = ProcessorState.Close;

        return this.state;
    }

    private void markUsed() {
        this.lastUsed = System.currentTimeMillis();
    }

    @Override
    public ProcessorState read(ByteBuffer buffer) throws IOException {
        markUsed();

        this.bytesRead += buffer.remaining();
        if(this.bytesRead > 0) {
            if(this.firstByteReadInstant == -1) {
                this.lastByteReadInstant = this.firstByteReadInstant = System.currentTimeMillis();
            } else {
                this.lastByteReadInstant = System.currentTimeMillis();
            }
        }

        RequestState requestState = this.requestProcessor.state();
        if(requestState == RequestState.Preamble) {
            requestState = this.requestProcessor.processPreambleBytes(buffer);

            // If the next state is not preamble, that means we are done processing that and ready to handle the request in a separate thread
            if(requestState != RequestState.Preamble && requestState != RequestState.Expect) {
                this.future = this.threadPool.submit(new HttpWorker(this.rootHandler, this, this.request, this.response));
            }
        } else {
            requestState = this.requestProcessor.processBodyBytes();
        }

        return this.state;
    }

    @Override
    public ByteBuffer readBuffer() throws IOException {
        markUsed();

        RequestState state = requestProcessor.state();
        ByteBuffer byteBuffer;

        switch (state) {
            case Preamble:
                byteBuffer = this.preambleBuffer;
                break;
            case Body:
                byteBuffer = this.requestProcessor.bodyBuffer();
                break;
            default:
                byteBuffer = null;
                break;
        }

        return byteBuffer;
    }

    @Override
    public ProcessorState close(boolean endOfStream) throws IOException {
        // Interrupt the thread if it is still running
        if(future != null) {
            future.cancel(true);
        }

        this.state = ProcessorState.Close;
        return this.state;
    }
}
