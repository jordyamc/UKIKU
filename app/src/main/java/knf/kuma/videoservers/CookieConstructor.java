package knf.kuma.videoservers;

public class CookieConstructor {
    private String cookie;
    private String useAgent;
    private String referer;

    public CookieConstructor(String cookie, String useAgent, String referer) {
        this.cookie = cookie;
        this.useAgent = useAgent;
        this.referer = referer;
    }

    public CookieConstructor(String cookie) {
        this.cookie = cookie;
    }

    public String getCookie() {
        return cookie;
    }

    public String getUseAgent() {
        return useAgent;
    }

    public String getReferer() {
        return referer;
    }
}
