package me.project.http;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class Cookie {

    public String domain;

    public ZonedDateTime expires;

    public boolean httpOnly;

    public Long maxAge;

    public String name;

    public String path;

    public SameSite sameSite;

    public boolean secure;

    public String value;

    public Cookie() {
    }

    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Cookie(Cookie other) {
        if (other == null) {
            return;
        }

        this.domain = other.domain;
        this.expires = other.expires;
        this.httpOnly = other.httpOnly;
        this.maxAge = other.maxAge;
        this.name = other.name;
        this.path = other.path;
        this.sameSite = other.sameSite;
        this.secure = other.secure;
        this.value = other.value;
    }

    public static List<Cookie> fromRequestHeader(String header) {
        List<Cookie> cookies = new ArrayList<>();

        return cookies;
    }

    public enum SameSite {
        Lax,
        None,
        Strict
    }
}
