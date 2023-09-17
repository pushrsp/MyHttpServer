package me.project.server;

import me.project.http.Http11Processor;
import me.project.http.HttpProcessor;
import me.project.http.ProcessorState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class JapressServer {

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private final int port;
    private final ByteBuffer readBuffer;

    private volatile boolean running = true;

    public static void run(int port) throws IOException {
        new JapressServer(port).run();
    }

    private JapressServer(int port) throws IOException {
        this.port = port;
        this.readBuffer = ByteBuffer.allocate(16 * 1024);

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
                    this.readBuffer.clear();
                }

                clean();
            } catch (IOException e) {
                close(selectionKey);
            }
        }
    }

    private void clean() {
        for (SelectionKey key : this.selector.keys()) {
            if(Objects.isNull(key.attachment())) {
                continue;
            }

//            key.cancel();
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
        Http11Processor http11Processor = new Http11Processor(this.readBuffer);
        client.configureBlocking(false);
        client.register(selectionKey.selector(), SelectionKey.OP_READ, http11Processor);
    }

    private void read(SelectionKey selectionKey) throws IOException {
        HttpProcessor httpProcessor = (HttpProcessor) selectionKey.attachment();
        SocketChannel client = (SocketChannel) selectionKey.channel();
        ProcessorState state = httpProcessor.state();

        if(state == ProcessorState.Read) {
            ByteBuffer buffer = httpProcessor.readBuffer();
            int size = client.read(buffer);
            System.out.println("read: " + size);

            if(size < 0) {
                state = httpProcessor.cancel(true);
            } else {
                buffer.flip();
                httpProcessor.read(buffer);
            }
        }

        if(state == ProcessorState.Close) {
            close(selectionKey);
        } else {
            //TODO
        }
    }

    private void write(SelectionKey selectionKey) {

    }

    private void cancel(SelectionKey selectionKey) {
        if(selectionKey != null) {
            selectionKey.cancel();
        }
    }
}
