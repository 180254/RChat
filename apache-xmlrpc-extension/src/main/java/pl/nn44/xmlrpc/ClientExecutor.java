package pl.nn44.xmlrpc;

import org.apache.xmlrpc.XmlRpcException;

/**
 * <pre>
 * Interface for XmlRpcClient.execute(String, Object[]).
 * </pre>
 */
@FunctionalInterface
public interface ClientExecutor {

    Object execute(String methodName, Object[] params) throws XmlRpcException;
}
