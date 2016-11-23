package pl.nn44.xmlrpc.client;

import org.apache.xmlrpc.XmlRpcException;

/**
 * Functional interface for XmlRpcClient.execute(String, Object[]).<br/>
 * Please check {@link pl.nn44.xmlrpc} package doc.
 */
@FunctionalInterface
public interface ClientExecutor {

    Object execute(String methodName, Object[] params) throws XmlRpcException;
}
