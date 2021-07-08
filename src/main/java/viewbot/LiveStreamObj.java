package viewbot;

public class LiveStreamObj {
    private final String token;
    private final String sig;

    public LiveStreamObj(String token, String sig) {
        this.token = token;
        this.sig = sig;
    }

    public String getToken() {
        return token;
    }

    public String getSig() {
        return sig;
    }
}
