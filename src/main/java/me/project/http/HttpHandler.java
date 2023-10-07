package me.project.http;

import java.nio.channels.SelectionKey;

public interface HttpHandler {

    void handle(HttpRequest request);
}
