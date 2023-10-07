package me.project.exception.io;

import java.io.IOException;

public class ClientAbortException extends IOException {

    public ClientAbortException(IOException e) {
        super(e);
    }
}
