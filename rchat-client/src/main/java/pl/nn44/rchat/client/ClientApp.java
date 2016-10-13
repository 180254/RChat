package pl.nn44.rchat.client;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.springframework.remoting.caucho.BurlapProxyFactoryBean;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;
import pl.nn44.rchat.client.xmlrpc.XmlRpcChatService;
import pl.nn44.rchat.protocol.ChatService;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;

public class ClientApp {

    public ChatService hessianClient() {
        HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
        factory.setServiceUrl("http://localhost:8080/hessian");
        factory.setServiceInterface(ChatService.class);
        factory.afterPropertiesSet();
        return (ChatService) factory.getObject();
    }

    public ChatService burlapClient() {
        BurlapProxyFactoryBean factory = new BurlapProxyFactoryBean();
        factory.setServiceUrl("http://localhost:8080/burlap");
        factory.setServiceInterface(ChatService.class);
        factory.afterPropertiesSet();
        return (ChatService) factory.getObject();
    }

    public ChatService xmlRpcClient() {
        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL("http://localhost:8080/xml-rpc"));
            config.setEnabledForExceptions(true);
            config.setEnabledForExtensions(true);
            config.setEncoding(XmlRpcClientConfigImpl.UTF8_ENCODING);

            XmlRpcClient xr = new XmlRpcClient();
            xr.setConfig(config);

            return new XmlRpcChatService(xr);
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

        System.out.println(hessianClient.message("x", "y"));
        System.out.println(LocalDateTime.now());

        System.out.println(burlapClient.message("x", "y"));
        System.out.println(LocalDateTime.now());

        System.out.println(xmlRpcClient.message("x", "y"));
        System.out.println(LocalDateTime.now());

        System.out.println(burlapClient.whatsUp(2000));
        System.out.println(LocalDateTime.now());
    }
}
