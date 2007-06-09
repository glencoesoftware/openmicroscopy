/*
 * ome.formats.importer.DebugMessenger
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package src.adminTool.ui.messenger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import layout.TableLayout;

/**
 * @author TheBrain
 * 
 */
public class DebugMessenger extends JDialog implements ActionListener {

    private static Log log = LogFactory.getLog(DebugMessenger.class);

    private static final long serialVersionUID = -1026712513033611084L;

    boolean debug = false;

    String url = "http://users.openmicroscopy.org.uk/~brain/omero/bugcollector.php";

    GuiCommonElements gui;

    JPanel mainPanel;

    JPanel commentPanel;

    JPanel debugPanel;

    JButton sendBtn;

    JButton ignoreBtn;

    JButton copyBtn;

    JTextField emailTextField;

    String emailText = "";

    JTextArea commentTextArea;

    String commentText = "";

    JTextPane debugTextPane;

    StyledDocument debugDocument;

    Style debugStyle;

    public DebugMessenger(JFrame owner, String title, Boolean modal, Exception e) {

        gui = new GuiCommonElements();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setTitle(title);
        setModal(modal);
        setResizable(true);
        setSize(new Dimension(680, 400));
        setLocationRelativeTo(owner);

        // Get the full debug text
        log.error("Exception logged.", e.getCause());
        String debugText = e.getMessage();

        // Set up the main panel for tPane, quit, and send buttons
        double mainTable[][] = { { TableLayout.FILL, 100, 5, 100, 10 }, // columns
                { TableLayout.FILL, 40 } }; // rows

        mainPanel = gui.addMainPanel(this, mainTable, 10, 10, 10, 10, debug);

        // Add the quit and send buttons to the main panel
        ignoreBtn = gui.addButton(mainPanel, "Ignore", 'I',
                "Ignore the error message", "1, 1, f, c", debug);
        ignoreBtn.addActionListener(this);

        sendBtn = gui.addButton(mainPanel, "Send", 'S',
                "Send debug information to the development team", "3, 1, f, c",
                debug);
        sendBtn.addActionListener(this);

        this.getRootPane().setDefaultButton(sendBtn);
        gui.enterPressesWhenFocused(sendBtn);

        // set up the tabbed panes
        JTabbedPane tPane = new JTabbedPane();
        tPane.setOpaque(false); // content panes must be opaque

        if (debug == true) {
            tPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory
                    .createLineBorder(Color.red), tPane.getBorder()));
        }

        // fill out the comments panel (changes according to icon existance)
        Icon errorIcon = UIManager.getIcon("OptionPane.errorIcon");

        int iconSpace = 0;
        if (errorIcon != null) {
            iconSpace = errorIcon.getIconWidth() + 10;
        }

        double commentTable[][] = {
                { iconSpace, (160 - iconSpace), TableLayout.FILL }, // columns
                { 100, 30, TableLayout.FILL } }; // rows

        commentPanel = gui.addMainPanel(this, commentTable, 10, 10, 10, 10,
                debug);

        tPane.addTab("Comments", null, commentPanel, "Your comments go here.");

        String message = "An error message has been generated by the "
                + "application.\n\nTo help us improve our software, please fill "
                + "out the following form. Your personal details are purely optional, "
                + "and will only be used for development purposes.\n\nPlease note that "
                + "your application may need to be restarted to work properly.";

        JLabel iconLabel = new JLabel(errorIcon);
        commentPanel.add(iconLabel, "0,0, l, c");

        @SuppressWarnings("unused")
        JTextPane instructions = gui.addTextPane(commentPanel, message,
                "1,0,2,0", debug);

        emailTextField = gui.addTextField(commentPanel, "Email: ", emailText,
                'E', "Input your email address here.", "(Optional)",
                TableLayout.PREFERRED, "0, 1, 2, 1", debug);

        commentTextArea = gui.addTextArea(commentPanel,
                "What you were doing when you crashed?", "", 'W', "0, 2, 2, 2",
                debug);

        // fill out the debug panel
        double debugTable[][] = { { TableLayout.FILL }, // columns
                { TableLayout.FILL, 32 } }; // rows

        debugPanel = gui.addMainPanel(this, debugTable, 10, 10, 10, 10, debug);

        debugTextPane = gui.addTextPane(debugPanel, "", "", 'W', "0, 0", debug);
        debugTextPane.setEditable(false);

        debugDocument = (StyledDocument) debugTextPane.getDocument();
        debugStyle = debugDocument.addStyle("StyleName", null);
        StyleConstants.setForeground(debugStyle, Color.black);
        StyleConstants.setFontFamily(debugStyle, "SansSerif");
        StyleConstants.setFontSize(debugStyle, 12);
        StyleConstants.setBold(debugStyle, false);

        gui.appendTextToDocument(debugDocument, debugStyle, debugText);

        copyBtn = gui.addButton(debugPanel, "Copy to Clipboard", 'C',
                "Copy the Exception Message to the clipboard", "0, 1, c, b",
                debug);
        copyBtn.addActionListener(this);

        tPane.addTab("Error Message", null, debugPanel,
                "The Exception Message.");

        // Add the tab panel to the main panel
        mainPanel.add(tPane, "0, 0, 4, 0");

        add(mainPanel, BorderLayout.CENTER);

        setVisible(true);

    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == sendBtn) {
            emailText = emailTextField.getText();
            commentText = commentTextArea.getText();
            String debugText = debugTextPane.getText();

            sendRequest(emailText, commentText, debugText,
                    "Extra data goes here.");
        }

        if (source == ignoreBtn) {
            dispose();
        }

        if (source == copyBtn) {
            debugTextPane.selectAll();
            debugTextPane.copy();
        }
    }

    private void sendRequest(String email, String comment, String error,
            String extra) {
        Map<String, String> map = new HashMap<String, String>();

        map.put("email", email);
        map.put("comment", comment);
        map.put("error", error);
        map.put("extra", extra);

        map.put("type", "admintool_bugs");
        map.put("java_version", System.getProperty("java.version"));
        map.put("java_class_path", System.getProperty("java.class.path"));
        map.put("os_name", System.getProperty("os.name"));
        map.put("os_arch", System.getProperty("os.arch"));
        map.put("os_version", System.getProperty("os.version"));

        try {
            HtmlMessenger messenger = new HtmlMessenger(url, map);
            String serverReply = messenger.executePost();
            JOptionPane.showMessageDialog(this, serverReply);
            this.dispose();
        } catch (Exception e) {
            // Get the full debug text
            log.error("Exception logged.", e.getCause());
            String debugText = e.getMessage();
            gui.appendTextToDocument(debugDocument, debugStyle, "----\n"
                    + debugText);
            JOptionPane
                    .showMessageDialog(
                            this,
                            "Sorry, but due to an error we were not able to automatically \n"
                                    + "send your debug information. \n\n"
                                    + "You can still send us the error message by clicking on the \n"
                                    + "error message tab, copying the error message to the clipboard, \n"
                                    + "and sending it to comments@openmicroscopy.org.uk.");
        }
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        String laf = UIManager.getSystemLookAndFeelClassName();
        // laf = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
        // laf = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
        // laf = "javax.swing.plaf.metal.MetalLookAndFeel";
        // laf = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";

        if (laf.equals("apple.laf.AquaLookAndFeel")) {
            System.setProperty("Quaqua.design", "panther");

            try {
                UIManager
                        .setLookAndFeel("ch.randelshofer.quaqua.QuaquaLookAndFeel");
            } catch (Exception e) {
                System.err.println(laf + " not supported.");
            }
        } else {
            try {
                UIManager.setLookAndFeel(laf);
            } catch (Exception e) {
                System.err.println(laf + " not supported.");
            }
        }

        try {
            HttpClient client = new HttpClient();
            PostMethod method = new PostMethod("blarg");
            client.executeMethod(method);
        } catch (Exception e) {
            new DebugMessenger(null, "Error Dialog Test", true, e);
        }
    }
}
