package me.project;

import org.junit.jupiter.api.Assertions;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

public abstract class BaseTest {

    public void send(String message, int port) {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();

            os.write(message.getBytes());
            os.flush();

            byte[] buffer = is.readAllBytes();
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
