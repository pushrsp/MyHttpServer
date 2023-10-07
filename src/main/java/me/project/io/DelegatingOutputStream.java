package me.project.io;

import me.project.http.HTTPValues;
import me.project.http.HttpRequest;
import me.project.http.HttpResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import static me.project.http.HTTPValues.*;

public class DelegatingOutputStream extends OutputStream {

    private final HttpRequest request;

    private final HttpResponse response;

    private final OutputStream unCompressingOutputStream;

    private boolean compress;

    private OutputStream outputStream;

    private boolean used;

    public DelegatingOutputStream(HttpRequest request, HttpResponse response, OutputStream outputStream, boolean compressByDefault) {
        this.request = request;
        this.response = response;
        this.unCompressingOutputStream = outputStream;
        this.compress = compressByDefault;
        this.outputStream = outputStream;
    }

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        if (used) {
            throw new IllegalStateException("The HttpResponse compression configuration cannot be modified once bytes have been written to it.");
        }

        this.compress = compress;
    }

    public boolean willCompress() {
        if(this.compress) {
            for (String acceptEncoding : this.request.getAcceptEncodings()) {
                if(acceptEncoding.equalsIgnoreCase(ContentEncodings.Gzip)) {
                    return true;
                } else if(acceptEncoding.equalsIgnoreCase(ContentEncodings.Deflate)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void init() {
        // Initialize the actual OutputStream latent so that we can call setCompress more than once.
        // - The GZIPOutputStream writes bytes to the OutputStream during construction which means we cannot build it
        //   more than once. This is why we must wait until we know for certain we are going to write bytes to construct
        //   the compressing OutputStream.
        this.used = true;

        if(!this.compress) {
            return;
        }

        for (String acceptEncoding : this.request.getAcceptEncodings()) {
            if(acceptEncoding.equalsIgnoreCase(ContentEncodings.Gzip)) {
                try {
                    this.outputStream = new GZIPOutputStream(this.unCompressingOutputStream);
                    this.response.setHeader(Headers.ContentEncoding, ContentEncodings.Gzip);
                    return;
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else if(acceptEncoding.equalsIgnoreCase(ContentEncodings.Deflate)) {
                this.outputStream = new DeflaterOutputStream(this.unCompressingOutputStream);
                this.response.setHeader(Headers.ContentEncoding, ContentEncodings.Deflate);
                return;
            }
        }

        this.compress = false;
    }

    @Override
    public void close() throws IOException {
        this.outputStream.close();
    }

    @Override
    public void flush() throws IOException {
        this.outputStream.flush();
    }

    @Override
    public void write(int b) throws IOException {
        if (!used) {
            init();
        }

        this.outputStream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (!used) {
            init();
        }

        this.outputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (!used) {
            init();
        }

        this.outputStream.write(b, off, len);
    }
}
