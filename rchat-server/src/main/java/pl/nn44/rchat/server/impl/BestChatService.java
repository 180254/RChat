package pl.nn44.rchat.server.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Striped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.rchat.protocol.exception.ChatException;
import pl.nn44.rchat.protocol.exception.ChatException.Reason;
import pl.nn44.rchat.protocol.model.Channel;
import pl.nn44.rchat.protocol.model.Response;
import pl.nn44.rchat.protocol.model.User;
import pl.nn44.rchat.protocol.model.WhatsUp;
import pl.nn44.rchat.protocol.model.WhatsUp.What;
import pl.nn44.rchat.server.aspect.Loggable;
import pl.nn44.rchat.server.model.ServerChannel;
import pl.nn44.rchat.server.model.ServerUser;
import pl.nn44.rchat.server.util.BigIdGenerator;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Loggable
public class BestChatService implements ChatService {

    private static final Logger LOG = LoggerFactory.getLogger(BestChatService.class);

    public static final int MAX_NEWS_PER_REQUEST = 8;
    public static final int ID_RANDOM_BITS = 8 * BigIdGenerator.BITS_PER_CHAR;
    public static final int STRIPED_LOCKS = 32;

    public static final String SESSION_CLEANUP_CRON = "0 */5 * * * *";
    public static final long SESSION_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(3L);

    private final Random random = new SecureRandom();
    private final Iterator<String> idGenerator = BigIdGenerator.bits(random, ID_RANDOM_BITS);
    private final Pattern nameValidator = Pattern.compile("[a-zA-Z0-9_.-]{1,10}");

    private final ConcurrentMap<String, String> accounts/*username/password*/ = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ServerUser> sessionToUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ServerChannel> channelByName = new ConcurrentHashMap<>();

    private final Striped<Lock> stripedLocks = Striped.lazyWeakLock(STRIPED_LOCKS);

    public BestChatService() {
        accounts.put("admin", "admin");
        accounts.put("student", "student");

        channelByName.put("anybody", new ServerChannel("anybody", null, ""));
        channelByName.put("python", new ServerChannel("python", null, "python lovers"));
        channelByName.put("cars", new ServerChannel("cars", null, "no bike"));
        channelByName.put("students", new ServerChannel("students", null, "trust me, i'm an engineer"));
        channelByName.put("admins", new ServerChannel("admins", "admins", "keep silence"));

        channelByName.get("anybody").getAdmins().add("admin");
        channelByName.get("python").getAdmins().add("admin");
        channelByName.get("cars").getAdmins().add("admin");
        channelByName.get("students").getAdmins().add("admin");
        channelByName.get("students").getAdmins().add("student");
        channelByName.get("admins").getAdmins().add("admin");

        channelByName.get("python").getBanned().add("java");

        LOG.info("{} instance created.", getClass().getSimpleName());
    }

