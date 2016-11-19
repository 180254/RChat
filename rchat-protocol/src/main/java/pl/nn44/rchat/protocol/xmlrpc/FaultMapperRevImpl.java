package pl.nn44.rchat.protocol.xmlrpc;

import org.apache.xmlrpc.XmlRpcException;
import pl.nn44.rchat.protocol.exception.ChatException;
import pl.nn44.xmlrpc.FaultMapperRev;

public class FaultMapperRevImpl implements FaultMapperRev {

    @Override
    public Throwable apply(XmlRpcException e) {

        if (e.code == 101) {
            String message = e.getMessage();
            ChatException.Reason reason = ChatException.Reason.valueOf(message);
            return new ChatException(reason);

        } else {
            return null;
        }
    }
}
