package pl.nn44.rchat.server;

import org.apache.xmlrpc.XmlRpcException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.caucho.BurlapServiceExporter;
import org.springframework.remoting.caucho.HessianServiceExporter;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.rchat.server.impl.ChatServiceImpl;
import pl.nn44.rchat.server.xmlrpc.XmlRpcController;

@Configuration
@EnableAutoConfiguration
public class ServerApp {

    public static void main(String[] args) {
        SpringApplication.run(ServerApp.class, args);
    }

    @Bean
    public ChatService chatService() {
        return new ChatServiceImpl();
    }

    @Bean(name = "/hessian")
    public HessianServiceExporter hessianService() {
        HessianServiceExporter exporter = new HessianServiceExporter();
        exporter.setService(chatService());
        exporter.setServiceInterface(ChatService.class);
        return exporter;
    }

    @Bean(name = "/burlap")
    public BurlapServiceExporter burlapService() {
        BurlapServiceExporter exporter = new BurlapServiceExporter();
        exporter.setService(chatService());
        exporter.setServiceInterface(ChatService.class);
        return exporter;
    }

    @Bean
    public XmlRpcController xmlRpcController() throws XmlRpcException {
        return new XmlRpcController("ChatService", ChatService.class, chatService(), 5);
    }
}
