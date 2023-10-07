package me.project.utils;

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
}
