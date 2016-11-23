package pl.nn44.xmlrpc.server;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;

import java.io.OutputStream;

/**
 * Please check {@link pl.nn44.xmlrpc} package doc.
 */
public class AnyXmlRpcServer extends XmlRpcServletServer {

    protected FaultMapper faultMapper;

    // ---------------------------------------------------------------------------------------------------------------

    public void setFaultMapper(FaultMapper faultMapper) {
        this.faultMapper = faultMapper;
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    protected void writeError(XmlRpcStreamRequestConfig pConfig,
                              OutputStream pStream,
                              Throwable pError)
            throws XmlRpcException {

        Throwable error = pError;

        if (faultMapper != null && pError instanceof XmlRpcException) {
            XmlRpcException xError = (XmlRpcException) pError;
            Throwable linkedError = xError.linkedException;

            XmlRpcException mappedError = faultMapper.apply(linkedError);
            if (mappedError != null) {
                error = mappedError;
            }
        }

        super.writeError(pConfig, pStream, error);
    }
}
