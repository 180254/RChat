package pl.nn44.rchat.client;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.XmlRpcInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.caucho.BurlapProxyFactoryBean;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.xmlrpc.AnyTypeFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class Clients {

    private static final Logger LOG = LoggerFactory.getLogger(Clients.class);
    private final Properties prop;

    public Clients(Properties prop) {
        this.prop = prop;
    }

    public ChatService hessianClient() {
        String serviceUrl = prop.getProperty("url.app") + prop.getProperty("url.hessian");

        HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
        factory.setServiceUrl(serviceUrl);
        factory.setServiceInterface(ChatService.class);
        factory.afterPropertiesSet();

        ChatService chatService = (ChatService) factory.getObject();

        LOG.info("HessianClient instance created.");
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

        LOG.info("BurlapClient instance created.");
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

            InvocationHandler invHandler = (proxy, method, args) -> {
                try {
                    return method.getName().equals("toString") && args == null
                            ? "XmlRpcProxy[" + config.getServerURL() + "]"
                            : client.execute("ChatService." + method.getName(), args);

                } catch (XmlRpcInvocationException e) {
                    throw e.getCause() != null ? e.getCause() : e;
                }
            };

            ChatService chatService = (ChatService)
                    Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class<?>[]{ChatService.class},
                            invHandler
                    );

            LOG.info("XML-RPC-Client instance created.");
            return chatService;

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
