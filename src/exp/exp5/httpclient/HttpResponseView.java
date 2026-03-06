package exp.exp5.httpclient;

public class HttpResponseView {
    private final String raw;
    private final String headers;
    private final String body;

    public HttpResponseView(String raw, String headers, String body) {
        this.raw = raw;
        this.headers = headers;
        this.body = body;
    }

    public String getRaw() {
        return raw;
    }

    public String getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public static HttpResponseView parse(String raw) {
        String separator = "\r\n\r\n";
        int idx = raw.indexOf(separator);
        if (idx < 0) {
            separator = "\n\n";
            idx = raw.indexOf(separator);
        }
        if (idx < 0) {
            return new HttpResponseView(raw, raw, "");
        }
        String headers = raw.substring(0, idx);
        String body = raw.substring(idx + separator.length());
        return new HttpResponseView(raw, headers, body);
    }
}