    @Override
    public Response<String> login(String username, @Nullable String password) throws ChatException {
        Locks locks = locks(null, null, username);

        try {
            if (!nameValidator.matcher(username).matches()) {
                throw new ChatException(Reason.GIVEN_BAD_USERNAME);
            }

            if (!Objects.equals(accounts.get(username), password)) {
                throw new ChatException(Reason.GIVEN_BAD_PASSWORD);
            }

            if (sessionToUser.containsValue(ServerUser.dummyUser(username))) {
                throw new ChatException(Reason.ALREADY_LOGGED_IN);
            }

            String session = idGenerator.next();
            ServerUser user = new ServerUser(session, username);
            sessionToUser.put(session, user);

            WhatsUp nothing = WhatsUp.create(What.NOTHING);
            offer(nothing, user);

            return Response.ok(user.getSession());

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response<?> logout(String session) throws ChatException {
        Locks locks = locks(session, null, null);

        try {
            Params params = params(session, null, null, false, false);

            for (ServerChannel channel : params.caller.getChannels()) {
                // double lock is safe operation:
                // "If the current thread already holds the lock
                // then the hold count is incremented by one and the method returns immediately."
                part(session, channel.getName(), "unused");
            }

            WhatsUp nothing = WhatsUp.create(What.NOTHING);
            offer(nothing, params.caller);

            sessionToUser.remove(session);

            return Response.ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response<Channel[]> channels(String session) throws ChatException {
        Locks locks = locks(session, null, null);

        try {
            //  side-effect used: verify session
            params(session, null, null, false, false);

            Channel[] channels = channelByName.values().stream()
                    .map(chan -> new Channel(
                            chan.getName(),
                            chan.getPassword() != null
                    ))
                    .toArray(Channel[]::new);

            return Response.ok(channels);

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response<Channel> join(String session, String channel, @Nullable String password) throws ChatException {
        Locks locks = locks(session, channel, null);

        try {
            Params params = params(session, null, null, false, false);
            // cannot get channel in one params request as then there is used verification if user is on channel
            params.channel = params(null, channel, null, false, false).channel;

            if (!Objects.equals(params.channel.getPassword(), password)) {
                throw new ChatException(Reason.GIVEN_BAD_PASSWORD);
            }

            if (params.channel.getBanned().contains(params.caller.getUsername())) {
                throw new ChatException(Reason.UNWELCOME_BANNED);
            }

            /* unnecessary until own channels are allowed
            if (!nameValidator.matcher(channel).matches()) {
                throw new ChatException(Reason.GIVEN_BAD_CHANNEL);
            }
            */

            boolean addC = params.channel.getUsers().addIfAbsent(params.caller);
            boolean addU = params.caller.getChannels().addIfAbsent(params.channel);

            if (addC ^ addU) {
                LOG.warn("join(): addC ^ addU is true, but it should not");
            }

            if (addC) {
                boolean auth = accounts.containsKey(params.caller.getUsername());
                boolean admin = params.channel.getAdmins().contains(params.caller.getUsername());

                WhatsUp whatsUp = WhatsUp.create(
                        What.JOIN,
                        params.channel.getName(),
                        params.caller.getUsername(),
                        Boolean.toString(auth),
                        Boolean.toString(admin)
                );

                params.channel.getUsers().stream()
                        .filter(cu -> !cu.equals(params.caller))
                        .forEach(cu -> offer(whatsUp, cu));
            }

            User[] users = params.channel.getUsers()
                    .stream()
                    .map(cUser -> new User(
                            params.channel.getName(),
                            cUser.getUsername(),
                            accounts.containsKey(cUser.getUsername()),
                            params.caller.getIgnored().contains(cUser.getUsername()),
                            params.channel.getAdmins().contains(cUser.getUsername()),
                            params.channel.getBanned().contains(cUser.getUsername())
                    ))
                    .toArray(User[]::new);

            Channel pChannel = new Channel(
                    params.channel.getName(),
                    params.channel.getPassword() != null,
                    params.channel.getTopic(),
                    users
            );

            return Response.ok(pChannel);

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response<?> part(String session, String channel, String unused) throws ChatException {
        Locks locks = locks(session, channel, null);

        try {
            Params params = params(session, null, null, false, false);
            // without verification if caller is on channel, NO_PERMISSION will not be thrown
            params.channel = params(null, channel, null, false, false).channel;

            boolean removeC = params.channel.getUsers().remove(params.caller);
            boolean removeU = params.caller.getChannels().remove(params.channel);

            if (removeC ^ removeU) {
                LOG.warn("part(): removeC ^ removeU is true, but it should not");
            }

            if (removeC) {
                WhatsUp whatsUp = WhatsUp.create(
                        What.PART,
                        params.channel.getName(),
                        params.caller.getUsername()
                );

                for (ServerUser su : params.channel.getUsers()) {
                    offer(whatsUp, su);
                }
            }

            return Response.ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response<?> topic(String session, String channel, String text) throws ChatException {
        Locks locks = locks(session, channel, null);

        try {
            Params params = params(session, channel, null, true, false);

            boolean change = !params.channel.getTopic().equals(text);

            if (change) {
                params.channel.setTopic(text);

                WhatsUp whatsUp = WhatsUp.create(
                        What.TOPIC,
                        params.channel.getName(),
                        params.caller.getUsername(),
                        text
                );

                for (ServerUser su : params.channel.getUsers()) {
                    offer(whatsUp, su);
                }
            }

            return Response.ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response<?> kick(String session, String channel, String username) throws ChatException {
        Locks locks = locks(session, channel, username);

        try {
            Params params = params(session, channel, username, true, true);

            boolean removeC = params.channel.getUsers().remove(params.affUser);
            boolean removeU = params.affUser.getChannels().remove(params.channel);

            if (removeC ^ removeU) {
                LOG.warn("kick(): removeC ^ removeU is true, but it should not");
            }

            if (removeC) {
                WhatsUp wuKick = WhatsUp.create(
                        What.KICK,
                        params.channel.getName(),
                        params.affUser.getUsername(),
                        params.caller.getUsername()
                );

                for (ServerUser su : params.channel.getUsers()) {
                    offer(wuKick, su);
                }

                offer(wuKick, params.affUser);
            }

            return Response.ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response<?> ban(String session, String channel, String username, boolean state) throws ChatException {
        Locks locks = locks(session, channel, username);

        try {
            Params params = params(session, channel, username, true, false);

            boolean change = state
                    ? params.channel.getBanned().addIfAbsent(params.affUser.getUsername())
                    : params.channel.getBanned().remove(params.affUser.getUsername());

            if (change) {
                WhatsUp whatsUp = WhatsUp.create(
                        What.BAN,
                        params.channel.getName(),
                        params.affUser.getUsername(),
                        params.caller.getUsername(),
                        state ? "on" : "off"
                );

                for (ServerUser su : params.channel.getUsers()) {
                    offer(whatsUp, su);
                }
            }

            return Response.ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response<?> admin(String session, String channel, String username, boolean state) throws ChatException {
        Locks locks = locks(session, channel, username);

        try {
            Params params = params(session, channel, username, true, false);

            boolean change = state
                    ? params.channel.getAdmins().addIfAbsent(params.affUser.getUsername())
                    : params.channel.getAdmins().remove(params.affUser.getUsername());

            if (change) {
                WhatsUp whatsUp = WhatsUp.create(
                        What.ADMIN,
                        params.channel.getName(),
                        params.affUser.getUsername(),
                        params.caller.getUsername(),
                        state ? "on" : "off"
                );

                for (ServerUser su : params.channel.getUsers()) {
                    offer(whatsUp, su);
                }
            }

            return Response.ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response<?> ignore(String session, String unused, String username, boolean state) throws ChatException {
        Locks locks = locks(session, null, username);

        try {
            Params params = params(session, null, username, false, false);

            boolean change = state
                    ? params.caller.getIgnored().addIfAbsent(params.affUser.getUsername())
                    : params.caller.getIgnored().remove(params.affUser.getUsername());

            if (change) {
                WhatsUp whatsUp = WhatsUp.create(
                        What.IGNORE,
                        "unused",
                        params.affUser.getUsername(),
                        params.caller.getUsername(),
                        state ? "on" : "off"
                );

                offer(whatsUp, params.caller);

                // notify affUser if he is on any chan
                channelByName.values().stream()
                        .flatMap(c -> c.getUsers().stream())
                        .filter(u -> u.equals(params.affUser))
                        .findFirst()
                        .ifPresent(su -> offer(whatsUp, su));
            }

            return Response.ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response<?> message(String session, String channel, String text) throws ChatException {
        Locks locks = locks(session, channel, null);

        try {
            Params params = params(session, channel, null, false, false);

            WhatsUp whatsUp = WhatsUp.create(
                    What.MESSAGE,
                    params.channel.getName(),
                    params.caller.getUsername(),
                    text
            );

            for (ServerUser su : params.channel.getUsers()) {
                boolean ignore = su.getIgnored().contains(params.caller.getUsername());

                if (!ignore) {
                    offer(whatsUp, su);
                }
            }

            return Response.ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response<?> privy(String session, String username, String text) throws ChatException {
        Locks locks = locks(session, null, username);

        try {
            Params params = params(session, null, username, false, false);

            boolean ignore = params.affUser.getIgnored().contains(params.caller.getUsername());

            if (!ignore) {
                WhatsUp whatsUp = WhatsUp.create(
                        What.PRIVY,
                        "unused",
                        params.affUser.getUsername(),
                        params.caller.getUsername(),
                        text
                );

                offer(whatsUp, params.caller);
                offer(whatsUp, params.affUser);
            }

            return Response.ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response<WhatsUp[]> whatsUp(String session, int longPoolingTimeoutMs) throws ChatException {
        Params params;

        Locks locks = locks(session, null, null);
        try {
            params = params(session, null, null, false, true);
        } finally {
            locks.unlock();
        }

        ArrayList<WhatsUp> news = new ArrayList<>(MAX_NEWS_PER_REQUEST / 2);

        while (news.size() < MAX_NEWS_PER_REQUEST) {
            WhatsUp poll = params.caller.getNews().poll();

            if (poll != null) {
                news.add(poll);

            } else if (news.size() == 0) {
                try {
                    poll = params.caller.getNews().poll(longPoolingTimeoutMs, TimeUnit.MILLISECONDS);

                    if (poll != null) {
                        news.add(poll);
                    } else {
                        break;
                    }

                } catch (InterruptedException e) {
                    LOG.warn("whatsUp InterruptedException", e);
                    break;
                }

            } else {
                break;
            }
        }

        WhatsUp[] newsArray = new WhatsUp[news.size()];
        newsArray = news.toArray(newsArray);
        return Response.ok(newsArray);
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Scheduled(cron = SESSION_CLEANUP_CRON)
    public int sessionCleanup() {
        LocalDateTime now = LocalDateTime.now();

        List<Map.Entry<String, ServerUser>> ghosts =
                sessionToUser
                        .entrySet().stream()
                        .filter(se -> {
                            LocalDateTime lastSync = se.getValue().getLastSync();
                            return ChronoUnit.SECONDS.between(lastSync, now) >= SESSION_TIMEOUT_SECONDS;
                        })
                        .collect(Collectors.toList());

        ghosts.forEach(se -> {
            try {
                logout(se.getKey());

            } catch (ChatException e) {
                LOG.warn("sessionCleanup ChatException: {} {}", se.getValue(), e.toString());
            }
        });

        return ghosts.size();
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public Response<?> test(boolean exception) throws ChatException {
        if (exception) {
            throw new ChatException(Reason.NO_PERMISSION);
        }

        return Response.ok(
                Response.ok(
                        new ImmutableMap.Builder<>()
                                .put("string", "xxx")
                                .put("number", 999)
                                .put("response", Response.ok(10))
                                .put("response-null", Response.ok())
                                .put("array-object", new Object[]{1, "2", What.BAN})
                                .put("list", Arrays.asList(2, "3", What.JOIN))
                                .put("map", ImmutableMap.<Object, Object>of("key", "value"))
                                .put("whatsUp", WhatsUp.create(What.TOPIC, "any", "topic"))
                                .put("whatsUp-param", WhatsUp.create(What.TOPIC, "any", "topic", "p1", "p2"))
                                .put("empty-array", new WhatsUp[0])
                                .put("empty-list", new ArrayList<WhatsUp>())
                                .build()
                )
        );
    }

    // ---------------------------------------------------------------------------------------------------------------

    private class Params {

        private final String pSession;
        private final String pChannel;
        private final String pUsername;
        private final boolean pCheckAdmin;
        private final boolean pAffUserOnChan;

        public ServerUser caller;
        public ServerChannel channel;
        public ServerUser affUser;

        Params(String session,
               String channel,
               String username,
               boolean checkAdmin,
               boolean affUserOnChan)
                throws ChatException {

            this.pSession = session;
            this.pChannel = channel;
            this.pUsername = username;
            this.pCheckAdmin = checkAdmin;
            this.pAffUserOnChan = affUserOnChan;
        }

        public void process() throws ChatException {

            if (pSession != null) {
                this.caller = sessionToUser.get(pSession);
                if (caller == null) {
                    throw new ChatException(Reason.GIVEN_BAD_SESSION);
                }
            }

            if (pChannel != null) {
                this.channel = channelByName.get(pChannel);
                if (channel == null) {
                    throw new ChatException(Reason.GIVEN_BAD_CHANNEL);
                }
            }

            if (caller != null && channel != null) {
                if (!channel.getUsers().contains(caller)) {
                    throw new ChatException(Reason.NO_PERMISSION);
                }
            }

            if (pUsername != null && channel == null) {
                this.affUser = ServerUser.dummyUser(pUsername);
            }

            if (pUsername != null && channel != null) {
                ServerUser dummyAffUser = ServerUser.dummyUser(pUsername);

                this.affUser = channel.getUsers().stream()
                        .filter(u -> u.equals(dummyAffUser))
                        .findFirst().orElse(null);
            }

            if (pUsername != null && channel != null && pAffUserOnChan) {
                if (affUser == null) {
                    throw new ChatException(Reason.GIVEN_BAD_USERNAME);
                }
            }

            if (pUsername != null && channel != null && !pAffUserOnChan) {
                if (affUser == null) {
                    this.affUser = ServerUser.dummyUser(pUsername);
                }
            }

            if (channel != null && caller != null && pCheckAdmin) {
                if (!channel.getAdmins().contains(caller.getUsername())) {
                    throw new ChatException(Reason.NO_PERMISSION);
                }
            }

            if (caller != null) {
                caller.updateLastSync();
            }
        }
    }

    // session user is now -> caller
    // channel is now -> channel
    // param username is now -> affUser
    //
    // checks if:
    // - caller(session) is proper (GIVEN_BAD_SESSION)           [ if session != null ]
    // - channel(channel) is proper (GIVEN_BAD_CHANNEL)          [ if channel != null ]
    // - affUser(username) is on channel (GIVEN_BAD_USERNAME)    [ if username,channel != null && affUserOnChan ]
    // - caller is on channel (NO_PERMISSION)                    [ if session,channel != null ]
    // - caller is admin on channel (NO_PERMISSION)              [ if session,channel != null && checkAdmin ]
    // and:
    // - update caller last sync timestamp
    private Params params(String session,
                          String channel,
                          String username,
                          boolean checkAdmin,
                          boolean affUserOnChan)
            throws ChatException {

        Params params = new Params(
                session, channel, username,
                checkAdmin, affUserOnChan
        );
        params.process();
        return params;
    }

    // ---------------------------------------------------------------------------------------------------------------

    private class Locks {

        private final Lock lockCaller;
        private final Lock lockChannel;
        private final Lock lockAffUser;

        Locks(String session,
              String channel,
              String username)
                throws ChatException {

            if (session != null) {
                ServerUser user = sessionToUser.get(session);

                if (user == null) {
                    throw new ChatException(Reason.GIVEN_BAD_SESSION);
                }

                this.lockCaller = stripedLocks.get("U$" + user.getUsername());
            } else {
                this.lockCaller = null;
            }

            if (username != null) {
                this.lockAffUser = stripedLocks.get("U$" + username);
            } else {
                this.lockAffUser = null;
            }

            if (channel != null) {
                this.lockChannel = stripedLocks.get("C$" + channel);
            } else {
                this.lockChannel = null;
            }
        }

        void lock() {
            if (lockChannel != null) {
                lockChannel.lock();
            }
            if (lockAffUser != null) {
                lockAffUser.lock();
            }
            if (lockCaller != null) {
                lockCaller.lock();
            }
        }

        void unlock() {
            if (lockChannel != null) {
                lockChannel.unlock();
            }
            if (lockAffUser != null) {
                lockAffUser.unlock();
            }
            if (lockCaller != null) {
                lockCaller.unlock();
            }
        }
    }

    private Locks locks(String session,
                        String channel,
                        String username)
            throws ChatException {

        Locks locks = new Locks(session, channel, username);
        locks.lock();
        return locks;
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void offer(WhatsUp wu, ServerUser su) {
        boolean offer = su.getNews().offer(wu);

        if (!offer) {
            LOG.warn("Unable to offer: {}, {}", wu, su);
        }
    }
}
