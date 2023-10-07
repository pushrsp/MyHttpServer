package me.project.http;

import me.project.http.HTTPValues.Methods;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class HttpMethod {

     public static final HttpMethod CONNECT = new HttpMethod(Methods.CONNECT);

     public static final HttpMethod DELETE = new HttpMethod(Methods.DELETE);

     public static final HttpMethod GET = new HttpMethod(Methods.GET);

     public static final HttpMethod HEAD = new HttpMethod(Methods.HEAD);

     public static final HttpMethod OPTIONS = new HttpMethod(Methods.OPTIONS);

     public static final HttpMethod PATCH = new HttpMethod(Methods.PATCH);

     public static final HttpMethod POST = new HttpMethod(Methods.POST);

     public static final HttpMethod PUT = new HttpMethod(Methods.PUT);

     public static final HttpMethod TRACE = new HttpMethod(Methods.TRACE);

     public static Map<String, HttpMethod> StandardMethods = new HashMap<>();

     private final String name;

     private HttpMethod(String name) {
          Objects.requireNonNull(name);
          this.name = name.toUpperCase(Locale.ROOT);
     }

     public static HttpMethod of(String name) {
          name = name.toUpperCase(Locale.ROOT);
          HttpMethod method = StandardMethods.get(name);
          if (method == null) {
               method = new HttpMethod(name);
          }

          return method;
     }
}
