package pl.nn44.rchat.client;

import org.springframework.remoting.caucho.BurlapProxyFactoryBean;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;
import pl.nn44.rchat.protocol.ChatService;

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

    public static void main(String[] args) {
        ClientApp clientApp = new ClientApp();

        ChatService hessianClient = clientApp.hessianClient();
        ChatService burlapClient = clientApp.burlapClient();
        System.out.println(LocalDateTime.now());

        System.out.println(hessianClient.message("x", "y"));
        System.out.println(LocalDateTime.now());

        System.out.println(burlapClient.whatsUp(2000));
        System.out.println(LocalDateTime.now());
    }
}
