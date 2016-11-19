package pl.nn44.rchat.client.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.rchat.protocol.exception.ChatException;
import pl.nn44.rchat.protocol.model.Response;
import pl.nn44.rchat.protocol.xmlrpc.FaultMapperRevImpl;

import java.util.Properties;

public class CsHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CsHandler.class);

    private final ChatService[] chatServices = new ChatService[3];
    private String username = null;
    private String token = null;
    private int current = 0;

    // ---------------------------------------------------------------------------------------------------------------

    public CsHandler() {
        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void init() {
        Properties prop = PropLoader.get();
        Clients<ChatService> clients = new Clients<>(prop, ChatService.class);

        this.chatServices[Clients.Cs.Hessian.i()] = clients.hessian();
        this.chatServices[Clients.Cs.Burlap.i()] = clients.burlap();
        this.chatServices[Clients.Cs.XmlRpc.i()] = clients.xmlRpc(new FaultMapperRevImpl());
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void test() {
        for (int i = 0; i < chatServices.length; i++) {
            String csName = Clients.Cs.byIndex(i).name();

            try {
                Response<?> response = chatServices[i].test(false);
                LOG.info("ChatService({}).test(false): OK={}", csName, response);
            } catch (Exception e) {
                LOG.warn("ChatService({}).test(false): FAIL={}", csName, e.toString());
            }

            try {
                Response<?> response = chatServices[i].test(true);
                LOG.warn("ChatService({}).test(true): FAIL={}", csName, response);
            } catch (ChatException e) {
                LOG.info("ChatService({}).test(true): OK={}", csName, e.toString());
            } catch (Exception e) {
                LOG.warn("ChatService({}).test(true): FAIL={}", csName, e.toString());
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    public String token() {
        return token;
    }

    public int current() {
        return current;
    }

    public ChatService cs() {
        return chatServices[current];
    }

    // ---------------------------------------------------------------------------------------------------------------

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void setToken(String token) {
        this.token = token;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void logout() {
        try {
            String token = this.token;
            if (token != null) {
                this.token = null;
                cs().logout(token);
            }
        } catch (Exception e) {
            LOG.warn("logout()", e);
        }
    }
}
