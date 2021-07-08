package viewbot;

import config.Config;
import controller.ControllerMain;
import javafx.application.Platform;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import service.TwitchUtil;
import utils.HttpClient;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static service.TwitchUtil.CLIENT_ID;


public class ViewBot {

    static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36";
    static final String ACCEPT_VIDEO = "application/x-mpegURL, application/vnd.apple.mpegurl, application/json, text/plain";
    static final String GET_VIDEO = "https://usher.ttvnw.net/api/channel/hls/%s.m3u8?" +
            "allow_source=true&baking_bread=true&baking_brownies=true&baking_brownies_timeout=1050&fast_bread=true&p=3168255&player_backend=mediaplayer&" +
            "playlist_include_framerate=true&reassignments_supported=false&rtqos=business_logic_reverse&cdm=wv&sig=%s&token=%s";
    static final String ACCEPT_INFO = "application/vnd.twitchtv.v5+json; charset=UTF-8";
    static final String GET_INFO = "https://api.twitch.tv/api/channels/" +
            "%s/access_token?need_https=true&oauth_token=&" +
            "platform=web&player_backend=mediaplayer&player_type=site&client_id=%s";
    static final String ACCEPT_LANG = "en-us";
    static final String CONTENT_INFO = "application/json; charset=UTF-8";
    static final String REFERER = "https://www.twitch.tv/";


    private ScheduledExecutorService threadPool;
    private long requestDelay;
    private LinkedBlockingQueue<String> proxyQueue;
    private String target;
    private final ControllerMain controllerMain;
    private int threads;

    private Thread waitingThread;

    public ViewBot(ControllerMain controllerMain, LinkedBlockingQueue<String> proxyQueue, String target) {
        this.controllerMain = controllerMain;
        this.proxyQueue = proxyQueue;
        this.target = target;
    }

    void writeToLog(String msg) {
        Platform.runLater(() ->
                controllerMain.writeToLog(msg)
        );
    }

    public void prepareToStart() {
        if (Config.startWhenLiveValue) {
            try {
                String channelId = TwitchUtil.getChannelId(target);
                Runnable waitingRunnable = getWaitingRunnable(channelId);
                waitingThread = new Thread(waitingRunnable);
                waitingThread.start();
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                writeToLog("Failed to get channel status");
            }
        }
    }

