package me.project.http;

import me.project.io.BlockingBufferOutputStream;
import me.project.utils.HttpUtils;

import java.nio.ByteBuffer;

import static me.project.http.HTTPValues.*;

public class HttpResponseProcessor {

    private final int maxHeadLength = 128 * 1024;

    private final BlockingBufferOutputStream outputStream;

    private final HttpRequest request;

    private final HttpResponse response;

    private ByteBuffer[] preambleBuffers;

    private volatile ResponseState state = ResponseState.Preamble;

    public HttpResponseProcessor(HttpRequest request, HttpResponse response, BlockingBufferOutputStream outputStream) {
        this.outputStream = outputStream;
        this.request = request;
        this.response = response;
    }

    public ResponseState state() {
        return this.state;
    }

    public void resetState(ResponseState state) {
        this.state = state;
    }

    public synchronized void failure() {
        // Go nuclear and wipe the response and stream, even if the response has already been committed (meaning one or more bytes have been
        // written)
        this.response.setStatus(500);
        this.response.setStatusMessage("Failure");
        this.response.clearHeaders();
        this.response.setContentLength(0L);

        this.preambleBuffers = null;

        this.outputStream.clear();
        this.outputStream.close();

        resetState(ResponseState.Preamble);
    }

    public synchronized ByteBuffer[] currentBuffer() {
        if (this.state == ResponseState.Preamble || this.state == ResponseState.Expect) {
            // We can't write the preamble under normal conditions if the worker thread is still working. Expect handling is different and the
            // client is waiting for a pre-canned response

            boolean remain = this.outputStream.hasReadableBuffer();
            boolean closed = this.outputStream.isClosed();

            if(this.state != ResponseState.Expect && !this.outputStream.hasReadableBuffer() && !this.outputStream.isClosed()) {
                return null;
            }

            // Construct the preamble if needed and return it if there is any bytes left
            if(this.preambleBuffers == null) {
                if(this.state == ResponseState.Preamble) {
                    fillHeaders();
                    this.preambleBuffers = new ByteBuffer[] { HttpUtils.buildResponsePreamble(this.response, this.maxHeadLength) };
                } else if (this.state == ResponseState.Expect) {
                    this.preambleBuffers = new ByteBuffer[] { HttpUtils.buildResponsePreamble(this.response, this.maxHeadLength) };
                }

                // TODO: Figure out the body processor
            }

            if(this.preambleBuffers[0].hasRemaining()) {
                return this.preambleBuffers;
            }

            // Reset the buffer in case we need to write another preamble (i.e. for expect)
            this.preambleBuffers = null;

            // If expect and preamble done, figure out stage to be Continue or Close
            if(this.state == ResponseState.Expect) {
                if(this.response.getStatus() == 100) {
                    this.state = ResponseState.Continue;
                } else {
                    this.state = ResponseState.Close;
                }
            } else {
                this.state = ResponseState.Body;
            }
        } else if(this.state == ResponseState.Body) {
            System.out.println("BODY");
        }

        return null;
    }

    private void fillHeaders() {
        // If the client wants the connection closed, force that in the response. This will force the code above to close the connection.
        // Otherwise, if the client asked for Keep-Alive and the server agrees, keep it. If the request asked for Keep-Alive, and the server
        // doesn't care, keep it. Otherwise, if the client and server both don't care, set to Keep-Alive.
        String requestConnection = this.request.getHeader(Headers.Connection);
        boolean requestKeepAlive = Connections.KeepAlive.equalsIgnoreCase(requestConnection);

        String responseConnection = this.response.getHeader(Headers.Connection);
        boolean responseKeepAlive = Connections.KeepAlive.equalsIgnoreCase(requestConnection);

        if(Connections.Close.equalsIgnoreCase(requestConnection)) {
            this.response.setHeader(Headers.Connection, Connections.Close);
        } else if((requestKeepAlive && responseKeepAlive) || (requestKeepAlive && responseConnection == null) || (requestConnection == null && responseConnection == null)) {
            this.response.setHeader(Headers.Connection, Connections.KeepAlive);
        }

        // Remove a Content-Length header when we know we will be compressing the response.
        if(this.response.getContentLength() != null && this.response.willCompress()) {
            this.response.removeHeader(Headers.ContentLength);
        }

        Long contentLength = response.getContentLength();
        if(contentLength == null && this.outputStream.isEmpty()) {
            this.response.setContentLength(0L);
        } else if(contentLength == null) {
            this.response.setHeader(Headers.TransferEncoding, TransferEncodings.Chunked);
        }

        for (Cookie cookie : this.response.getCookies()) {
            String value = cookie.toResponseHeader();
            this.response.setHeader(Headers.SetCookie, value);
        }
    }
}
