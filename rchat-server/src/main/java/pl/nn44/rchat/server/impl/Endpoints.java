package pl.nn44.rchat.server.impl;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.metadata.XmlRpcSystemImpl;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcErrorLogger;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.caucho.BurlapServiceExporter;
import org.springframework.remoting.caucho.HessianServiceExporter;
import org.springframework.web.HttpRequestHandler;
import pl.nn44.xmlrpc.common.AnyTypeFactory;
import pl.nn44.xmlrpc.server.AnyXmlRpcServer;
import pl.nn44.xmlrpc.server.FaultMapper;

public class Endpoints<T> {

    private static final Logger LOG = LoggerFactory.getLogger(Endpoints.class);

    private final T service;
    private final Class<T> clazz;

    public Endpoints(T service, Class<T> clazz) {
        this.service = service;
        this.clazz = clazz;
    }

    public HttpRequestHandler hessian() {
        HessianServiceExporter exporter = new HessianServiceExporter();
        exporter.setService(service);
        exporter.setServiceInterface(clazz);

        LOG.info("hessian endpoint created.");
        return exporter;
    }

    public HttpRequestHandler burlap() {
        // noinspection deprecation
        BurlapServiceExporter exporter = new BurlapServiceExporter();
        exporter.setService(service);
        exporter.setServiceInterface(clazz);

        LOG.info("burlap endpoint created.");
        return exporter;
    }

    public HttpRequestHandler xmlRpc(FaultMapper faultMapper) throws XmlRpcException {
        // axe-180254 (rchat/apache-xmlrpc-extension) is used.
        // Comments contains alternative action to get rid of the extensions.

        XmlRpcServerConfigImpl config = new XmlRpcServerConfigImpl();
        config.setEncoding(XmlRpcServerConfigImpl.UTF8_ENCODING);
        config.setEnabledForExceptions(false); // !! [axe-180254 or [set true]]
        config.setEnabledForExtensions(false); // !! [axe-180254 or [set true]]
        config.setKeepAliveEnabled(true);

        PropertyHandlerMapping handlerMapping = new PropertyHandlerMapping();
        handlerMapping.setRequestProcessorFactoryFactory(pClass -> pRequest -> service);
        handlerMapping.addHandler(clazz.getSimpleName(), clazz);
        XmlRpcSystemImpl.addSystemHandler(handlerMapping);

        AnyXmlRpcServer server = new AnyXmlRpcServer(); // [axe-180254 or [use XmlRpcServletServer]]
        server.setConfig(config);
        server.setErrorLogger(new XmlRpcErrorLogger());
        server.setHandlerMapping(handlerMapping);
        server.setTypeFactory(new AnyTypeFactory(server)); // [axe-180254 or [remove statement]]
        server.setFaultMapper(faultMapper); // [axe-180254 or [remove statement]]

        LOG.info("xml-rpc endpoint created.");
        return server::execute;
    }
}
