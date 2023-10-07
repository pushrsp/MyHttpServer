package me.project.http;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static me.project.http.HTTPValues.*;

public class HttpRequest {

    private final List<String> acceptEncodings = new LinkedList<>();

    private final Map<String, Object> attributes = new HashMap<>();

    private final Map<String, Cookie> cookies = new HashMap<>();

    private final List<FileInfo> files = new LinkedList<>();

    private final Map<String, List<String>> headers = new HashMap<>();

    private final List<Locale> locales = new LinkedList<>();

    private final int multipartBufferSize;

    private final Map<String, List<String>> urlParameters = new HashMap<>();

    private byte[] bodyBytes;

    private Map<String, List<String>> combinedParameters;

    private Long contentLength;

    private String contentType;

    private String contextPath;

    private Charset encoding = StandardCharsets.UTF_8;

    private Map<String, List<String>> formData;

    private String host;

    private InputStream inputStream;

    private String ipAddress;

    private HttpMethod method;

    private boolean multipart;

    private String multipartBoundary;

    private String path = "/";

    private int port = -1;

    private String protocol;

    private String queryString;

    private String scheme;

    public HttpRequest() {
        this.contextPath = "";
        this.multipartBufferSize = 1024;
    }

    public HttpRequest(int port) {
        this.contextPath = "";
        this.multipartBufferSize = 1024;
        this.port = port;
    }

    public HttpRequest(String contextPath, int multipartBufferSize, String scheme, int port, String ipAddress) {
        Objects.requireNonNull(contextPath);
        Objects.requireNonNull(scheme);
        this.contextPath = contextPath;
        this.multipartBufferSize = multipartBufferSize;
        this.scheme = scheme;
        this.port = port;
        this.ipAddress = ipAddress;
    }

    public List<String> getAcceptEncodings() {
        return this.acceptEncodings;
    }

    public Long getContentLength() {
        return contentLength;
    }

    public void addAcceptEncoding(String encoding) {
        this.acceptEncodings.add(encoding);
    }

    public void addAcceptEncodings(List<String> encodings) {
        this.acceptEncodings.addAll(encodings);
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public HttpMethod getMethod() {
        return this.method;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public String getHeader(String name) {
        List<String> values = getHeaders(name);
        return values != null && values.size() > 0 ? values.get(0) : null;
    }

    public List<String> getHeaders(String name) {
        return headers.get(name.toLowerCase());
    }

    public void addHeader(String name, String value) {
        name = name.toLowerCase();
        this.headers.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
        decodeHeader(name, value);
    }

    private void decodeHeader(String name, String value) {
        //TODO
    }

    public boolean isChunked() {
        return getTransferEncoding() != null && getTransferEncoding().equalsIgnoreCase(TransferEncodings.Chunked);
    }

    public String getTransferEncoding() {
        return getHeader(Headers.TransferEncoding);
    }
}
