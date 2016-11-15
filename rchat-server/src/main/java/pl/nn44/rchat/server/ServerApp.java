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
import pl.nn44.rchat.server.aspect.AsLogger;
import pl.nn44.rchat.server.impl.BestChatService;
import pl.nn44.rchat.server.impl.RpcProviders;
import pl.nn44.rchat.server.page.PlainErrorController;
import pl.nn44.rchat.server.page.PlainPageController;

import javax.servlet.Filter;

@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ServerApp {

    public static void main(String[] args) {
        SpringApplication.run(ServerApp.class, args);
    }

    @Bean
    public ChatService chatService() {
        return new BestChatService();
    }

    @Bean
    public RpcProviders<ChatService> rpcProviders(ChatService cs) {
        return new RpcProviders<>(cs, ChatService.class);
    }

    @Bean(name = "/hessian")
    public HttpRequestHandler hessianRpc(RpcProviders<ChatService> rp) {
        return rp.hessian();
    }

    @Bean(name = "/burlap")
    public HttpRequestHandler burlapRpc(RpcProviders<ChatService> rp) {
        return rp.burlap();
    }

    @Bean(name = "/xml-rpc")
    public HttpRequestHandler xmlRpcRpc(RpcProviders<ChatService> rp) throws XmlRpcException {
        return rp.xmlRpc();
    }

    @Bean
    public PlainPageController pageController() {
        return new PlainPageController();
    }

    @Bean
    public ErrorController errorController() {
        return new PlainErrorController();
    }

    @Bean
    public AsLogger asLogger() {
        return new AsLogger();
    }

    @Bean
    public Filter mdcInsertingServletFilter() {
        return new MDCInsertingServletFilter();
    }
}
