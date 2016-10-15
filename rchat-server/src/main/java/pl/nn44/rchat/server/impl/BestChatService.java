package pl.nn44.rchat.server.impl;

import com.google.common.util.concurrent.Striped;
import org.jetbrains.annotations.Nullable;
import pl.nn44.rchat.protocol.*;
import pl.nn44.rchat.protocol.RChatException.Reason;
import pl.nn44.rchat.protocol.WhatsUp.What;
import pl.nn44.rchat.server.util.BigIdGenerator;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

public class BestChatService implements ChatService {

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


            for (Channel chan : params.session$user.getChannels()) {
                boolean remove = chan.getUsers().remove(params.session$user);

                if (remove) {
                    WhatsUp whatsUp = new WhatsUp(
                            What.PART,
                            chan.getName(),
                            params.session$user.getUsername()
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

            if (!Objects.equals(params.channel$chan.getPassword(), password)) {
                throw new RChatException(Reason.GIVEN_BAD_PASSWORD);
            }

            if (params.channel$chan.getBanned().contains(params.session$user.getUsername())) {
                throw new RChatException(Reason.UNWELCOME_BANNED);
            }

            boolean add = params.session$user.getChannels().addIfAbsent(params.channel$chan);
            params.channel$chan.getUsers().addIfAbsent(params.session$user);

            if (add) {
                WhatsUp whatsUp = new WhatsUp(
                        What.JOIN,
                        params.channel$chan.getName(),
                        params.session$user.getUsername()
                );


                for (User cu : params.channel$chan.getUsers()) {
                    cu.getNews().offer(whatsUp);
                }
            }

            return Response.Ok(params.channel$chan.getTopic());

        } finally {
            locks.unlock();
        }
    }

    @Override
    public Response part(String session, String channel) throws RChatException {
        Locks locks = locks(session, channel, null);

        try {
            Params params = params(session, channel, null, false);

            boolean remove = params.session$user.getChannels().remove(params.channel$chan);
            params.channel$chan.getUsers().remove(params.session$user);

            if (remove) {
                WhatsUp whatsUp = new WhatsUp(
                        What.PART,
                        params.channel$chan.getName(),
                        params.session$user.getUsername()
                );

                for (User cu : params.channel$chan.getUsers()) {
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

            boolean remove = params.channel$chan.getUsers().remove(params.username$user);

            if (remove) {
                WhatsUp whatsUp = new WhatsUp(
                        What.KICK,
                        params.channel$chan.getName(),
                        params.username$user.getUsername(),
                        params.session$user.getUsername()
                );

                for (User cu : params.channel$chan.getUsers()) {
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
                    ? params.channel$chan.getBanned().addIfAbsent(username)
                    : params.channel$chan.getBanned().remove(username);

            if (change) {
                WhatsUp whatsUp = new WhatsUp(
                        What.BAN,
                        params.channel$chan.getName(),
                        params.username$user.getUsername(),
                        params.session$user.getUsername(),
                        state ? "ON" : "OFF"
                );

                for (User cu : params.channel$chan.getUsers()) {
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

            Params params = new Params(session, channel, null, false);


            ChannelUser[] channelUsers = params.channel$chan.getUsers()
                    .stream()
                    .map(cUser -> new ChannelUser(
                            cUser.getUsername(),
                            accounts.containsKey(cUser.getUsername()),
                            params.session$user.getIgnored().contains(cUser.getUsername()),
                            params.channel$chan.getAdmins().contains(cUser.getUsername())
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
            Params params = new Params(session, channel, null, true);

            params.channel$chan.setTopic(text);

            WhatsUp whatsUp = new WhatsUp(
                    What.TOPIC,
                    params.channel$chan.getName(),
                    params.session$user.getUsername(),
                    text
            );

            for (User cu : params.channel$chan.getUsers()) {
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
                    ? params.channel$chan.getAdmins().addIfAbsent(username)
                    : params.channel$chan.getAdmins().remove(username);

            if (change) {
                WhatsUp whatsUp = new WhatsUp(
                        What.ADMIN,
                        params.channel$chan.getName(),
                        params.username$user.getUsername(),
                        params.session$user.getUsername(),
                        state ? "ON" : "OFF"
                );

                for (User cu : params.channel$chan.getUsers()) {
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
        return null;
    }

    @Override
    public Response privy(String session, String nickname, String text) throws RChatException {
        throw new RChatException(Reason.GIVEN_BAD_PASSWORD);
    }

    @Override
    public Response message(String session, String channel, String message) throws RChatException {
        return null;
    }

    @Override
    public Response<WhatsUp[]> whatsUp(String session, int longPoolingTimeoutMs) throws RChatException {
        return null;
    }

    // ---------------------------------------------------------------------------------------------------------------

    class Params {

        public User session$user;
        public Channel channel$chan;
        public User username$user;

        Params(String session,
               String channel,
               String username,
               boolean needAdmin)
                throws RChatException {

            if (session != null) {
                this.session$user = sessionToUser.get(session);
                if (this.session$user == null) {
                    throw new RChatException(Reason.GIVEN_BAD_SESSION);
                }
            }

            if (channel != null) {
                this.channel$chan = channelByName.get(channel);
                if (this.channel$chan == null) {
                    throw new RChatException(Reason.GIVEN_BAD_CHANNEL);
                }
            }

            if (username != null && this.channel$chan != null) {
                this.username$user = this.channel$chan.getUsers().stream()
                        .filter(u -> u.getUsername().equals(username))
                        .findFirst().orElse(null);
                if (this.username$user == null) {
                    throw new RChatException(Reason.GIVEN_BAD_USERNAME);
                }

                if (!this.channel$chan.getUsers().contains(this.session$user)) {
                    throw new RChatException(Reason.NO_PERMISSION);
                }
            }

            if (needAdmin && this.channel$chan != null && this.session$user != null) {
                if (this.channel$chan.getAdmins().contains(this.session$user.getUsername())) {
                    throw new RChatException(Reason.NO_PERMISSION);
                }
            }

        }
    }

    // checks if:
    // - session is proper (GIVEN_BAD_SESSION)
    // - channel is proper (GIVEN_BAD_CHANNEL)
    // - username$user is on channel$chan (GIVEN_BAD_USERNAME)
    // - session$user is on channel$chan (NO_PERMISSION)
    // - session$user is admin on channel$chan (NO_PERMISSION)
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
