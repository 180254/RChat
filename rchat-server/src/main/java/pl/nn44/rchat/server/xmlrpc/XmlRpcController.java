package pl.nn44.rchat.server.xmlrpc;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcErrorLogger;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RequestMapping("/")
public class XmlRpcController {

    private final XmlRpcServletServer server;

    public <T> XmlRpcController(String handlerName,
                                Class<T> clazz,
                                T instance,
                                int maxThreads)
            throws XmlRpcException {

        XmlRpcServerConfigImpl config = new XmlRpcServerConfigImpl();
        config.setBasicEncoding(XmlRpcServerConfigImpl.UTF8_ENCODING);
        config.setEnabledForExceptions(true);
        config.setEnabledForExtensions(true);
        config.setKeepAliveEnabled(true);

        server = new XmlRpcServletServer();
        server.setConfig(config);
        server.setErrorLogger(new XmlRpcErrorLogger());
        server.setMaxThreads(maxThreads);

        PropertyHandlerMapping handlerMapping = new PropertyHandlerMapping();
        handlerMapping.setRequestProcessorFactoryFactory(pClass -> pRequest -> instance);
        handlerMapping.addHandler(handlerName, clazz);

        server.setHandlerMapping(handlerMapping);
    }

    @RequestMapping(value = "/xml-rpc", method = RequestMethod.POST)
    public void serve(HttpServletRequest request, HttpServletResponse response)
            throws XmlRpcException {
        try {
            server.execute(request, response);
        } catch (Exception e) {
            throw new XmlRpcException(e.getMessage(), e);
        }
    }
}
