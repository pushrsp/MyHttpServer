package me.project.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class JapressServer {

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private final int port;
    private final ByteBuffer preambleBuffer;

    private volatile boolean running = true;

    public static void run(int port) throws IOException {
        new JapressServer(port).run();
    }

    private JapressServer(int port) throws IOException {
        this.port = port;
        this.preambleBuffer = ByteBuffer.allocate(16 * 1024);

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
                    this.preambleBuffer.clear();
                }
            } catch (IOException e) {
                close(selectionKey);
            }
        }

        clean();
    }

    private void clean() {
        for (SelectionKey key : this.selector.keys()) {
            if(Objects.isNull(key.attachment())) {
                continue;
            }

            System.out.println("HI");
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
        client.configureBlocking(false);
        client.register(selectionKey.selector(), SelectionKey.OP_READ);
    }

    private void read(SelectionKey selectionKey) {

    }

    private void write(SelectionKey selectionKey) {

    }
}
