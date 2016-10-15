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
            User exUser = new User(session, username);
            sessionToUser.put(username, exUser);
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
            User exUser = sessionToUser.get(session);
            if (exUser == null) {
                throw new RChatException(Reason.GIVEN_BAD_SESSION);
            }

            for (Channel chan : exUser.getChannels()) {
                boolean remove = chan.getUsers().remove(exUser);

                if (remove) {
                    for (User cu : chan.getUsers()) {
                        cu.getNews().offer(
                                new WhatsUp(What.PART, chan.getName(), exUser.getUsername())
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
    public Response<String> join(String session, String channel, @Nullable String password) throws RChatException {
        Lock lock$u = locks.get("U$" + sessionToUsername(session));
        Lock lock$c = locks.get("C$" + channel);
        lock$u.lock();
        lock$c.lock();

        try {
            User exUser = sessionToUser.get(session);
            if (exUser == null) {
                throw new RChatException(Reason.GIVEN_BAD_SESSION);
            }

            Channel chan = channelByName.get(channel);
            if (chan == null) {
                throw new RChatException(Reason.GIVEN_BAD_CHANNEL);
            }

            if (!Objects.equals(chan.getPassword(), password)) {
                throw new RChatException(Reason.GIVEN_BAD_PASSWORD);
            }

            if (chan.getBanned().contains(exUser.getUsername())) {
                throw new RChatException(Reason.UNWELCOME_BANNED);
            }

            boolean add = exUser.getChannels().addIfAbsent(chan);
            chan.getUsers().addIfAbsent(exUser);

            if (add) {
                for (User cu : chan.getUsers()) {
                    cu.getNews().offer(
                            new WhatsUp(What.JOIN, chan.getName(), exUser.getUsername())
                    );
                }
            }

            return Response.Ok(chan.getTopic());

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
            User exUser = sessionToUser.get(session);
            if (exUser == null) {
                throw new RChatException(Reason.GIVEN_BAD_SESSION);
            }

            Channel chan = channelByName.get(channel);
            if (chan == null) {
                throw new RChatException(Reason.GIVEN_BAD_CHANNEL);
            }

            boolean remove = exUser.getChannels().remove(chan);
            chan.getUsers().remove(exUser);

            if (remove) {
                for (User cu : chan.getUsers()) {
                    cu.getNews().offer(
                            new WhatsUp(What.PART, chan.getName(), exUser.getUsername())
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
        Lock lock$p = locks.get("U$" + username);
        Lock lock$c = locks.get("C$" + channel);
        lock$u.lock();
        lock$p.lock();
        lock$c.lock();

        try {
            User exUser = sessionToUser.get(session);
            if (exUser == null) {
                throw new RChatException(Reason.GIVEN_BAD_SESSION);
            }

            Channel chan = channelByName.get(channel);
            if (chan == null) {
                throw new RChatException(Reason.GIVEN_BAD_CHANNEL);
            }

            CopyOnWriteArrayList<User> chanUsers = chan.getUsers();
            Optional<User> oPaUser = chanUsers.stream()
                    .filter(u -> u.getUsername().equals(username))
                    .findFirst();
            if (!oPaUser.isPresent()) {
                throw new RChatException(Reason.GIVEN_BAD_USERNAME);
            }

            User paUser = oPaUser.get();

            if (!chanUsers.contains(paUser)) {
                throw new RChatException(Reason.GIVEN_BAD_USERNAME);
            }

            if (!chanUsers.contains(exUser)) {
                throw new RChatException(Reason.NO_PERMISSION);
            }

            if (!chan.getAdmins().contains(exUser.getUsername())) {
                throw new RChatException(Reason.NO_PERMISSION);
            }

            chanUsers.remove(paUser);

            for (User cu : chanUsers) {
                cu.getNews().offer(
                        new WhatsUp(What.KICK, chan.getName(), paUser.getUsername(), exUser.getUsername())
                );
            }

            return Response.Ok();

        } finally {
            lock$c.unlock();
            lock$p.unlock();
            lock$u.unlock();
        }
    }

    @Override
    public Response ban(String session, String channel, String username, boolean state) throws RChatException {
        Lock lock$u = locks.get("U$" + sessionToUsername(session));
        Lock lock$p = locks.get("U$" + username);
        Lock lock$c = locks.get("C$" + channel);
        lock$u.lock();
        lock$p.lock();
        lock$c.lock();

        try {
            User exUser = sessionToUser.get(session);
            if (exUser == null) {
                throw new RChatException(Reason.GIVEN_BAD_SESSION);
            }

            Channel chan = channelByName.get(channel);
            if (chan == null) {
                throw new RChatException(Reason.GIVEN_BAD_CHANNEL);
            }

            CopyOnWriteArrayList<User> chanUsers = chan.getUsers();
            Optional<User> oPaUser = chanUsers.stream()
                    .filter(u -> u.getUsername().equals(username))
                    .findFirst();
            if (!oPaUser.isPresent()) {
                throw new RChatException(Reason.GIVEN_BAD_USERNAME);
            }

            User paUser = oPaUser.get();

            if (!chanUsers.contains(paUser)) {
                throw new RChatException(Reason.GIVEN_BAD_USERNAME);
            }

            if (!chanUsers.contains(exUser)) {
                throw new RChatException(Reason.NO_PERMISSION);
            }

            if (!chan.getAdmins().contains(exUser.getUsername())) {
                throw new RChatException(Reason.NO_PERMISSION);
            }

            boolean change = state
                    ? chan.getBanned().addIfAbsent(paUser.getUsername())
                    : chan.getBanned().remove(paUser.getUsername());

            if (change) {
                for (User cu : chanUsers) {
                    cu.getNews().offer(
                            new WhatsUp(
                                    What.BAN, chan.getName(), paUser.getUsername(), exUser.getUsername(),
                                    state ? "ON" : "OFF")
                    );
                }
            }

            return Response.Ok();

        } finally {
            lock$c.unlock();
            lock$p.unlock();
            lock$u.unlock();
        }
    }

    @Override
    @Blocking(user = false, chan = false)
    public Response<ChannelUser[]> names(String session, String channel) throws RChatException {
        User exUser = sessionToUser.get(session);
        if (exUser == null) {
            throw new RChatException(Reason.GIVEN_BAD_SESSION);
        }

        Channel chan = channelByName.get(channel);
        if (chan == null) {
            throw new RChatException(Reason.GIVEN_BAD_CHANNEL);
        }

        if (!chan.getUsers().contains(exUser)) {
            throw new RChatException(Reason.NO_PERMISSION);
        }

        ChannelUser[] channelUsers = chan.getUsers()
                .stream()
                .map(cUser -> new ChannelUser(
                        cUser.getUsername(),
                        accounts.containsKey(cUser.getUsername()),
                        exUser.getIgnored().contains(cUser.getUsername()),
                        chan.getAdmins().contains(cUser.getUsername())
                ))
                .toArray(ChannelUser[]::new);

        return Response.Ok(channelUsers);
    }

    @Override
    public Response topic(String session, String channel, String text) throws RChatException {
        Lock lock$u = locks.get("U$" + sessionToUsername(session));
        Lock lock$c = locks.get("C$" + channel);
        lock$u.lock();
        lock$c.lock();

        try {
            User exUser = sessionToUser.get(session);
            if (exUser == null) {
                throw new RChatException(Reason.GIVEN_BAD_SESSION);
            }

            Channel chan = channelByName.get(channel);
            if (chan == null) {
                throw new RChatException(Reason.GIVEN_BAD_CHANNEL);
            }

            if (!chan.getUsers().contains(exUser)) {
                throw new RChatException(Reason.NO_PERMISSION);
            }

            if (!chan.getAdmins().contains(exUser.getUsername())) {
                throw new RChatException(Reason.NO_PERMISSION);
            }

            chan.setTopic(text);

            for (User cu : chan.getUsers()) {
                cu.getNews().offer(
                        new WhatsUp(What.TOPIC, chan.getName(), exUser.getUsername(), text)
                );
            }

            return Response.Ok();

        } finally {
            lock$c.unlock();
            lock$u.unlock();
        }
    }

    @Override
    public Response admin(String session, String channel, String username, boolean state) throws RChatException {
        Lock lock$u = locks.get("U$" + sessionToUsername(session));
        Lock lock$p = locks.get("U$" + username);
        Lock lock$c = locks.get("C$" + channel);
        lock$u.lock();
        lock$p.lock();
        lock$c.lock();

        try {
            User exUser = sessionToUser.get(session);
            if (exUser == null) {
                throw new RChatException(Reason.GIVEN_BAD_SESSION);
            }

            Channel chan = channelByName.get(channel);
            if (chan == null) {
                throw new RChatException(Reason.GIVEN_BAD_CHANNEL);
            }

            CopyOnWriteArrayList<User> chanUsers = chan.getUsers();
            Optional<User> oPaUser = chanUsers.stream()
                    .filter(u -> u.getUsername().equals(username))
                    .findFirst();
            if (!oPaUser.isPresent()) {
                throw new RChatException(Reason.GIVEN_BAD_USERNAME);
            }

            User paUser = oPaUser.get();

            if (!chanUsers.contains(paUser)) {
                throw new RChatException(Reason.GIVEN_BAD_USERNAME);
            }

            if (!chanUsers.contains(exUser)) {
                throw new RChatException(Reason.NO_PERMISSION);
            }

            if (!chan.getAdmins().contains(exUser.getUsername())) {
                throw new RChatException(Reason.NO_PERMISSION);
            }

            boolean change = state
                    ? chan.getAdmins().addIfAbsent(paUser.getUsername())
                    : chan.getAdmins().remove(paUser.getUsername());

            if (change) {
                for (User cu : chanUsers) {
                    cu.getNews().offer(
                            new WhatsUp(
                                    What.ADMIN, chan.getName(), paUser.getUsername(), exUser.getUsername(),
                                    state ? "ON" : "OFF")
                    );
                }
            }

            return Response.Ok();

        } finally {
            lock$c.unlock();
            lock$p.unlock();
            lock$u.unlock();
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

    protected String sessionToUsername(String session) throws RChatException {
        User user = sessionToUser.get(session);

        if (user == null) {
            throw new RChatException(Reason.GIVEN_BAD_SESSION);
        }

        return user.getUsername();
    }
}
