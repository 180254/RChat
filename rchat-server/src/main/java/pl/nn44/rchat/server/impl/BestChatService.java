package pl.nn44.rchat.server.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Striped;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import pl.nn44.rchat.protocol.*;
import pl.nn44.rchat.protocol.ChatException.Reason;
import pl.nn44.rchat.protocol.WhatsUp.What;
import pl.nn44.rchat.server.as.AsLoggable;
import pl.nn44.rchat.server.util.BigIdGenerator;

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

import static pl.nn44.rchat.protocol.ChatException.Reason.*;

@AsLoggable
public class BestChatService implements ChatService {

    private static final Logger LOG = LoggerFactory.getLogger(BestChatService.class);
    private static final int MAX_NEWS_PER_REQUEST = 10;

    private final Random random = new SecureRandom();
    private final Iterator<String> idGenerator = BigIdGenerator.chars(random, 6);
    private final Pattern nameValidator = Pattern.compile("[a-zA-Z0-9_.-]{1,10}");

    private final ConcurrentMap<String, String> accounts/*username/password*/ = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, User> sessionToUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Channel> channelByName = new ConcurrentHashMap<>();

    private final Striped<Lock> stripedLocks = Striped.lazyWeakLock(100);

    public BestChatService() {
        accounts.put("admin", "admin");
        accounts.put("student", "student");

        channelByName.put("standard", new Channel("standard", null));
        channelByName.put("admins", new Channel("admins", "admins"));

        channelByName.get("standard").getAdmins().add("admin");
        channelByName.get("standard").getAdmins().add("student");
        channelByName.get("standard").getAdmins().add("admin");

        LOG.info("{} instance created.", getClass().getSimpleName());
    }

    @Override
    @CeReason({ALREADY_LOGGED_IN, GIVEN_BAD_PASSWORD, GIVEN_BAD_USERNAME})
    public Response<String> login(String username, @Nullable String password) throws ChatException {
        Locks locks = locks(null, null, username);

        try {
            if (sessionToUser.containsValue(User.Dummy(username))) {
                throw new ChatException(Reason.ALREADY_LOGGED_IN);
            }

            if (!Objects.equals(accounts.get(username), password)) {
                throw new ChatException(Reason.GIVEN_BAD_PASSWORD);
            }

            if (!nameValidator.matcher(username).matches()) {
                throw new ChatException(Reason.GIVEN_BAD_USERNAME);
            }

            String session = idGenerator.next();
            User exUser = new User(session, username);
            sessionToUser.put(session, exUser);
            return Response.Ok(session);

        } finally {
            locks.unlock();
        }
    }

