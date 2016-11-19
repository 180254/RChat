package pl.nn44.rchat.protocol.xmlrpc;

import org.apache.xmlrpc.XmlRpcException;
import pl.nn44.rchat.protocol.exception.ChatException;
import pl.nn44.xmlrpc.FaultMapper;

public class FaultMapperImpl implements FaultMapper {

    @Override
    public XmlRpcException apply(Throwable throwable) {

        if (throwable instanceof ChatException) {
            ChatException ce = (ChatException) throwable;
            return new XmlRpcException(101, ce.getReason().name());

        } else {
            return null;
        }
    }
}
