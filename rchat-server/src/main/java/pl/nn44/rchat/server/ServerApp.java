package pl.nn44.rchat.server;

import ch.qos.logback.classic.helpers.MDCInsertingServletFilter;
import org.apache.xmlrpc.XmlRpcException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.HttpRequestHandler;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.rchat.protocol.xmlrpc.FaultMapperImpl;
import pl.nn44.rchat.server.aspect.AsLogger;
import pl.nn44.rchat.server.impl.BestChatService;
import pl.nn44.rchat.server.impl.Endpoints;
import pl.nn44.rchat.server.page.PlainErrorController;
import pl.nn44.rchat.server.page.PlainPageController;
import pl.nn44.xmlrpc.FaultMapper;

import javax.servlet.Filter;

@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ServerApp {

    public static void main(String[] args) {
        SpringApplication.run(ServerApp.class, args);
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Bean
    public ChatService chatService() {
        return new BestChatService();
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Bean
    public Endpoints<ChatService> endpoints(ChatService cs) {
        return new Endpoints<>(cs, ChatService.class);
    }

    @Bean
    public FaultMapper faultMapper() {
        return new FaultMapperImpl();
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Bean(name = "/hessian")
    public HttpRequestHandler hessianRpc(Endpoints<?> ep) {
        return ep.hessian();
    }

    @Bean(name = "/burlap")
    public HttpRequestHandler burlapRpc(Endpoints<?> ep) {
        return ep.burlap();
    }

    @Bean(name = "/xml-rpc")
    public HttpRequestHandler xmlRpcRpc(Endpoints<?> ep, FaultMapper fm) throws XmlRpcException {
        return ep.xmlRpc(fm);
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Bean
    public PlainPageController pageController() {
        return new PlainPageController();
    }

    @Bean
    public ErrorController errorController() {
        return new PlainErrorController();
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Bean
    public AsLogger asLogger() {
        return new AsLogger();
    }

    @Bean
    public Filter mdcInsertingServletFilter() {
        return new MDCInsertingServletFilter();
    }
}
