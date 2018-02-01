package knf.kuma.videoservers;

/**
 * Created by Jordy on 11/01/2018.
 */

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
