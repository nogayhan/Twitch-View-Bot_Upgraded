package viewbot;

import config.Config;
import org.json.JSONException;
import service.TwitchUtil;

import java.io.IOException;

public class StartThread extends Thread {

    private final ViewBot viewBot;

    public StartThread(ViewBot viewBot) {
        this.viewBot = viewBot;
    }

    public void prepareToStart() {
        if (Config.startWhenLiveValue) {
            try {
                String channelId = TwitchUtil.getChannelId(viewBot.getTarget());
                synchronized (this) {
                    while (true) {
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        viewBot.writeToLog("Waiting when channel goes live");

                        if (TwitchUtil.isChannelLive(channelId)) {
                            break;
                        }
                        wait((long) Config.repeatEveryMinutesValue * 1000 * 60);
                    }
                    viewBot.start();
                }
            } catch (IOException e) {
                viewBot.writeToLog("Failed to get channel status");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (JSONException e) {
                viewBot.writeToLog("Failed to parse JSON");
            }
        }
    }

    @Override
    public void run() {
        prepareToStart();
    }
}
