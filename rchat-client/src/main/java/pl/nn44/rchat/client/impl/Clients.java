package pl.nn44.rchat.client.impl;

import com.caucho.burlap.client.BurlapProxyFactory;
import com.caucho.hessian.client.HessianConnection;
import com.caucho.hessian.client.HessianConnectionFactory;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianURLConnectionFactory;
import com.google.common.base.CharMatcher;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.TypeConverterFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.caucho.BurlapProxyFactoryBean;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;
import pl.nn44.xmlrpc.client.AnyXmlRpcTransport;
import pl.nn44.xmlrpc.client.ClientFactoryFix;
import pl.nn44.xmlrpc.client.FaultRevMapper;
import pl.nn44.xmlrpc.common.AnyTypeFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.function.Function;

public class Clients<T> {

    private static final Logger LOG = LoggerFactory.getLogger(Clients.class);

    private final Class<T> serviceInterface;
    private final Function<String, String> url;

    // ---------------------------------------------------------------------------------------------------------------

    public Clients(Properties prop, Class<T> clazz) {
        this.serviceInterface = clazz;

        this.url = (resource) -> MessageFormat.format(
                "http{0}://{1}:{2}/{3}",
                Boolean.parseBoolean(prop.getProperty("server.ssl")) ? "s" : "",
                prop.getProperty("server.ip"),
                prop.getProperty("server.port"),
                CharMatcher.is('/').trimLeadingFrom(prop.getProperty(resource))
        );

        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    // ---------------------------------------------------------------------------------------------------------------

    public T hessian() {
        String serviceUrl = url.apply("rpc.hessian");

        HessianProxyFactory hpf = new HessianProxyFactory();
        HessianConnectionFactory hcf = new HessianURLConnectionFactory() {

            @Override
            public HessianConnection open(URL url) throws IOException {
                HessianConnection hc = super.open(url);
                hc.addHeader("User-Agent", "CT-Hessian");
                return hc;
            }
        };

        hcf.setHessianProxyFactory(hpf);
        hpf.setConnectionFactory(hcf);

        HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
        factory.setProxyFactory(hpf);
        factory.setConnectionFactory(hcf);
        factory.setServiceUrl(serviceUrl);
        factory.setServiceInterface(serviceInterface);

        factory.afterPropertiesSet();
        Object proxy = factory.getObject();
        T client = serviceInterface.cast(proxy);

        LOG.debug("HessianClient instance created.");
        return client;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public T burlap() {
        String serviceUrl = url.apply("rpc.burlap");

        BurlapProxyFactory bpf = new BurlapProxyFactory() {
            @Override
            protected URLConnection openConnection(URL url) throws IOException {
                URLConnection uc = super.openConnection(url);
                uc.setRequestProperty("User-Agent", "CT-Burlap");
                return uc;
            }
        };

        // noinspection deprecation
        BurlapProxyFactoryBean factory = new BurlapProxyFactoryBean();
        factory.setServiceUrl(serviceUrl);
        factory.setServiceInterface(serviceInterface);
        factory.setProxyFactory(bpf);

        factory.afterPropertiesSet();
        factory.afterPropertiesSet();
        Object proxy = factory.getObject();
        T client = serviceInterface.cast(proxy);

        LOG.debug("BurlapClient instance created.");
        return client;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public T xmlRpc(FaultRevMapper errorMapper) {
        // axe-180254 (rchat/apache-xmlrpc-extension) is used.
        // Comments contains alternative action to get rid of the extensions.

        String serviceUrl = url.apply("rpc.xml-rpc");

        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(serviceUrl));
            config.setEncoding(XmlRpcClientConfigImpl.UTF8_ENCODING);
            config.setEnabledForExceptions(false); // !! [axe-180254 or [set true]]
            config.setEnabledForExtensions(false); // !! [axe-180254 or [set true]]
            config.setUserAgent("CT-XmlRpc");

            XmlRpcClient rpcClient = new XmlRpcClient();
            rpcClient.setConfig(config);
            rpcClient.setTypeFactory(new AnyTypeFactory(rpcClient)); // !! [axe-180254 or [remove statement]]
            rpcClient.setTransportFactory(
                    () -> new AnyXmlRpcTransport(rpcClient, errorMapper)
            ); // !! [axe-180254 or [remove statement]]

            Object proxy = ClientFactoryFix.newInstance( // axe-180254
                    Thread.currentThread().getContextClassLoader(),
                    serviceInterface,
                    config.getServerURL().toString(),
                    rpcClient::execute,
                    new TypeConverterFactoryImpl()
            );

            T client = serviceInterface.cast(proxy);

            LOG.debug("XmlRpcClient instance created.");
            return client;

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    public enum Cs {

        Hessian,
        Burlap,
        XmlRpc;

        public int i() {
            return ordinal();
        }

        public static Cs byIndex(int i) {
            return Cs.values()[i];
        }
    }
}
