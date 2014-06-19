package slcodeblocks;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import codeblockutil.CButton;
import codeblockutil.CGraphiteButton;
import codeblockutil.CLabel;
import codeblockutil.CTextField;
import codeblockutil.Canvas;
import codeblockutil.DefaultCanvas;
import codeblockutil.Explorer;
import codeblockutil.TabbedExplorer;

import static slcodeblocks.SLNetworkManager.*;

public class NetworkPanel extends JDialog{
	private static final long serialVersionUID = 328149080414L;
	/** Default Port Text */
    private final static String DEFAULT_PORT = "14330";
    /** Default Host Text */
    private final static String DEFAULT_HOST = "localhost";
    /** The NumberFormat instance we use. */
    private final static NumberFormat FORMATTER = NumberFormat.getIntegerInstance();
    
    public NetworkPanel(Frame owner) {
        super(owner, "Network Connections", true);
        
        FORMATTER.setGroupingUsed(false);
        
    	Explorer explorer = new TabbedExplorer();
    	List<Canvas> canvases = new ArrayList<Canvas>();
    	canvases.add(buildServerPanel());
    	canvases.add(buildClientPanel());
    	explorer.setDrawersCard(canvases);
    	explorer.getJComponent().putClientProperty("jgoodies.noContentBorder", Boolean.TRUE);
        
    	this.add(explorer.getJComponent());
        this.setBounds(150,100,400, 170);
        this.setResizable(false);
        this.setAlwaysOnTop(true);
    }
    private void close(){
    	this.setVisible(false);
    	this.dispose();
    }
    private Canvas buildClientPanel() {
    	//set up internal components
    	CLabel hostLabel = new CLabel("Host:");
    	final CTextField host = new CTextField(DEFAULT_HOST);
    	host.setText(DEFAULT_HOST);
    	CLabel portLabel = new CLabel("Port:");
        final CTextField port = new CTextField(FORMATTER);
        port.setText(DEFAULT_PORT);
        CButton cancel = new CGraphiteButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	close();
            }
        });
        CButton ok = new CGraphiteButton("Connect");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                connect(host.getText(), port.getText());
            }
        });
        
        //set up main main panel
        DefaultCanvas pane = new DefaultCanvas(null);
        pane.setName("Connect As Client");
    	pane.setBackground(new Color(25,25,25));
        double[][] constraints = {
        		{5,60,TableLayoutConstants.FILL, 100,100,5},
        		{5,22,5,22,5,40,TableLayoutConstants.FILL}};
        pane.setLayout(new TableLayout(constraints));
        pane.add(hostLabel, "1, 1");
        pane.add(host, "2, 1, 4, 1");
        pane.add(portLabel, "1, 3");
        pane.add(port, "2, 3, 4, 3");
        String lcOSName = System.getProperty("os.name").toLowerCase();
        boolean MAC_OS_X = lcOSName.startsWith("mac os x");
        if(MAC_OS_X){
            pane.add(cancel, "3, 5");
            pane.add(ok, "4, 5");
        }else{
            pane.add(ok, "3, 5");
            pane.add(cancel, "4, 5");
        }
        return pane;
    }
    
    private Canvas buildServerPanel() {
    	//set up internal components
    	CLabel portLabel = new CLabel("Port:");
        final CTextField port = new CTextField(FORMATTER);
        port.setText(DEFAULT_PORT);
        CButton cancel = new CGraphiteButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	close();
            }
        });
        CButton ok = new CGraphiteButton("Connect");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	connect(null, port.getText());
            }
        });
        
        //set up main main panel
        DefaultCanvas pane = new DefaultCanvas(null);
        pane.setName("Connect As Host");
    	pane.setBackground(new Color(25,25,25));
        double[][] constraints = {
        		{5,60,TableLayoutConstants.FILL, 100,100,5},
        		{5,22,5,22,5,40,TableLayoutConstants.FILL}};
        pane.setLayout(new TableLayout(constraints));
        pane.add(portLabel, "1, 1");
        pane.add(port, "2, 1, 4, 1");
        String lcOSName = System.getProperty("os.name").toLowerCase();
        boolean MAC_OS_X = lcOSName.startsWith("mac os x");
        if(MAC_OS_X){
            pane.add(cancel, "3, 5");
            pane.add(ok, "4, 5");
        }else{
            pane.add(ok, "3, 5");
            pane.add(cancel, "4, 5");
        }
        return pane;
    }

    public static void main(String[] args) {
        NetworkPanel panel = new NetworkPanel(null);
        panel.setVisible(true);
        panel.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }
    
    /**
     * Make a connection. 
     */
    private void connect(String host, String port) {
        // Check for validity.
        int p;
        try {
            p = Integer.parseInt(port);
        }
        catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(null, "Invalid port");
            return;
        }
        
        // TODO remove port option for server connection 
        if (host == null) {
            if (WorkspaceController.getNetworkManager().openServerConnection())
            	close();
        }
        // TODO set dir 
        else if (WorkspaceController.getNetworkManager().connect(host, p, NORTH)) {
        	close();
        }
    }
}