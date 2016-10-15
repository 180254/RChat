package pl.nn44.rchat.server.impl;

import com.google.common.util.concurrent.Striped;
import org.jetbrains.annotations.Nullable;
import pl.nn44.rchat.protocol.*;
import pl.nn44.rchat.protocol.RChatException.Reason;
import pl.nn44.rchat.protocol.WhatsUp.What;
import pl.nn44.rchat.server.util.BigIdGenerator;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public class BestChatService implements ChatService {

    static int MAX_NEWS_PER_REQUEST = 10;

    Random random = new SecureRandom();
    Iterator<String> idGenerator = BigIdGenerator.bits(random, 128);

    ConcurrentMap<String, String> accounts/*username$user/password*/ = new ConcurrentHashMap<>();
    ConcurrentMap<String, User> sessionToUser = new ConcurrentHashMap<>();
    ConcurrentMap<String, Channel> channelByName = new ConcurrentHashMap<>();

    Striped<Lock> stripedLocks = Striped.lazyWeakLock(100);

    @Override
    public Response<String> login(String username, @Nullable String password) throws RChatException {
        Locks locks = locks(null, null, username);


        try {
            if (sessionToUser.containsValue(User.Dummy(username))) {
                throw new RChatException(Reason.ALREADY_LOGGED_IN);
            }

            if (!Objects.equals(accounts.get(username), password)) {
                throw new RChatException(Reason.GIVEN_BAD_PASSWORD);
            }

            String session = idGenerator.next();
            User exUser = new User(session, username);
            sessionToUser.put(username, exUser);
            return Response.Ok(session);

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response logout(String session) throws RChatException {
        Locks locks = locks(session, null, null);

        try {
            Params params = params(session, null, null, false);

            for (Channel chan : params.caller.getChannels()) {
                boolean remove = chan.getUsers().remove(params.caller);

                if (remove) {
                    WhatsUp whatsUp = new WhatsUp(
                            What.PART,
                            chan.getName(),
                            params.caller.getUsername()
                    );

                    for (User cu : chan.getUsers()) {
                        cu.getNews().offer(whatsUp);
                    }
                }
            }

            sessionToUser.remove(session);
            return Response.Ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response<String> join(String session, String channel, @Nullable String password) throws RChatException {
        Locks locks = locks(session, channel, null);

        try {
            Params params = params(session, channel, null, false);

            if (!Objects.equals(params.channel.getPassword(), password)) {
                throw new RChatException(Reason.GIVEN_BAD_PASSWORD);
            }

            if (params.channel.getBanned().contains(params.caller)) {
                throw new RChatException(Reason.UNWELCOME_BANNED);
            }

            boolean add = params.caller.getChannels().addIfAbsent(params.channel);
            params.channel.getUsers().addIfAbsent(params.caller);

            if (add) {
                WhatsUp whatsUp = new WhatsUp(
                        What.JOIN,
                        params.channel.getName(),
                        params.caller.getUsername()
                );

                for (User cu : params.channel.getUsers()) {
                    cu.getNews().offer(whatsUp);
                }
            }

            return Response.Ok(params.channel.getTopic());

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response part(String session, String channel) throws RChatException {
        Locks locks = locks(session, channel, null);

        try {
            Params params = params(session, channel, null, false);

            boolean remove = params.caller.getChannels().remove(params.channel);
            params.channel.getUsers().remove(params.caller);

            if (remove) {
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
    public Response kick(String session, String channel, String username) throws RChatException {
        Locks locks = locks(session, channel, username);

        try {
            Params params = params(session, channel, username, true);

            boolean remove = params.channel.getUsers().remove(params.affUser);

            if (remove) {
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
    public Response ban(String session, String channel, String username, boolean state) throws RChatException {
        Locks locks = locks(session, channel, username);

        try {
            Params params = params(session, channel, username, true);

            boolean change = state
                    ? params.channel.getBanned().addIfAbsent(params.affUser)
                    : params.channel.getBanned().remove(params.affUser);

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

    public Response<ChannelUser[]> names(String session, String channel) throws RChatException {
        Locks locks = new Locks(session, channel, null);

        try {
            Params params = params(session, channel, null, false);

            ChannelUser[] channelUsers = params.channel.getUsers()
                    .stream()
                    .map(cUser -> new ChannelUser(
                            cUser.getUsername(),
                            accounts.containsKey(cUser.getUsername()),
                            params.caller.getIgnored().contains(cUser),
                            params.channel.getAdmins().contains(cUser)
                    ))
                    .toArray(ChannelUser[]::new);

            return Response.Ok(channelUsers);

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response topic(String session, String channel, String text) throws RChatException {
        Locks locks = new Locks(session, channel, null);

        try {
            Params params = params(session, channel, null, true);

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

            return Response.Ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response admin(String session, String channel, String username, boolean state) throws RChatException {
        Locks locks = locks(session, channel, username);

        try {
            Params params = params(session, channel, username, true);

            boolean change = state
                    ? params.channel.getAdmins().addIfAbsent(params.affUser)
                    : params.channel.getAdmins().remove(params.affUser);

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
    public Response ignore(String session, String channel, String username, boolean state) throws RChatException {
        Locks locks = locks(session, channel, username);

        try {
            Params params = params(session, channel, username, false);

            boolean change = state
                    ? params.caller.getIgnored().addIfAbsent(params.affUser)
                    : params.caller.getIgnored().remove(params.affUser);

            if (change) {
                WhatsUp whatsUp = new WhatsUp(
                        What.IGNORE,
                        params.channel.getName(),
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
    public Response privy(String session, String username, String message) throws RChatException {
        Locks locks = locks(session, null, username);

        try {
            Params params = params(session, null, username, false);

            WhatsUp whatsUp = new WhatsUp(
                    What.PRIVY,
                    null,
                    params.affUser.getUsername(),
                    params.caller.getUsername(),
                    message
            );

            params.caller.getNews().offer(whatsUp);
            params.affUser.getNews().offer(whatsUp);

            return Response.Ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response message(String session, String channel, String message) throws RChatException {
        Locks locks = locks(session, channel, null);

        try {
            Params params = params(session, channel, null, false);

            WhatsUp whatsUp = new WhatsUp(
                    What.MESSAGE,
                    params.channel.getName(),
                    params.caller.getUsername(),
                    message
            );

            for (User cu : params.channel.getUsers()) {
                cu.getNews().offer(whatsUp);
            }

            return Response.Ok();

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response<WhatsUp[]> whatsUp(String session, int longPoolingTimeoutMs) throws RChatException {
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
                    } catch (InterruptedException ignored) {
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

    class Params {

        public User caller;
        public Channel channel;
        public User affUser;

        Params(String session,
               String channel,
               String username,
               boolean needAdmin)
                throws RChatException {

            if (session != null) {
                this.caller = sessionToUser.get(session);
                if (this.caller == null) {
                    throw new RChatException(Reason.GIVEN_BAD_SESSION);
                }
            }

            if (channel != null) {
                this.channel = channelByName.get(channel);
                if (this.channel == null) {
                    throw new RChatException(Reason.GIVEN_BAD_CHANNEL);
                }
            }

            if (username != null && this.channel != null) {
                this.affUser = this.channel.getUsers().stream()
                        .filter(u -> u.getUsername().equals(username))
                        .findFirst().orElse(null);
                if (this.affUser == null) {
                    throw new RChatException(Reason.GIVEN_BAD_USERNAME);
                }

                if (!this.channel.getUsers().contains(this.caller)) {
                    throw new RChatException(Reason.NO_PERMISSION);
                }
            }

            if (needAdmin && this.channel != null && this.caller != null) {
                if (this.channel.getAdmins().contains(this.caller)) {
                    throw new RChatException(Reason.NO_PERMISSION);
                }
            }

        }
    }

    // checks if:
    // - caller is proper (GIVEN_BAD_SESSION)
    // - channel is proper (GIVEN_BAD_CHANNEL)
    // - username$user is on channel$chan (GIVEN_BAD_USERNAME)
    // - caller$user is on channel$chan (NO_PERMISSION)
    // - caller$user is admin on channel$chan (NO_PERMISSION)
    Params params(String session,
                  String channel,
                  String username,
                  boolean needAdmin)
            throws RChatException {

        return new Params(session, channel, username, needAdmin);
    }

    // ---------------------------------------------------------------------------------------------------------------

    class Locks {

        Lock lock$session;
        Lock lock$channel;
        Lock lock$username;

        Locks(String session,
              String channel,
              String username)
                throws RChatException {

            if (session != null) {
                User user = sessionToUser.get(session);

                if (user == null) {
                    throw new RChatException(Reason.GIVEN_BAD_SESSION);
                }

                this.lock$session = stripedLocks.get("U$" + user.getUsername());
            }
            if (username != null) {
                this.lock$username = stripedLocks.get("U$" + username);
            }
            if (channel != null) {
                this.lock$channel = stripedLocks.get("C$" + channel);
            }
        }

        void unlock() {
            if (this.lock$channel != null) {
                this.lock$channel.unlock();
            }
            if (this.lock$username != null) {
                this.lock$username.unlock();
            }
            if (this.lock$session != null) {
                this.lock$session.unlock();
            }
        }
    }

    Locks locks(String session,
                String channel,
                String username)
            throws RChatException {

        return new Locks(session, channel, username);
    }
}
