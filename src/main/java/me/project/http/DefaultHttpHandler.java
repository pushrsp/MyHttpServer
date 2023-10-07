package me.project.http;
public class DefaultHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request) {
        System.out.println("METHOD: " + request.getMethod());
        System.out.println("PATH: " + request.getPath());
        System.out.println("PROTOCOL: " + request.getProtocol());
    }
}