    private Runnable getWaitingRunnable(String channelId) {
        return () -> {
            synchronized (this) {
                while (true) {
                    try {
                        if (Thread.currentThread().isInterrupted()) break;
                        writeToLog("Waiting when channel goes live");
                        if (TwitchUtil.isChannelLive(channelId)) break;
                    } catch (IOException e) {
                        writeToLog("Can't get channel status");
                    }
                    try {
                        wait((long) Config.repeatEveryMinutesValue * 1000 * 60);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
    }


    public void start() {
        threadPool = Executors.newScheduledThreadPool(threads);
        writeToLog("Viewbot has been started with: " + threads + " threads");
        try {
            for (int i = 0; i < threads; i++) {
                threadPool.scheduleWithFixedDelay(getViewRunnable(), 0, requestDelay, TimeUnit.MILLISECONDS);
            }
            if (!Config.stopWhenOfflineValue) {
                Thread.sleep((long) Config.stopAfterHsValue * 1000 * 60 * 60);
            } else {
                while (controllerMain.getStartButton().getText().equals("START")) {
                    if (!TwitchUtil.isChannelLive(target)) break;
                    Thread.sleep(2000);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            writeToLog("Failed get channel status");
        }
        stop();
    }


    private ViewRunnable getViewRunnable() throws IOException {
        String[] fullIp = new String[0];
        try {
            fullIp = proxyQueue.take().split(":");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String ip = fullIp[0];
        int port = Integer.parseInt(fullIp[1]);

        return new ViewRunnable(ip, port);
    }


    public void stop() {
        if (waitingThread.isAlive()) {
            waitingThread.interrupt();
        } else if (threadPool != null) {
            new Thread(() -> {
                writeToLog("Shutdowning threads...");
                writeToLog("Wait until console has been not cleared");
                threadPool.shutdown();
                threadPool.shutdownNow();
                try {
                    if (threadPool.awaitTermination(20000, TimeUnit.MILLISECONDS)) {
                        threadPool.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    threadPool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(controllerMain::stopViewBot);
            }).start();
        }
    }


    private void sendView(HttpClient client, String url) throws IOException {
        HttpHead headRequest = new HttpHead(url);
        headRequest.setHeader(HttpHeaders.USER_AGENT, USER_AGENT);
        headRequest.setHeader(HttpHeaders.ACCEPT, ACCEPT_VIDEO);
        client.client.execute(headRequest);
    }

    public Queue<String> getProxyQueue() {
        return proxyQueue;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public ViewBot setTarget(String target) {
        this.target = target;
        return this;
    }

    public ViewBot setProxyQueue(LinkedBlockingQueue<String> proxyQueue) {
        this.proxyQueue = proxyQueue;
        return this;
    }

    public void setRequestDelay(long requestDelay) {
        this.requestDelay = requestDelay;
    }

    public class ViewRunnable implements Runnable {

        private final String ip;
        private final int port;
        private final HttpClient httpClient;
        private final LiveStreamObj liveStreamObj;

        public ViewRunnable(String ip, int port) throws IOException {
            this.ip = ip;
            this.port = port;
            this.httpClient = new HttpClient(ip, port);
            this.liveStreamObj = initLiveStreamObj();
            if (liveStreamObj == null)
                throw new IllegalStateException();
        }

        public LiveStreamObj initLiveStreamObj() throws IOException {
            LiveStreamObj info = getInfo(httpClient);
            if (info.getSig() == null || info.getToken() == null) {
                writeToLog("Bad proxy. Continuing...");
                return null;
            }
            return info;
        }

        @Override
        public void run() {
            try {
                String videoSequenceURL = getVideoSequence(httpClient, liveStreamObj.getToken(), liveStreamObj.getSig());
                if (!videoSequenceURL.isEmpty()) {
                    Platform.runLater(controllerMain::addCount);
                }
            } catch (IOException | JSONException e) {
                writeToLog(String.format("Bad proxy: %s:%d", ip, port));
            }
        }

        private LiveStreamObj getInfo(HttpClient httpClient) throws JSONException, IOException {
            String url = String.format(GET_INFO, target, CLIENT_ID);
            HttpGet getRequest = new HttpGet(url);
            getRequest.setHeader(HttpHeaders.USER_AGENT, USER_AGENT);
            getRequest.setHeader(HttpHeaders.ACCEPT, ACCEPT_INFO);
            getRequest.setHeader(HttpHeaders.CONTENT_TYPE, CONTENT_INFO);
            getRequest.setHeader(HttpHeaders.ACCEPT_LANGUAGE, ACCEPT_LANG);
            getRequest.setHeader(HttpHeaders.REFERER, REFERER + target);
            CloseableHttpResponse response = httpClient.client.execute(getRequest);
            String body;
            try {
                body = EntityUtils.toString(response.getEntity());
            } finally {
                response.close();
            }
            JSONObject jsonObject = new JSONObject(body);
            String token = URLEncoder.encode(jsonObject.getString("token").replace("\\", ""), StandardCharsets.UTF_8);
            String sig = jsonObject.getString("sig").replace("\\", "");
            return new LiveStreamObj(token, sig);
        }

        private String getVideoSequence(HttpClient client, String token, String sig) throws IOException {
            String url = String.format(GET_VIDEO, target, sig, token);
            System.out.println(url);
            HttpGet getRequest = new HttpGet(url);
            getRequest.setHeader(HttpHeaders.USER_AGENT, USER_AGENT);
            getRequest.setHeader(HttpHeaders.ACCEPT, ACCEPT_VIDEO);
            CloseableHttpResponse response = client.client.execute(getRequest);
            String body;
            body = EntityUtils.toString(response.getEntity());

            if (body == null) {
                writeToLog("Can't get video sequence");
                return "";
            }
            return "https://" + body.substring(body.indexOf("https://") + "https://".length(), body.indexOf(".m3u8")) + ".m3u8";
        }
    }

}
