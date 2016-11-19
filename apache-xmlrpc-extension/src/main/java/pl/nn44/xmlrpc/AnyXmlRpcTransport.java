package pl.nn44.xmlrpc;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransport;
import org.apache.xmlrpc.common.XmlRpcInvocationException;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;

import java.io.InputStream;

/**
 * Please check {@link pl.nn44.xmlrpc} package doc.
 */
public class AnyXmlRpcTransport extends XmlRpcSun15HttpTransport {

    protected FaultMapperRev faultMapperRev;

    // ---------------------------------------------------------------------------------------------------------------

    public AnyXmlRpcTransport(XmlRpcClient pClient) {
        super(pClient);
    }

    public AnyXmlRpcTransport(XmlRpcClient pClient,
                              FaultMapperRev faultMapperRev) {
        super(pClient);
        this.faultMapperRev = faultMapperRev;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void setFaultMapperRev(FaultMapperRev faultMapperRev) {
        this.faultMapperRev = faultMapperRev;
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    protected Object readResponse(XmlRpcStreamRequestConfig pConfig,
                                  InputStream pStream)
            throws XmlRpcException {

        try {
            return super.readResponse(pConfig, pStream);

        } catch (Throwable t) {
            if (faultMapperRev != null && t instanceof XmlRpcException) {
                XmlRpcException xre = (XmlRpcException) t;

                Throwable mappedError = faultMapperRev.apply(xre);
                if (mappedError != null) {
                    throw new XmlRpcInvocationException(xre.code, xre.getMessage(), mappedError);
                }
            }

            throw t;
        }
    }
}
