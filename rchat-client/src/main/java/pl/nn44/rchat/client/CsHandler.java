package pl.nn44.rchat.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.protocol.ChatException;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.rchat.protocol.Response;

import java.util.Properties;
import java.util.concurrent.ExecutorService;

public class CsHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CsHandler.class);

    private final ChatService[] chatServices = new ChatService[3];
    private final ExecutorService executor;
    private int current = 0;

    public CsHandler(ExecutorService executor) {
        this.executor = executor;
        LOG.info("CsHandler instance created.");
    }

    public void init() {
        Properties prop = PropLoader.get();
        Clients clients = new Clients(prop);

        try {
            this.chatServices[0] = clients.hessianClient();
        } catch (Exception e) {
            LOG.error("HessianClient creation fail.", e);
        }

        try {
            this.chatServices[1] = clients.burlapClient();
        } catch (Exception e) {
            LOG.error("BurlapClient creation fail.", e);
        }

        try {
            this.chatServices[2] = clients.xmlRpcClient();
        } catch (Exception e) {
            LOG.error("XML-RPC-Client creation fail.", e);
        }
    }

    public void runTestAsync() {
        executor.submit(() -> {
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
        });
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
