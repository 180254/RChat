package pl.nn44.xmlrpc;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.common.XmlRpcInvocationException;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;

import java.io.OutputStream;

/**
 * @see package doc
 */
public class AnyXmlRpcServer extends XmlRpcServletServer {

    protected FaultMapper faultMapper;

    public void setFaultMapper(FaultMapper faultMapper) {
        this.faultMapper = faultMapper;
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    protected void writeError(XmlRpcStreamRequestConfig pConfig, OutputStream pStream, Throwable pError)
            throws XmlRpcException {

        Throwable error = pError;

        if (faultMapper != null && pError instanceof XmlRpcInvocationException) {
            XmlRpcInvocationException invError = (XmlRpcInvocationException) pError;
            Throwable linkedError = invError.linkedException;

            XmlRpcException mappedError = faultMapper.apply(linkedError);
            if (mappedError != null) {
                error = mappedError;
            }
        }

        super.writeError(pConfig, pStream, error);
    }
}
