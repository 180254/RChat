package pl.nn44.rchat.client.model;

import pl.nn44.rchat.protocol.RChannel;

import java.text.MessageFormat;

public class ChannelEx {

    private final RChannel rChannel;
    private String topic;
    private boolean join;

    public ChannelEx(RChannel rChannel) {
        this.rChannel = rChannel;
    }

    @Override
    public String toString() {
        String join = this.join ? "[+]" : "[-]";

        String modes = "(";
        modes += rChannel.isPassword() ? "p" : "";
        modes += ")";

        modes = modes.length() > 2 ? modes : "";

        return MessageFormat.format(
                "{0} {1} {2}",
                join,
                rChannel.getName(),
                modes
        ).trim();
    }
}
