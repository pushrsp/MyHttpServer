package me.project.http;

import java.io.IOException;
import java.io.OutputStream;

import static me.project.http.HTTPValues.*;

public class DefaultHttpHandler implements HttpHandler {

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        System.out.println("PATH: " + request.getPath());
        System.out.println("PROTOCOL: " + request.getProtocol());

        response.setHeader(Headers.ContentType, "text/plain");
//        response.setHeader("Content-Length", "16");
        response.setStatus(200);

        try {
            response.getOutputStream().close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
