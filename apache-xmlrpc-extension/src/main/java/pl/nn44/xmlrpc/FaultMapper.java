package pl.nn44.xmlrpc;

import org.apache.xmlrpc.XmlRpcException;

import java.util.function.Function;

/**
 * Please check {@link pl.nn44.xmlrpc} package doc.
 */
public interface FaultMapper
        extends Function<Throwable, XmlRpcException> {

}
