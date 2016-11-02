package pl.nn44.rchat.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.client.Clients.Cs;
import pl.nn44.rchat.protocol.ChatException;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.rchat.protocol.Response;

import java.util.Properties;

public class CsHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CsHandler.class);

    private final ChatService[] chatServices = new ChatService[3];
    private int current = 0;

    public CsHandler() {
        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    public void init() {
        Properties prop = PropLoader.get();
        Clients clients = new Clients(prop);

        this.chatServices[Cs.Hessian.i()] = clients.hessianClient();
        this.chatServices[Cs.Burlap.i()] = clients.burlapClient();
        this.chatServices[Cs.XmlRpc.i()] = clients.xmlRpcClient();
    }

    public void runTest() {
        for (int i = 0; i < chatServices.length; i++) {

            try {
                Response<?> test = chatServices[i].test(false);
                LOG.debug("ChatService.test.a({})=OK;   {}", i, test);
            } catch (Exception e) {
                LOG.error("ChatService.test.a({})=FAIL; {}", i, e.toString());
            }

            try {
                Response<?> test = chatServices[i].test(true);
                LOG.error("ChatService.test.a({})=FAIL; {}", i, test);
            } catch (ChatException e) {
                LOG.debug("ChatService.test.b({})=OK;   {}", i, e.toString());
            } catch (Exception e) {
                LOG.error("ChatService.test.a({})=FAIL; {}", i, e.toString());
            }
        }
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public ChatService getCs() {
        return chatServices[current];
    }
}
