package pl.nn44.rchat.server.impl;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.metadata.XmlRpcSystemImpl;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcErrorLogger;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.caucho.BurlapServiceExporter;
import org.springframework.remoting.caucho.HessianServiceExporter;
import org.springframework.web.HttpRequestHandler;
import pl.nn44.xmlrpc.AnyTypeFactory;

public class RpcProvider<T> {

    private static final Logger LOG = LoggerFactory.getLogger(RpcProvider.class);

    private final T service;
    private final Class<T> clazz;

    public RpcProvider(T service, Class<T> clazz) {
        this.service = service;
        this.clazz = clazz;
    }

    public HttpRequestHandler hessian() {
        HessianServiceExporter exporter = new HessianServiceExporter();
        exporter.setService(service);
        exporter.setServiceInterface(clazz);

        LOG.info("hessian provider created.");
        return exporter;
    }

    public HttpRequestHandler burlap() {
        // noinspection deprecation
        BurlapServiceExporter exporter = new BurlapServiceExporter();
        exporter.setService(service);
        exporter.setServiceInterface(clazz);

        LOG.info("burlap provider created.");
        return exporter;
    }

    public HttpRequestHandler xmlRpc() throws XmlRpcException {
        XmlRpcServerConfigImpl config = new XmlRpcServerConfigImpl();
        config.setEncoding(XmlRpcServerConfigImpl.UTF8_ENCODING);
        config.setEnabledForExceptions(true);
        config.setEnabledForExtensions(true); // required by enabledForExceptions
        config.setKeepAliveEnabled(true);

        PropertyHandlerMapping handlerMapping = new PropertyHandlerMapping();
        handlerMapping.setRequestProcessorFactoryFactory(pClass -> pRequest -> service);
        handlerMapping.addHandler(clazz.getSimpleName(), clazz);
        XmlRpcSystemImpl.addSystemHandler(handlerMapping);

        XmlRpcServletServer server = new XmlRpcServletServer();
        server.setConfig(config);
        server.setErrorLogger(new XmlRpcErrorLogger());
        server.setHandlerMapping(handlerMapping);
        server.setTypeFactory(new AnyTypeFactory(server));

        LOG.info("xml-rpc provider created.");
        return server::execute;
    }
}
