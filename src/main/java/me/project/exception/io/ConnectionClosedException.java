package me.project.exception.io;

public class ConnectionClosedException extends RuntimeException {

    public ConnectionClosedException(Throwable cause) {
        super(cause);
    }
}
