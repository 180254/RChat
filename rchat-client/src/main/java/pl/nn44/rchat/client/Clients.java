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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class Clients {

    static Logger LOG = LoggerFactory.getLogger(Clients.class);

    private final Properties prop;

    public Clients() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream("app.properties");

        this.prop = new Properties();
        this.prop.load(stream);
    }

    public ChatService hessianClient() {
        LOG.debug("HessianClient instance created.");
        String serviceUrl = prop.getProperty("url.app") + prop.getProperty("url.hessian");

        HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
        factory.setServiceUrl(serviceUrl);
        factory.setServiceInterface(ChatService.class);
        factory.afterPropertiesSet();
        return (ChatService) factory.getObject();
    }

    public ChatService burlapClient() {
        LOG.debug("BurlapClient instance created.");
        String serviceUrl = prop.getProperty("url.app") + prop.getProperty("url.burlap");

        BurlapProxyFactoryBean factory = new BurlapProxyFactoryBean();
        factory.setServiceUrl(serviceUrl);
        factory.setServiceInterface(ChatService.class);
        factory.afterPropertiesSet();
        return (ChatService) factory.getObject();
    }

    public ChatService xmlRpcClient() {
        LOG.debug("XML-RPC-Client instance created.");
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

            InvocationHandler invHandler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    try {
                        return method.getName().equals("toString") && args == null
                                ? "XmlRpcProxy[" + config.getServerURL() + "]"
                                : client.execute("ChatService." + method.getName(), args);
                    } catch (XmlRpcInvocationException e) {
                        throw e.getCause() != null
                                ? e.getCause()
                                : e;
                    }
                }
            };

            return (ChatService)
                    Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class<?>[]{ChatService.class},
                            invHandler
                    );

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
