package pl.nn44.rchat.client;

import pl.nn44.rchat.client.impl.Clients;
import pl.nn44.rchat.client.util.PropLoader;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.rchat.protocol.exception.ChatException;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Spammer {

    private static final int SLEEP_MS = 400;

    private final ChatService cs = new Clients<>(PropLoader.get(), ChatService.class).burlap();
    private final Random random = new Random();

    private final String username;
    private final String channel;

    public Spammer(String channel) {
        this.username = nextStr();
        this.channel = channel;
    }

    public void go() throws ChatException, InterruptedException {
        String token = cs.login(username, null).getPayload();
        cs.join(token, channel, null);

        //noinspection InfiniteLoopStatement
        while (true) {
            cs.message(token, channel, nextStr());
            Thread.sleep(SLEEP_MS);
        }
    }

    private String nextStr() {
        return new BigInteger(40, random).toString(32);
    }

    // ---------------------------------------------------------------------------------------------------------------

    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool();

        executor.submit(() -> {
            new Spammer("python").go();
            return 1;
        });

        executor.submit(() -> {
            new Spammer("anybody").go();
            return 1;
        });
    }
}
