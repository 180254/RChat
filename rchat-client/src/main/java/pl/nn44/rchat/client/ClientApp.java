package pl.nn44.rchat.client;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.springframework.remoting.caucho.BurlapProxyFactoryBean;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;
import pl.nn44.rchat.protocol.ChatService;

import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;

public class ClientApp {

    public static String APP_URL = "http://localhost:8080";

    public ChatService hessianClient() {
        HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
        factory.setServiceUrl(APP_URL + "/hessian");
        factory.setServiceInterface(ChatService.class);
        factory.afterPropertiesSet();
        return (ChatService) factory.getObject();
    }

    public ChatService burlapClient() {
        BurlapProxyFactoryBean factory = new BurlapProxyFactoryBean();
        factory.setServiceUrl(APP_URL + "/burlap");
        factory.setServiceInterface(ChatService.class);
        factory.afterPropertiesSet();
        return (ChatService) factory.getObject();
    }

    public ChatService xmlRpcClient() {
        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(APP_URL + "/xml-rpc"));
            config.setEnabledForExceptions(true);
            config.setEnabledForExtensions(true);
            config.setEncoding(XmlRpcClientConfigImpl.UTF8_ENCODING);

            XmlRpcClient xr = new XmlRpcClient();
            xr.setConfig(config);

            return (ChatService)
                    Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class<?>[]{ChatService.class},
                            (proxy, method, args) -> xr.execute("ChatService." + method.getName(), args)
                    );

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws MalformedURLException, XmlRpcException {
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
    }
}
