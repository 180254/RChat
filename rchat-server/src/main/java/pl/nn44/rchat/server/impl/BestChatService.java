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
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;

public class BestChatService implements ChatService {

    Random random = new SecureRandom();
    Iterator<String> idGenerator = BigIdGenerator.bits(random, 128);

    ConcurrentMap<String, String> accounts/*username/password*/ = new ConcurrentHashMap<>();
    ConcurrentMap<String, User> sessionToUser = new ConcurrentHashMap<>();
    ConcurrentMap<String, Channel> channelByName = new ConcurrentHashMap<>();

    Striped<Lock> locks = Striped.lazyWeakLock(100);

    @Override
    @Blocking(user = true, chan = false)
    public Response<String> login(String username, @Nullable String password) throws RChatException {
        Lock lock$u = locks.get("U$" + username);
        lock$u.lock();

        try {
            if (sessionToUser.containsValue(User.Dummy(username))) {
                throw new RChatException(Reason.ALREADY_LOGGED_IN);
            }

            if (!Objects.equals(accounts.get(username), password)) {
                throw new RChatException(Reason.GIVEN_BAD_PASSWORD);
            }

            String session = idGenerator.next();
            User user = new User(session, username);
            sessionToUser.put(username, user);
            return Response.Ok(session);

        } finally {
            lock$u.unlock();
        }
    }

    @Override
    @Blocking(user = true, chan = false)
    public Response logout(String session) throws RChatException {
        Lock lock$u = locks.get("U$" + sessionToUsername(session));
        lock$u.lock();

        try {
            User user = sessionToUser.get(session);
            if (user == null) {
                throw new RChatException(Reason.GIVEN_BAD_SESSION);
            }

            for (Channel chan : user.getChannels()) {
                boolean remove = chan.getUsers().remove(user);

                if (remove) {
                    for (User cu : chan.getUsers()) {
                        cu.getNews().offer(
                                new WhatsUp(What.PART, chan.getName(), user.getUsername())
                        );
                    }
                }
            }

            sessionToUser.remove(session);
            return Response.Ok();

        } finally {
            lock$u.unlock();
        }
    }

    @Override
    @Blocking(user = true, chan = true)
    public Response join(String session, String channel, @Nullable String password) throws RChatException {
        Lock lock$u = locks.get("U$" + sessionToUsername(session));
        Lock lock$c = locks.get("C$" + channel);
        lock$u.lock();
        lock$c.lock();

        try {
            User user = sessionToUser.get(session);
            if (user == null) {
                throw new RChatException(Reason.GIVEN_BAD_SESSION);
            }

            Channel chan = channelByName.get(channel);
            if (chan == null) {
                throw new RChatException(Reason.GIVEN_BAD_CHANNEL);
            }

            if (!Objects.equals(chan.getPassword(), password)) {
                throw new RChatException(Reason.GIVEN_BAD_PASSWORD);
            }

            boolean add = user.getChannels().addIfAbsent(chan);
            chan.getUsers().addIfAbsent(user);

            if (add) {
                for (User cu : chan.getUsers()) {
                    cu.getNews().offer(
                            new WhatsUp(What.JOIN, chan.getName(), user.getUsername())
                    );
                }
            }

            return Response.Ok();

        } finally {
            lock$c.unlock();
            lock$u.unlock();
        }
    }

    @Override
    @Blocking(user = true, chan = true)
    public Response part(String session, String channel) throws RChatException {
        Lock lock$u = locks.get("U$" + sessionToUsername(session));
        Lock lock$c = locks.get("C$" + channel);
        lock$u.lock();
        lock$c.lock();

        try {
            User user = sessionToUser.get(session);
            if (user == null) {
                throw new RChatException(Reason.GIVEN_BAD_SESSION);
            }

            Channel chan = channelByName.get(channel);
            if (chan == null) {
                throw new RChatException(Reason.GIVEN_BAD_CHANNEL);
            }

            boolean remove = user.getChannels().remove(chan);
            chan.getUsers().remove(user);

            if (remove) {
                for (User cu : chan.getUsers()) {
                    cu.getNews().offer(
                            new WhatsUp(What.PART, chan.getName(), user.getUsername())
                    );
                }
            }

            return Response.Ok();

        } finally {
            lock$c.unlock();
            lock$u.unlock();
        }
    }

    @Override
    @Blocking(user = true, chan = true)
    public Response kick(String session, String channel, String username) throws RChatException {
        Lock lock$u = locks.get("U$" + sessionToUsername(session));
        Lock lock$k = locks.get("U$" + username);
        Lock lock$c = locks.get("C$" + channel);
        lock$u.lock();
        lock$k.lock();
        lock$c.lock();

        try {
            User user = sessionToUser.get(session);
            if (user == null) {
                throw new RChatException(Reason.GIVEN_BAD_SESSION);
            }

            Channel chan = channelByName.get(channel);
            if (chan == null) {
                throw new RChatException(Reason.GIVEN_BAD_CHANNEL);
            }

            CopyOnWriteArrayList<User> chanUsers = chan.getUsers();
            Optional<User> oKick = chanUsers.stream()
                    .filter(u -> u.getUsername().equals(username))
                    .findFirst();
            if (!oKick.isPresent()) {
                throw new RChatException(Reason.GIVEN_BAD_USERNAME);
            }

            User kick = oKick.get();

            if (!chanUsers.contains(kick)) {
                throw new RChatException(Reason.GIVEN_BAD_USERNAME);
            }

            if (!chanUsers.contains(user)) {
                throw new RChatException(Reason.NO_PERMISSION);
            }

            if (!chan.getAdmins().contains(user.getUsername())) {
                throw new RChatException(Reason.NO_PERMISSION);
            }

            chanUsers.remove(kick);

            for (User cu : chanUsers) {
                cu.getNews().offer(
                        new WhatsUp(What.KICK, chan.getName(), kick.getUsername(), user.getUsername())
                );
            }

            return Response.Ok();

        } finally {
            lock$c.unlock();
            lock$k.unlock();
            lock$u.unlock();
        }
    }

    @Override
    public Response ban(String session, String channel, String username, boolean state) throws RChatException {
        return null;
    }

    @Override
    @Blocking(user = false, chan = false)
    public Response<ChannelUser[]> names(String session, String channel) throws RChatException {
        User user = sessionToUser.get(session);
        if (user == null) {
            throw new RChatException(Reason.GIVEN_BAD_SESSION);
        }

        Channel chan = channelByName.get(channel);
        if (chan == null) {
            throw new RChatException(Reason.GIVEN_BAD_CHANNEL);
        }

        if (!chan.getUsers().contains(user)) {
            throw new RChatException(Reason.NO_PERMISSION);
        }

        ChannelUser[] channelUsers = chan.getUsers()
                .stream()
                .map(cUser -> new ChannelUser(
                        cUser.getUsername(),
                        accounts.containsKey(cUser.getUsername()),
                        user.getIgnored().contains(cUser.getUsername()),
                        chan.getAdmins().contains(cUser.getUsername())
                ))
                .toArray(ChannelUser[]::new);

        return Response.Ok(channelUsers);
    }

    @Override
    public Response topic(String session, String channel) throws RChatException {
        return null;
    }

    @Override
    public Response topic(String session, String channel, String text) throws RChatException {
        return null;
    }

    @Override
    public Response admin(String session, String channel, String username, boolean state) throws RChatException {
        return null;
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

    protected String sessionToUsername(String session) throws RChatException {
        User user = sessionToUser.get(session);

        if (user == null) {
            throw new RChatException(Reason.GIVEN_BAD_SESSION);
        }

        return user.getUsername();
    }
}
