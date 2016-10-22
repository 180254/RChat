package pl.nn44.rchat.client;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.nn44.rchat.protocol.ChatException;
import pl.nn44.rchat.protocol.ChatService;
import pl.nn44.rchat.protocol.Response;

import javax.swing.*;
import java.util.Properties;

public class ClientApp {

    private static final Logger LOG = LoggerFactory.getLogger(ClientApp.class);

    private JPanel panel;
    private JButton button;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(WindowsLookAndFeel.class.getName());
        } catch (ClassNotFoundException | InstantiationException |
                UnsupportedLookAndFeelException | IllegalAccessException ignored) {
        }

        JFrame frame = new JFrame("ClientApp");
        frame.setContentPane(new ClientApp().panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ---------------------------------------------------------------------------------------------------------------

    private final ChatService[] chatServices = new ChatService[3];

    public ClientApp() {
        Properties prop = PropLoader.get();
        Clients clients = new Clients(prop);

        try {
            this.chatServices[0] = clients.hessianClient();
        } catch (Exception e) {
            LOG.error("HessianClient creation fail.", e);
        }

        try {
            this.chatServices[1] = clients.burlapClient();
        } catch (Exception e) {
            LOG.error("BurlapClient creation fail.", e);
        }

        try {
            this.chatServices[2] = clients.xmlRpcClient();
        } catch (Exception e) {
            LOG.error("XML-RPC-Client creation fail.", e);
        }

        for (int i = 0; i < 3; i++) {
            try {
                Response<?> test = this.chatServices[i].test(false);
                LOG.debug("ChatService.test.a({})=OK;   {}", i, test);
            } catch (ChatException e) {
                LOG.error("ChatService.test.a({})=FAIL; {}", i, e.toString());
            }

            try {
                Response<?> test = this.chatServices[i].test(true);
                LOG.error("ChatService.test.a({})=FAIL; {}", i, test);
            } catch (ChatException e) {
                LOG.debug("ChatService.test.b({})=OK;   {}", i, e.toString());
            }
        }

    }
}
