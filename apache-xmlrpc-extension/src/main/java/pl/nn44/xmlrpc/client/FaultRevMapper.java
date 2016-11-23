package pl.nn44.xmlrpc.client;

import org.apache.xmlrpc.XmlRpcException;

import java.util.function.Function;

/**
 * Please check {@link pl.nn44.xmlrpc} package doc.
 */
public interface FaultRevMapper
        extends Function<XmlRpcException, Throwable> {

}
