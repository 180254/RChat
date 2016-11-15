package pl.nn44.rchat.server.impl;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.metadata.XmlRpcSystemImpl;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcErrorLogger;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;
import org.springframework.remoting.caucho.BurlapServiceExporter;
import org.springframework.remoting.caucho.HessianServiceExporter;
import org.springframework.web.HttpRequestHandler;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.xmlrpc.AnyTypeFactory;

public class RpcProviders {

    private final ChatService cs;

    public RpcProviders(ChatService cs) {
        this.cs = cs;
    }

    public HttpRequestHandler hessian() {
        HessianServiceExporter exporter = new HessianServiceExporter();
        exporter.setService(cs);
        exporter.setServiceInterface(ChatService.class);
        return exporter;
    }

    public HttpRequestHandler burlap() {
        // noinspection deprecation
        BurlapServiceExporter exporter = new BurlapServiceExporter();
        exporter.setService(cs);
        exporter.setServiceInterface(ChatService.class);
        return exporter;
    }

    public HttpRequestHandler xmlRpc() throws XmlRpcException {
        XmlRpcServerConfigImpl config = new XmlRpcServerConfigImpl();
        config.setEncoding(XmlRpcServerConfigImpl.UTF8_ENCODING);
        config.setEnabledForExceptions(true);
        config.setEnabledForExtensions(true); // required by enabledForExceptions
        config.setKeepAliveEnabled(true);

        PropertyHandlerMapping handlerMapping = new PropertyHandlerMapping();
        handlerMapping.setRequestProcessorFactoryFactory(pClass -> pRequest -> cs);
        handlerMapping.addHandler("ChatService", ChatService.class);
        XmlRpcSystemImpl.addSystemHandler(handlerMapping);

        XmlRpcServletServer server = new XmlRpcServletServer();
        server.setConfig(config);
        server.setErrorLogger(new XmlRpcErrorLogger());
        server.setHandlerMapping(handlerMapping);
        server.setTypeFactory(new AnyTypeFactory(server));

        return server::execute;
    }
}
