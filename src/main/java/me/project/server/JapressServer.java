package me.project.server;

import me.project.exception.io.ClientAbortException;
import me.project.http.*;
import me.project.pool.JapressThreadPool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.time.Duration;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class JapressServer {

    private final long minimumReadThroughput = 16 * 1024;

    private long minimumWriteThroughput = 16 * 1024;

    private final Duration clientTimeout =  Duration.ofSeconds(20);

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private final int port;
    private final ByteBuffer preambleBuffer;
    private final JapressThreadPool threadPool;
    private final HttpHandler rootHandler;

    private volatile boolean running = true;

    public static void run(int port, int numOfThreads) throws IOException {
        new JapressServer(port, numOfThreads).run();
    }

    private JapressServer(int port, int numOfThreads) throws IOException {
        this.rootHandler = new DefaultHttpHandler();
        this.port = port;
        this.preambleBuffer = ByteBuffer.allocate(16 * 1024);
        this.threadPool = new JapressThreadPool(numOfThreads, "HTTP Server Worker Thread", Duration.ofSeconds(10));

        openSelector();
        openChannel();
    }

    private void openSelector() throws IOException {
        this.selector = Selector.open();
    }

    private void openChannel() throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.configureBlocking(false);
        this.serverSocketChannel.bind(new InetSocketAddress(this.port));
        this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
    }

    private void run() {
        while (this.running) {
            SelectionKey selectionKey = null;
            try {
                this.selector.select(300L);

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    selectionKey = iterator.next();

                    if(selectionKey.isAcceptable()) {
                        accept(selectionKey);
                    } else if(selectionKey.isReadable()) {
                        read(selectionKey);
                    } else if(selectionKey.isWritable()) {
                        write(selectionKey);
                    }

                    iterator.remove();
                    selectionKey = null;
                    this.preambleBuffer.clear();
                }

                clean();
            } catch (IOException e) {
                close(selectionKey);
            }
        }
    }

    private void clean() {
        long now = System.currentTimeMillis();
        for (SelectionKey key : this.selector.keys()) {
            if(Objects.isNull(key.attachment())) {
                continue;
            }

            HttpProcessor processor = (HttpProcessor) key.attachment();
            ProcessorState state = processor.state();
            boolean readingSlow = state == ProcessorState.Read && processor.readThroughput() < this.minimumReadThroughput;
            boolean writingSlow = state == ProcessorState.Write && processor.writeThroughput() < this.minimumWriteThroughput;
            boolean timeOut = processor.lastUsed() < now - this.clientTimeout.toMillis();
            boolean badChannel = readingSlow || writingSlow || timeOut;

            if (!badChannel) {
                continue;
            }

            cancelAndCloseKey(key);
        }
    }

    private void close(SelectionKey selectionKey) {
        if(Objects.isNull(selectionKey)) {
            return;
        }

        if(Objects.isNull(selectionKey.channel())) {
            return;
        }

        // TODO
        Object attachment = selectionKey.attachment();

        selectionKey.cancel();
    }

    private void accept(SelectionKey selectionKey) throws IOException {
        SocketChannel client = this.serverSocketChannel.accept();
        Http11Processor http11Processor = new Http11Processor(this.rootHandler, this.threadPool, this.preambleBuffer, this.port);
        client.configureBlocking(false);
        client.register(selectionKey.selector(), SelectionKey.OP_READ, http11Processor);
    }

    private void read(SelectionKey selectionKey) throws IOException {
        HttpProcessor httpProcessor = (HttpProcessor) selectionKey.attachment();
        SocketChannel client = (SocketChannel) selectionKey.channel();
        ProcessorState state = httpProcessor.state();

        if(state == ProcessorState.Read) {
            ByteBuffer buffer = httpProcessor.readBuffer();
            if(buffer != null) {
                int size;
                try {
                    size = client.read(buffer);
                } catch (IOException ex) {
                    //FIXME
                    throw ex;
                }

                System.out.println("size: " + size);

                if(size < 0) {
                    state = httpProcessor.close(true);
                } else {
                    buffer.flip();
                    state = httpProcessor.read(buffer);
                }
            }
        }

        if(state == ProcessorState.Close) {
            close(selectionKey);
        } else {
            selectionKey.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void write(SelectionKey selectionKey) throws IOException {
        Http11Processor processor = (Http11Processor) selectionKey.attachment();
        ProcessorState state = processor.state();
        SocketChannel client = (SocketChannel) selectionKey.channel();
        ByteBuffer[] buffers = processor.writeBuffers();
        if(state == ProcessorState.Write) {
            long size = 0;
            if(buffers != null) {
                try {
                    size = client.write(buffers);
                } catch (IOException ex) {
                    throw new ClientAbortException(ex);
                }
            }

            if(size < 0) {
                state = processor.close(true);
            } else {
                // Always call wrote to update the state even if zero bytes were written
                state = processor.wrote(size);
            }
        }

        // If the key is done, cancel and close it out. Otherwise, turn it around for KeepAlive handling to start reading the next request
        if(state == ProcessorState.Close) {
            cancelAndCloseKey(selectionKey);
        } else if(state == ProcessorState.Read) {
            selectionKey.interestOps(SelectionKey.OP_READ);
        } else if(state == ProcessorState.Reset) {
            // TODO: reset processor
            Http11Processor http11Processor = new Http11Processor(this.rootHandler, this.threadPool, this.preambleBuffer, this.port);
            selectionKey.interestOps(SelectionKey.OP_READ);
        }
    }

    private void cancelAndCloseKey(SelectionKey selectionKey) {
        if(selectionKey != null) {
            if(selectionKey.attachment() != null) {
                ((HttpProcessor) selectionKey.attachment()).close(false);
            }

            selectionKey.cancel();
        }
    }
}
