package me.project.http;

import me.project.io.BlockingBufferOutputStream;
import me.project.pool.JapressThreadPool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
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

    private Duration readThroughputCalculationDelayDuration = Duration.ofSeconds(5);

    private Duration writeThroughputCalculationDelayDuration = Duration.ofSeconds(5);

    private final int responseBufferSize = 16 * 1024;

    private final int maxOutputBufferQueueLength = 128;

    private boolean compressByDefault = true;

    public Http11Processor(HttpHandler rootHandler, JapressThreadPool threadPool, ByteBuffer preambleBuffer, int port) {
        this.rootHandler = rootHandler;
        this.threadPool = threadPool;
        this.preambleBuffer = preambleBuffer;
        this.state = ProcessorState.Read;

        this.request = new HttpRequest(port);
        this.requestProcessor = new HttpRequestProcessor(this.request);

        BlockingBufferOutputStream outputStream = new BlockingBufferOutputStream(this.responseBufferSize, this.maxOutputBufferQueueLength);

        this.response = new HttpResponse(outputStream, this.request, this.compressByDefault);
        this.responseProcessor = new HttpResponseProcessor(this.request, this.response, outputStream);
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

    @Override
    public ByteBuffer[] writeBuffers() {
        ResponseState responseState = this.responseProcessor.state();
        if(responseState == ResponseState.Expect || responseState == ResponseState.Preamble || responseState == ResponseState.Body) {
            return this.responseProcessor.currentBuffer();
        }

        return null;
    }

    @Override
    public ProcessorState wrote(long num) {
        markUsed();

        this.bytesWritten += num;
        if(this.bytesWritten > 0 && this.firstByteWroteInstant == -1) {
            this.firstByteWroteInstant = System.currentTimeMillis();
        }

        if(num > 0) {
            this.response.setCommitted(true);
        }

        // Determine the state transition based on the state of the response processor
        ResponseState responseState = this.responseProcessor.state();
        if(responseState == ResponseState.Continue) {
            // Flip back to reading and back to the preamble state, so we write the real response headers. Then start the worker thread and flip the ops
            this.requestProcessor.resetState(RequestState.Body);
            this.responseProcessor.resetState(ResponseState.Preamble);
            this.future = this.threadPool.submit(new HttpWorker(this.rootHandler, this, this.request, this.response));
            this.state = ProcessorState.Read;
        } else if(responseState == ResponseState.KeepAlive) {
            this.state = ProcessorState.Reset;
        } else if (responseState == ResponseState.Close) {
            this.state = ProcessorState.Close;
        }

        return this.state;
    }

    @Override
    public long readThroughput() {
        // Haven't read anything yet, or we read everything in the first read (instants are equal)
        if(this.firstByteReadInstant == -1 || this.bytesRead == 0 || lastByteReadInstant == firstByteReadInstant) {
            return Long.MAX_VALUE;
        }

        if (firstByteWroteInstant == -1) {
            long millis = System.currentTimeMillis() - firstByteReadInstant;
            if(millis < this.readThroughputCalculationDelayDuration.toMillis()) {
                return Long.MAX_VALUE;
            }

            double result = ((double) this.bytesRead / (double) millis) * 1_000;
            return Math.round(result);
        }

        double result = ((double) this.bytesRead / (double) (this.lastByteReadInstant - this.firstByteReadInstant)) * 1_000;
        return Math.round(result);
    }

    @Override
    public long writeThroughput() {
        // Haven't written anything yet or not enough time has passed to calculated throughput (2s)
        if(this.firstByteWroteInstant == -1 || this.bytesWritten == 0) {
            return Long.MAX_VALUE;
        }

        // Always use currentTime since this calculation is ongoing until the client reads all the bytes
        long millis = System.currentTimeMillis() - this.firstByteWroteInstant;
        if(millis < this.writeThroughputCalculationDelayDuration.toMillis()) {
            return Long.MAX_VALUE;
        }

        double result = ((double) this.bytesWritten / (double) millis) * 1_000;
        return Math.round(result);
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

        if(requestState == RequestState.Expect) {
            this.responseProcessor.resetState(ResponseState.Expect);
            this.state = ProcessorState.Write;
        } else if (requestState == RequestState.Complete) {
            this.state = ProcessorState.Write;
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
    public ProcessorState close(boolean endOfStream) {
        // Interrupt the thread if it is still running
        if(future != null) {
            future.cancel(true);
        }

        this.state = ProcessorState.Close;
        return this.state;
    }

    @Override
    public void failure(Throwable t) {
        if(this.response.isCommitted()) {
            this.state = ProcessorState.Close;
        } else {
            this.state = ProcessorState.Write;
            this.responseProcessor.failure();
        }
    }
}
