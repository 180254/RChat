package pl.nn44.rchat.client;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.XmlRpcInvocationException;
import org.springframework.remoting.caucho.BurlapProxyFactoryBean;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;
import pl.nn44.rchat.protocol.ChatException;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.rchat.protocol.Response;
import pl.nn44.xmlrpc.AnyTypeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Properties;

public class ClientApp {

    private final Properties prop;

    public ClientApp() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream("app.properties");

        this.prop = new Properties();
        this.prop.load(stream);
    }

    public ChatService hessianClient() {
        HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
        factory.setServiceUrl(prop.getProperty("url.app") + prop.getProperty("url.hessian"));
        factory.setServiceInterface(ChatService.class);
        factory.afterPropertiesSet();
        return (ChatService) factory.getObject();
    }

    public ChatService burlapClient() {
        BurlapProxyFactoryBean factory = new BurlapProxyFactoryBean();
        factory.setServiceUrl(prop.getProperty("url.app") + prop.getProperty("url.burlap"));
        factory.setServiceInterface(ChatService.class);
        factory.afterPropertiesSet();
        return (ChatService) factory.getObject();
    }

    public ChatService xmlRpcClient() {
        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(prop.getProperty("url.app") + prop.getProperty("url.xml-rpc")));
            config.setEncoding(XmlRpcClientConfigImpl.UTF8_ENCODING);
            config.setEnabledForExceptions(true);
            config.setEnabledForExtensions(true); // required by enabledForExceptions

            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            client.setTypeFactory(new AnyTypeFactory(client));

            InvocationHandler invHandler = (proxy, method, args) -> {
                try {
                    return client.execute("ChatService." + method.getName(), args);
                } catch (XmlRpcInvocationException e) {
                    throw e.getCause() != null
                            ? e.getCause()
                            : e;
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

    public static void main(String[] args) throws IOException, XmlRpcException, ChatException {
        ClientApp clientApp = new ClientApp();

        ChatService hessianClient = clientApp.hessianClient();
        ChatService burlapClient = clientApp.burlapClient();
        ChatService xmlRpcClient = clientApp.xmlRpcClient();

        System.out.println(LocalDateTime.now());

        System.out.println(xmlRpcClient.test(false));
        System.out.println(xmlRpcClient.test(true));

        Response<String> session = hessianClient.login("somebody", null);
        String payload = session.getPayload();

        System.out.println(xmlRpcClient.join(payload, "standard", null));
        System.out.println(xmlRpcClient.message(payload, "standard", "y3"));

        // org.springframework.remoting.RemoteAccessException
        // org.apache.xmlrpc.XmlRpcException
        // pl.nn44.rchat.protocol.ChatException
    }
}
