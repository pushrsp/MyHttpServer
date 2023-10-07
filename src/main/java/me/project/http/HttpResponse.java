package me.project.http;

import me.project.io.DelegatingOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static me.project.http.HTTPValues.*;

public class HttpResponse {

    private final Map<String, Map<String, Cookie>> cookies = new HashMap<>(); // <Path, <Name, Cookie>>

    private final Map<String, List<String>> headers = new HashMap<>();

    private final DelegatingOutputStream outputStream;

    private volatile boolean committed;

    private Writer writer;

    private int status = 200;

    private String statusMessage;

    public HttpResponse(OutputStream outputStream, HttpRequest request, boolean compressByDefault) {
        this.outputStream = new DelegatingOutputStream(request, this, outputStream, compressByDefault);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    public String getHeader(String name) {
        String key = name.toLowerCase();
        return headers.containsKey(key) && headers.get(key).size() > 0 ? headers.get(key).get(0) : null;
    }

    public boolean isCommitted() {
        return committed;
    }

    public void setCommitted(boolean committed) {
        this.committed = committed;
    }

    public Map<String, List<String>> getHeadersMap() {
        return headers;
    }

    public void setHeader(String name, String value) {
        if (name == null || value == null) {
            return;
        }

        this.headers.put(name.toLowerCase(), new ArrayList<>(List.of(value)));
    }

    public void removeHeader(String name) {
        if (name != null) {
            headers.remove(name.toLowerCase());
        }
    }

    public Long getContentLength() {
        if (containsHeader(Headers.ContentLength)) {
            return Long.parseLong(getHeader(Headers.ContentLength));
        }

        return null;
    }

    public void setContentLength(long length) {
        setHeader(Headers.ContentLength, Long.toString(length));
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public List<Cookie> getCookies() {
        return this.cookies.values()
                .stream()
                .flatMap(map -> map.values().stream())
                .collect(Collectors.toList());
    }

    public boolean willCompress() {
        return this.outputStream.willCompress();
    }

    public void close() throws IOException {
        if(writer != null) {
            this.writer.close();
        } else {
            this.outputStream.close();
        }
    }

    public boolean containsHeader(String name) {
        String key = name.toLowerCase();
        return headers.containsKey(key) && headers.get(key).size() > 0;
    }

    public void clearHeaders() {
        headers.clear();
    }
}
