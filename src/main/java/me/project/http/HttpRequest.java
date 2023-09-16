package me.project.http;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

    public HttpRequest(String contextPath, int multipartBufferSize, String scheme, int port, String ipAddress) {
        Objects.requireNonNull(contextPath);
        Objects.requireNonNull(scheme);
        this.contextPath = contextPath;
        this.multipartBufferSize = multipartBufferSize;
        this.scheme = scheme;
        this.port = port;
        this.ipAddress = ipAddress;
    }

    public void addAcceptEncoding(String encoding) {
        this.acceptEncodings.add(encoding);
    }

    public void addAcceptEncodings(List<String> encodings) {
        this.acceptEncodings.addAll(encodings);
    }
}