    @Override
    @CeReason({GIVEN_BAD_SESSION})
    public Response<?> logout(String session) throws ChatException {
        Locks locks = locks(session, null, null);

        try {
            Params params = params(session, null, null, false);

            for (Channel channel : params.caller.getChannels()) {
                // double lock is safe operation:
                // "If the current thread already holds the lock
                // then the hold count is incremented by one and the method returns immediately."
                part(session, channel.getName());
            }

            sessionToUser.remove(session);

            return Response.Ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    @CeReason({GIVEN_BAD_SESSION})
    public Response<RChannel[]> channels(String session) throws ChatException {
        Locks locks = locks(session, null, null);

        try {
            //  side-effect used: verify session
            params(session, null, null, false);

            RChannel[] rChannels = channelByName.values().stream()
                    .map(c -> new RChannel(
                            c.getName(),
                            c.getPassword() != null
                    ))
                    .toArray(RChannel[]::new);

            return Response.Ok(rChannels);

        } finally {
            locks.unlock();
        }
    }

    @Override
    @CeReason({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_PASSWORD, UNWELCOME_BANNED})
    public Response<RChannel> join(String session, String channel, @Nullable String password) throws ChatException {
        Locks locks = locks(session, channel, null);

        try {
            Params params = params(session, null, null, false);
            // cannot get channel in one params request as then there is verified if user is on channel
            params.channel = params(null, channel, null, false).channel;

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
            boolean admin = params.channel.getAdmins().contains(params.caller.getUsername());

            if (addC ^ addU) {
                LOG.warn("join(): addC ^ addU is true, but it should not");
            }

            if (addC) {
                WhatsUp whatsUp = new WhatsUp(
                        What.JOIN,
                        params.channel.getName(),
                        params.caller.getUsername()
                );

                for (User cu : params.channel.getUsers()) {
                    cu.getNews().offer(whatsUp);
                }
            }

            if (addC && admin) {
                WhatsUp whatsUp = new WhatsUp(
                        What.ADMIN,
                        params.channel.getName(),
                        params.caller.getUsername(),
                        null,
                        "ON"
                );

                for (User cu : params.channel.getUsers()) {
                    cu.getNews().offer(whatsUp);
                }
            }

            RChUser[] rChUsers = params.channel.getUsers()
                    .stream()
                    .map(cUser -> new RChUser(
                            params.channel.getName(),
                            cUser.getUsername(),
                            accounts.containsKey(cUser.getUsername()),
                            params.caller.getIgnored().contains(cUser),
                            params.channel.getAdmins().contains(cUser.getUsername()),
                            params.channel.getBanned().contains(cUser.getUsername())
                    ))
                    .toArray(RChUser[]::new);

            RChannel rChannel = new RChannel(
                    params.channel.getName(),
                    params.channel.getPassword() != null,
                    params.channel.getTopic(),
                    rChUsers
            );

            return Response.Ok(rChannel);

        } finally {
            locks.unlock();
        }
    }

    @Override
    @CeReason({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL})
    public Response<?> part(String session, String channel) throws ChatException {
        Locks locks = locks(session, channel, null);

        try {
            Params params = params(session, channel, null, false);

            boolean removeC = params.channel.getUsers().remove(params.caller);
            boolean removeU = params.caller.getChannels().remove(params.channel);

            if (removeC ^ removeU) {
                LOG.warn("part(): removeC ^ removeU is true, but it should not");
            }

            if (removeC) {
                WhatsUp whatsUp = new WhatsUp(
                        What.PART,
                        params.channel.getName(),
                        params.caller.getUsername()
                );

                for (User cu : params.channel.getUsers()) {
                    cu.getNews().offer(whatsUp);
                }
            }

            return Response.Ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    @CeReason({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, NO_PERMISSION})
    public Response<?> topic(String session, String channel, String text) throws ChatException {
        Locks locks = new Locks(session, channel, null);

        try {
            Params params = params(session, channel, null, true);

            boolean change = !params.channel.getTopic().equals(text);

            if (change) {
                params.channel.setTopic(text);

                WhatsUp whatsUp = new WhatsUp(
                        What.TOPIC,
                        params.channel.getName(),
                        params.caller.getUsername(),
                        text
                );

                for (User cu : params.channel.getUsers()) {
                    cu.getNews().offer(whatsUp);
                }
            }

            return Response.Ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    @CeReason({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_USERNAME, NO_PERMISSION})
    public Response<?> kick(String session, String channel, String username) throws ChatException {
        Locks locks = locks(session, channel, username);

        try {
            Params params = params(session, channel, username, true);

            boolean removeC = params.channel.getUsers().remove(params.affUser);
            boolean removeU = params.affUser.getChannels().remove(params.channel);

            if (removeC ^ removeU) {
                LOG.warn("kick(): removeC ^ removeU is true, but it should not");
            }

            if (removeC) {
                WhatsUp whatsUp = new WhatsUp(
                        What.KICK,
                        params.channel.getName(),
                        params.affUser.getUsername(),
                        params.caller.getUsername()
                );

                for (User cu : params.channel.getUsers()) {
                    cu.getNews().offer(whatsUp);
                }
            }

            return Response.Ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    @CeReason({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_USERNAME, NO_PERMISSION})
    public Response<?> ban(String session, String channel, String username, boolean state) throws ChatException {
        Locks locks = locks(session, channel, username);

        try {
            Params params = params(session, channel, username, true);

            boolean change = state
                    ? params.channel.getBanned().addIfAbsent(params.affUser.getUsername())
                    : params.channel.getBanned().remove(params.affUser.getUsername());

            if (change) {
                WhatsUp whatsUp = new WhatsUp(
                        What.BAN,
                        params.channel.getName(),
                        params.affUser.getUsername(),
                        params.caller.getUsername(),
                        state ? "ON" : "OFF"
                );

                for (User cu : params.channel.getUsers()) {
                    cu.getNews().offer(whatsUp);
                }
            }

            return Response.Ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    @CeReason({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_USERNAME, NO_PERMISSION})
    public Response<?> admin(String session, String channel, String username, boolean state) throws ChatException {
        Locks locks = locks(session, channel, username);

        try {
            Params params = params(session, channel, username, true);

            boolean change = state
                    ? params.channel.getAdmins().addIfAbsent(params.affUser.getUsername())
                    : params.channel.getAdmins().remove(params.affUser.getUsername());

            if (change) {
                WhatsUp whatsUp = new WhatsUp(
                        What.ADMIN,
                        params.channel.getName(),
                        params.affUser.getUsername(),
                        params.caller.getUsername(),
                        state ? "ON" : "OFF"
                );

                for (User cu : params.channel.getUsers()) {
                    cu.getNews().offer(whatsUp);
                }
            }

            return Response.Ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    @CeReason({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_USERNAME, NO_PERMISSION})
    public Response<?> ignore(String session, String username, boolean state) throws ChatException {
        Locks locks = locks(session, null, username);

        try {
            Params params = params(session, null, username, false);

            boolean change = state
                    ? params.caller.getIgnored().addIfAbsent(params.affUser)
                    : params.caller.getIgnored().remove(params.affUser);

            if (change) {
                WhatsUp whatsUp = new WhatsUp(
                        What.IGNORE,
                        null,
                        params.affUser.getUsername(),
                        params.caller.getUsername(),
                        state ? "ON" : "OFF"
                );

                params.caller.getNews().offer(whatsUp);
                params.affUser.getNews().offer(whatsUp);
            }

            return Response.Ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    @CeReason({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_USERNAME, NO_PERMISSION})
    public Response<?> privy(String session, String username, String text) throws ChatException {
        Locks locks = locks(session, null, username);

        try {
            Params params = params(session, null, username, false);

            boolean ignore = params.affUser.getIgnored().contains(params.caller);

            if (!ignore) {
                WhatsUp whatsUp = new WhatsUp(
                        What.PRIVY,
                        null,
                        params.affUser.getUsername(),
                        params.caller.getUsername(),
                        text
                );

                params.caller.getNews().offer(whatsUp);
                params.affUser.getNews().offer(whatsUp);
            }

            return Response.Ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    @CeReason({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, NO_PERMISSION})
    public Response<?> message(String session, String channel, String text) throws ChatException {
        Locks locks = locks(session, channel, null);

        try {
            Params params = params(session, channel, null, false);

            WhatsUp whatsUp = new WhatsUp(
                    What.MESSAGE,
                    params.channel.getName(),
                    params.caller.getUsername(),
                    text
            );

            for (User cu : params.channel.getUsers()) {
                boolean ignore = cu.getIgnored().contains(params.caller);

                if (!ignore) {
                    cu.getNews().offer(whatsUp);
                }
            }

            return Response.Ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    @CeReason({GIVEN_BAD_SESSION})
    public Response<WhatsUp[]> whatsUp(String session, int longPoolingTimeoutMs) throws ChatException {
        Locks locks = locks(session, null, null);

        try {
            Params params = params(session, null, null, false);

            List<WhatsUp> news = new LinkedList<>();

            while (news.size() < MAX_NEWS_PER_REQUEST) {
                WhatsUp poll = params.caller.getNews().poll();

                if (poll != null) {
                    news.add(poll);

                } else if (news.size() == 0) {
                    try {
                        poll = params.caller.getNews().poll(longPoolingTimeoutMs, TimeUnit.MILLISECONDS);
                        if (poll != null) {
                            news.add(poll);
                        }
                    } catch (InterruptedException e) {
                        LOG.warn("whatsUp assertion error", e);
                        throw new AssertionError(e);
                    }

                    break;

                } else {
                    break;
                }
            }

            return Response.Ok(news.toArray(new WhatsUp[news.size()]));

        } finally {
            locks.unlock();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Scheduled(cron = "0 */5 * * * *")
    @CeReason({NO_PERMISSION})
    public int sessionCleanup() {
        LocalDateTime now = LocalDateTime.now();

        List<Map.Entry<String, User>> ghosts =
                sessionToUser
                        .entrySet().stream()
                        .filter(se -> ChronoUnit.MINUTES.between(se.getValue().getLastSync(), now) >= 3)
                        .collect(Collectors.toList());

        ghosts.forEach(se -> {
            try {
                logout(se.getKey());
            } catch (ChatException e) {
                LOG.warn("sessionCleanup assertion error", e);
                throw new AssertionError(e);
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

        return Response.Ok(
                Response.Ok(
                        new ImmutableMap.Builder<>()
                                .put("null", "null")
                                .put("string", "xxx")
                                .put("number", 999)
                                .put("response", Response.Ok(10))
                                .put("response-null", Response.Ok())
                                .put("array-object", new Object[]{1, "2", What.BAN})
                                .put("list", Arrays.asList(2, "3", What.JOIN))
                                .put("map", ImmutableMap.<Object, Object>of("key", "value"))
                                .put("whatsUp", new WhatsUp(What.TOPIC, "any", "topic"))
                                .put("whatsUp-param", new WhatsUp(What.TOPIC, "any", "topic", "p1", "p2"))
                                .build()
                )
        );
    }

    // ---------------------------------------------------------------------------------------------------------------

    private class Params {

        public User caller;
        public Channel channel;
        public User affUser;

        Params(String session,
               String channel,
               String username,
               boolean needAdmin)
                throws ChatException {

            if (session != null) {
                this.caller = sessionToUser.get(session);
                if (this.caller == null) {
                    throw new ChatException(Reason.GIVEN_BAD_SESSION);
                }
            }

            if (channel != null) {
                this.channel = channelByName.get(channel);
                if (this.channel == null) {
                    throw new ChatException(Reason.GIVEN_BAD_CHANNEL);
                }
            }

            if (this.caller != null && this.channel != null) {
                if (!this.channel.getUsers().contains(this.caller)) {
                    throw new ChatException(Reason.NO_PERMISSION);
                }
            }

            if (username != null && this.channel != null) {
                User dummyAffUser = User.Dummy(username);

                this.affUser = this.channel.getUsers().stream()
                        .filter(u -> u.equals(dummyAffUser))
                        .findFirst().orElse(null);
                if (this.affUser == null) {
                    throw new ChatException(Reason.GIVEN_BAD_USERNAME);
                }
            }

            if (needAdmin && this.channel != null && this.caller != null) {
                if (this.channel.getAdmins().contains(this.caller.getUsername())) {
                    throw new ChatException(Reason.NO_PERMISSION);
                }
            }

            if (this.caller != null) {
                this.caller.updateLastSync();
            }
        }
    }

    // checks if:
    // - caller(session) is proper (GIVEN_BAD_SESSION)
    // - channel(channel) is proper (GIVEN_BAD_CHANNEL)
    // - affUser(username) is on channel (GIVEN_BAD_USERNAME)
    // - caller is on channel (NO_PERMISSION)
    // - caller is admin on channel (NO_PERMISSION)
    // and:
    // - update caller last sync timestamp
    private Params params(String session,
                          String channel,
                          String username,
                          boolean needAdmin)
            throws ChatException {

        return new Params(session, channel, username, needAdmin);
    }

    // ---------------------------------------------------------------------------------------------------------------

    private class Locks {

        Lock lock$caller;
        Lock lock$channel;
        Lock lock$affUser;

        Locks(String session,
              String channel,
              String username)
                throws ChatException {

            if (session != null) {
                User user = sessionToUser.get(session);

                if (user == null) {
                    throw new ChatException(Reason.GIVEN_BAD_SESSION);
                }

                this.lock$caller = stripedLocks.get("U$" + user.getUsername());
            }
            if (username != null) {
                this.lock$affUser = stripedLocks.get("U$" + username);
            }
            if (channel != null) {
                this.lock$channel = stripedLocks.get("C$" + channel);
            }
        }

        void lock() {
            if (this.lock$channel != null) {
                this.lock$channel.lock();
            }
            if (this.lock$affUser != null) {
                this.lock$affUser.lock();
            }
            if (this.lock$caller != null) {
                this.lock$caller.lock();
            }
        }

        void unlock() {
            if (this.lock$channel != null) {
                this.lock$channel.unlock();
            }
            if (this.lock$affUser != null) {
                this.lock$affUser.unlock();
            }
            if (this.lock$caller != null) {
                this.lock$caller.unlock();
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
}
