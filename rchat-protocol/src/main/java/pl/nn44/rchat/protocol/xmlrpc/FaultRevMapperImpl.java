package pl.nn44.rchat.protocol.xmlrpc;

import org.apache.xmlrpc.XmlRpcException;
import pl.nn44.rchat.protocol.exception.ChatException;
import pl.nn44.rchat.protocol.exception.ChatException.Reason;
import pl.nn44.xmlrpc.client.FaultRevMapper;

/**
 * Fault rev mapper for xml-rpc protocol.<br/>
 * Please check {@link pl.nn44.xmlrpc} package doc.
 */
public class FaultRevMapperImpl implements FaultRevMapper {

    @Override
    public Throwable apply(XmlRpcException e) {

        if (e.code == 101) {
            String message = e.getMessage();
            Reason reason = Reason.valueOf(message);
            return new ChatException(reason);

        } else {
            return null;
        }
    }
}
