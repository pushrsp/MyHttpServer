package me.project;

import me.project.server.JapressServer;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        JapressServer.run(8080, 10);
    }
}
