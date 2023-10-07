package me.project.http;

public class HttpWorker implements Runnable {

    private final HttpHandler httpHandler;

    private final HttpProcessor httpProcessor;

    private final HttpRequest request;
    private final HttpResponse response;

    public HttpWorker(HttpHandler httpHandler, HttpProcessor httpProcessor, HttpRequest request, HttpResponse response) {
        this.httpHandler = httpHandler;
        this.httpProcessor = httpProcessor;
        this.request = request;
        this.response = response;
    }

    @Override
    public void run() {
        try {
            this.httpHandler.handle(this.request);
        } catch (Throwable t) {
            //FIXME
            t.printStackTrace();
        }
    }
}
