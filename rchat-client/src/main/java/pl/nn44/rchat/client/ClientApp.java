package pl.nn44.rchat.client;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.XmlRpcInvocationException;
import org.springframework.remoting.caucho.BurlapProxyFactoryBean;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;
import pl.nn44.rchat.protocol.ChatException;
import pl.nn44.rchat.protocol.ChatService;

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
            config.setEnabledForExtensions(true);

            XmlRpcClient xr = new XmlRpcClient();
            xr.setConfig(config);

            InvocationHandler invHandler = (proxy, method, args) -> {
                try {
                    return xr.execute("ChatService." + method.getName(), args);
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

        System.out.println(hessianClient.message("any1", "x1", "y1"));
        System.out.println(LocalDateTime.now());

        System.out.println(burlapClient.message("any2", "x2", "y2"));
        System.out.println(LocalDateTime.now());

        System.out.println(xmlRpcClient.message("any3", "x3", "y3"));
        System.out.println(LocalDateTime.now());

        System.out.println(burlapClient.whatsUp("any4", 2000));
        System.out.println(LocalDateTime.now());

        try {
            hessianClient.privy("any1", "x1", "y1");
        } catch (ChatException e) {
            System.out.println("EXCEPTION EXPECTED <0>");
            System.out.println(LocalDateTime.now());
        }

        try {
            burlapClient.privy("any2", "x2", "y2");
        } catch (ChatException e) {
            System.out.println("EXCEPTION EXPECTED <1>");
            System.out.println(LocalDateTime.now());
        }

        try {
            xmlRpcClient.privy("any3", "x3", "y3");
        } catch (ChatException e) {
            System.out.println("EXCEPTION EXPECTED <2>");
            System.out.println(LocalDateTime.now());
        }

        // org.springframework.remoting.RemoteAccessException
        // org.apache.xmlrpc.XmlRpcException
        // pl.nn44.rchat.protocol.ChatException
    }
}
