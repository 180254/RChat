package pl.nn44.rchat.client.xmlrpc;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import pl.nn44.rchat.protocol.ChannelUser;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.rchat.protocol.Response;
import pl.nn44.rchat.protocol.WhatsUp;

@SuppressWarnings("serial")
public class XmlRpcChatService implements ChatService {

    private final XmlRpcClient xr;

    public XmlRpcChatService(XmlRpcClient xr) {
        this.xr = xr;
    }

    @Override
    public Response login(String username, String password) {
        return null;
    }

    @Override
    public Response join(String channel, String password) {
        return null;
    }

    @Override
    public Response part(String channel) {
        return null;
    }

    @Override
    public Response kick(String channel, String username) {
        return null;
    }

    @Override
    public Response ban(String channel, String username, boolean state) {
        return null;
    }

    @Override
    public Response<ChannelUser[]> names(String channel) {
        return null;
    }

    @Override
    public Response topic(String channel) {
        return null;
    }

    @Override
    public Response topic(String channel, String text) {
        return null;
    }

    @Override
    public Response admin(String channel, String username, boolean state) {
        return null;
    }

    @Override
    public Response ignore(String channel, String username, boolean state) {
        return null;
    }

    @Override
    public Response privy(String nickname, String text) {
        return null;
    }

    @Override
    public Response message(String channel, String message) {
        try {
            return (Response) xr.execute("ChatService.message", new Object[]{channel, message});
        } catch (XmlRpcException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Response<WhatsUp[]> whatsUp(int longPoolingTimeoutMs) {
        return null;
    }
}
