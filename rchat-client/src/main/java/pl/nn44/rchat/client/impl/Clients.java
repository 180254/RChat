package pl.nn44.rchat.client.impl;

import com.caucho.burlap.client.BurlapProxyFactory;
import com.caucho.hessian.client.HessianConnection;
import com.caucho.hessian.client.HessianConnectionFactory;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianURLConnectionFactory;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.TypeConverterFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.caucho.BurlapProxyFactoryBean;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.xmlrpc.AnyTypeFactory;
import pl.nn44.xmlrpc.ClientFactoryFix;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

public class Clients {

    private static final Logger LOG = LoggerFactory.getLogger(Clients.class);
    private final Properties prop;

    public Clients(Properties prop) {
        this.prop = prop;
        LOG.debug("{} instance created.", getClass().getSimpleName());
    }

    // ---------------------------------------------------------------------------------------------------------------

    public ChatService hessianClient() {
        String serviceUrl = prop.getProperty("url.app") + prop.getProperty("url.hessian");

        HessianProxyFactory hpf = new HessianProxyFactory();
        HessianConnectionFactory hcf = new HessianURLConnectionFactory() {

            @Override
            public HessianConnection open(URL url) throws IOException {
                HessianConnection hc = super.open(url);
                hc.addHeader("User-Agent", "RC-Hessian");
                return hc;
            }
        };

        hcf.setHessianProxyFactory(hpf);
        hpf.setConnectionFactory(hcf);

        HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
        factory.setProxyFactory(hpf);
        factory.setConnectionFactory(hcf);
        factory.setServiceUrl(serviceUrl);
        factory.setServiceInterface(ChatService.class);

        factory.afterPropertiesSet();
        ChatService chatService = (ChatService) factory.getObject();

        LOG.debug("HessianClient instance created.");
        return chatService;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public ChatService burlapClient() {
        String serviceUrl = prop.getProperty("url.app") + prop.getProperty("url.burlap");

        BurlapProxyFactory bpf = new BurlapProxyFactory() {
            @Override
            protected URLConnection openConnection(URL url) throws IOException {
                URLConnection uc = super.openConnection(url);
                uc.setRequestProperty("User-Agent", "RC-Burlap");
                return uc;
            }
        };

        // noinspection deprecation
        BurlapProxyFactoryBean factory = new BurlapProxyFactoryBean();
        factory.setServiceUrl(serviceUrl);
        factory.setServiceInterface(ChatService.class);
        factory.setProxyFactory(bpf);

        factory.afterPropertiesSet();
        ChatService chatService = (ChatService) factory.getObject();

        LOG.debug("BurlapClient instance created.");
        return chatService;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public ChatService xmlRpcClient() {
        String serviceUrl = prop.getProperty("url.app") + prop.getProperty("url.xml-rpc");

        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(serviceUrl));
            config.setEncoding(XmlRpcClientConfigImpl.UTF8_ENCODING);
            config.setEnabledForExceptions(true);
            config.setEnabledForExtensions(true); // required by enabledForExceptions
            config.setUserAgent("RC-XmlRpc");

            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            client.setTypeFactory(new AnyTypeFactory(client));

            ChatService chatService = (ChatService) ClientFactoryFix.newInstance(
                    Thread.currentThread().getContextClassLoader(),
                    ChatService.class,
                    "ChatService",
                    client::execute,
                    config.getServerURL().toString(),
                    new TypeConverterFactoryImpl()
            );

            LOG.debug("XmlRpcClient instance created.");
            return chatService;

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
