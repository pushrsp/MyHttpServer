package me.project.utils;

import me.project.http.HTTPValues;
import me.project.http.HttpResponse;
import me.project.io.ByteBufferOutputStream;

import java.nio.ByteBuffer;

import static me.project.http.HTTPValues.*;

public class HttpUtils {

    public static boolean isTokenCharacter(byte ch) {
        return ch == '!' || ch == '#' || ch == '$' || ch == '%' || ch == '&' || ch == '\'' || ch == '*' || ch == '+' || ch == '-' || ch == '.' ||
                ch == '^' || ch == '_' || ch == '`' || ch == '|' || ch == '~' || (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') ||
                (ch >= '0' && ch <= '9');
    }

    public static boolean isURICharacter(byte ch) {
        // TODO : Fully implement RFC 3986 to accurate parsing
        return ch >= '!' && ch <= '~';
    }

    public static boolean isValueCharacter(byte ch) {
        return isURICharacter(ch) || ch == ' ' || ch == '\t' || ch == '\n';
    }

    public static ByteBuffer buildResponsePreamble(HttpResponse response, int maxLength) {
        ByteBufferOutputStream os = new ByteBufferOutputStream(1024, maxLength);
        writeStatusLine(response, os);

        response.getHeadersMap().forEach((key, values) ->
            values.forEach(value -> {
                os.write(key.getBytes());
                os.write(':');
                os.write(' ');
                os.write(value.getBytes());
                os.write(ControlBytes.CRLF);
            })
        );

        os.write(ControlBytes.CRLF);

        return os.toByteBuffer();
    }

    private static void writeStatusLine(HttpResponse response, ByteBufferOutputStream os) {
        os.write(ProtocolBytes.HTTTP1_1);
        os.write(' ');
        os.write(String.valueOf(response.getStatus()).getBytes());
        os.write(' ');

        if(response.getStatusMessage() != null) {
            os.write(response.getStatusMessage().getBytes());
        }

        os.write(ControlBytes.CRLF);
    }
}
