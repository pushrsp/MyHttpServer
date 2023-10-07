package me.project.http;

import java.nio.ByteBuffer;

public class HttpRequestProcessor {

    private final StringBuilder sb = new StringBuilder();

    private final HttpRequest httpRequest;

    private String headerName;

    private RequestState state = RequestState.Preamble;

    private RequestPreambleState preambleState = RequestPreambleState.REQUEST_METHOD;

    public HttpRequestProcessor(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public RequestState state() {
        return this.state;
    }

    public RequestState processPreambleBytes(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            byte ch = buffer.get();

            RequestPreambleState nextState = this.preambleState.next(ch);
            if(nextState != preambleState) {
                switch (this.preambleState) {
                    case REQUEST_METHOD:
                        this.httpRequest.setMethod(HttpMethod.of(this.sb.toString()));
                        break;
                    case REQUEST_PATH:
                        this.httpRequest.setPath(this.sb.toString());
                        break;
                    case REQUEST_PROTOCOL:
                        this.httpRequest.setProtocol(this.sb.toString());
                        break;
                    case HEADER_NAME:
                        this.headerName = sb.toString();
                        break;
                    case HEADER_VALUE:
                        this.httpRequest.addHeader(this.headerName, sb.toString());
                        break;
                }

                if(nextState.store()) {
                    this.sb.delete(0, this.sb.length());
                    this.sb.appendCodePoint(ch);
                }
            } else if(nextState.store()) {
                this.sb.appendCodePoint(ch);
            }

            this.preambleState = nextState;
            if(this.preambleState == RequestPreambleState.COMPLETE) {
                // Determine if there is a body and if we should handle it. Even if we are in an expect request, the body will be coming, so we need
                // to prepare for it here
                Long contentLength = this.httpRequest.getContentLength();
                if((contentLength != null && contentLength > 0) || this.httpRequest.isChunked()) {
                    this.state = RequestState.Body;
                } else {
                    this.state = RequestState.Complete;
                }

                // If we are expecting, set the state and bail
            }
        }

        return this.state;
    }

    public RequestState processBodyBytes() {
        return RequestState.Body;
    }

    public ByteBuffer bodyBuffer() {
        return null;
    }
}
