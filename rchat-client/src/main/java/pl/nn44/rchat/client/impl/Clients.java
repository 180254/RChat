package pl.nn44.rchat.client.impl;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.util.ClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.caucho.BurlapProxyFactoryBean;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.xmlrpc.AnyTypeFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class Clients {

    private static final Logger LOG = LoggerFactory.getLogger(Clients.class);
    private final Properties prop;

    public Clients(Properties prop) {
        this.prop = prop;
        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    public ChatService hessianClient() {
        String serviceUrl = prop.getProperty("url.app") + prop.getProperty("url.hessian");

        HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
        factory.setServiceUrl(serviceUrl);
        factory.setServiceInterface(ChatService.class);
        factory.afterPropertiesSet();

        ChatService chatService = (ChatService) factory.getObject();

        LOG.debug("HessianClient instance created.");
        return chatService;
    }

    public ChatService burlapClient() {
        String serviceUrl = prop.getProperty("url.app") + prop.getProperty("url.burlap");

        // noinspection deprecation
        BurlapProxyFactoryBean factory = new BurlapProxyFactoryBean();
        factory.setServiceUrl(serviceUrl);
        factory.setServiceInterface(ChatService.class);
        factory.afterPropertiesSet();

        ChatService chatService = (ChatService) factory.getObject();

        LOG.debug("BurlapClient instance created.");
        return chatService;
    }

    public ChatService xmlRpcClient() {
        String serviceUrl = prop.getProperty("url.app") + prop.getProperty("url.xml-rpc");

        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(serviceUrl));
            config.setEncoding(XmlRpcClientConfigImpl.UTF8_ENCODING);
            config.setEnabledForExceptions(true);
            config.setEnabledForExtensions(true); // required by enabledForExceptions

            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            client.setTypeFactory(new AnyTypeFactory(client));

            ClientFactory clientFactory = new ClientFactory(client);
            ChatService chatService = (ChatService)
                    clientFactory.newInstance(
                            Thread.currentThread().getContextClassLoader(),
                            ChatService.class,
                            "ChatService"
                    );

            LOG.debug("XmlRpcClient instance created.");
            return chatService;

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    public enum Cs {

        Hessian,
        Burlap,
        XmlRpc;

        public int i() {
            return ordinal();
        }

        public static Cs byIndex(int i) {
            return Cs.values()[i];
        }
    }
}
