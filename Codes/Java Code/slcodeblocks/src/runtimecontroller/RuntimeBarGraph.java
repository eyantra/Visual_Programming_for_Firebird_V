package runtimecontroller;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartPanel;

import slcodeblocks.RuntimeWorkspace;
import slcodeblocks.SLBlockCompiler;
import slcodeblocks.WorkspaceController;
import codeblocks.Block;
import codeblocks.BlockConnector;
import codeblockutil.CBarGraph;
import codeblockutil.CButton;
import codeblockutil.CFileHandler;

/**
 * A RuntimeBarGraph is a RuntimeButton (with all 
 * socket/label restrictions; see documentation for 
 * RuntimeButton).  It has a corresponding workspace 
 * block which will be referred to as the wsblock.  
 * 
 * RuntimeBarGraph should have the following property:
 * this.blockLabel = wsblock.blockLabel;
 * this.pageLabel = null
 * this.socketLabels = empty set;
 * this.sockets = empty set;
 * this.plug = null;
 * this.afterConnector = null;
 * this.beforeConnector = null;
 * 
 * A RuntimeBarGraph serves the purpose of displaying 
 * output data in a bar graph. To this this, it must 
 * keep a set of data points and also a GUI that renders 
 * these data points in some logical stacked progression.  
 * To do this, it uses a CBarGraph to both store, and 
 * display the data.
 * 
 * A RuntimeBarGraph wraps a set of monitorables, whose 
 * members equals the set of blocks connected to its 
 * children sockets.  The set of monitorables will be 
 * referred to as the "domain" of this graph.  
 * From time to time, the domain of a RuntimeBarGraph 
 * changes such that the number of series in which it 
 * must consider changes (often a change in the number 
 * of sockets translate into a change in the domain).  
 * To synchronize the domain changes to the graph's 
 * series data, the user should invoke the UpdateDomain 
 * method.
 * 
 * A RuntimeBarGraph will only update its series once 
 * ever PERIOD amount of time has passed.  After PERIOD 
 * number of seconds have passed, the graph will make 
 * a call to the observer and asks for the current 
 * numerical state of its domain.  The RuntimeBarGraph 
 * will then pass this information to the CBarGraph 
 * widget to store, process, and render the newly updated 
 * information.
 * 
 * A RuntimeBarGraph can export its data in two ways.  
 * The first is is sending the data to an output frame, 
 * which is really an expandable JFrame that renders 
 * the same data using much more screen space.  The 
 * output frame provides the user with four core 
 * functionality: setting the period, clearing the graph, 
 * saving the data, and saving the an image of the graph.
 * 
 */
public class RuntimeBarGraph extends RuntimeWidget implements RuntimeRecorder{
	private static final long serialVersionUID = 328149080401L;
	/** The output frame of this */
	private JFrame outputFrame;
	/** the graph widget that stores, processes, and renders the data */
	private CBarGraph bargraph;
	/** if in lock mode, then a call to update will have no effects */
	private boolean lock = false;
	/** the last time this graph was updated */
	private double lastUpdateTime = 0;
	
	/** the current bar graph data */
	private double[] data;
	
	/** true when new data is ready */
	private Boolean dataReady = false;

	
    /**
     * Constructs a new bar graph with an EMPTY DOMAIN!
     * @param blockID
     * @param workspaceblockID
     * 
     * @requires workspaceBlockID != Block.NULL && != null
     */
	public RuntimeBarGraph(Long blockID, Long workspaceblockID, RuntimeWorkspace runtimeWorkspace) {
    	super (blockID, workspaceblockID, runtimeWorkspace);
    	Block workspaceGraph = Block.getBlock(workspaceblockID);
    	bargraph = new CBarGraph(workspaceGraph.getBlockLabel(), workspaceGraph.getNumSockets(), this.getBLockColor());
		this.setBlockWidget(bargraph);
		this.repaintBlock();
		updateDomain();
    }
	

	
	/**
	 * show output frame if mouse is clicked
	 */
	public void mouseClicked(MouseEvent e){
		outsourceGraph();
	}
	/**
	 * Disposes of the RuntimeBarGraph and it's output frame.
	 */
	public void disposeWidget(){
		if(outputFrame != null) outputFrame.dispose();
	}
	/**
	 * Updates the domain of this bar graph (or the set of monitorables)
	 */
	public void updateDomain(){
	    // If there are compile errors, don't change anything.
	    // XXX - display to user?
		if (!SLBlockCompiler.getCompiler().compileBlocks(getWorkspaceBlockID())) {
		    return;
		}
		
		Block workspaceGraph = getWorkspaceBlock();
		bargraph.updateDomain(workspaceGraph.getBlockLabel(), workspaceGraph.getNumSockets(), this.getBLockColor());
	}
	
	/**
	 * Updates the data by storing, processing, and rendering the changes,
	 * Only do this if the the data is not being locked and one period cycle has passed
	 */
	public void updateChart(double currentTime) {
		if(!lock){
			synchronized (dataReady) {
				if (data == null || data.length != Block.getBlock(getWorkspaceBlockID()).getNumSockets()-1) {
					data = new double[Block.getBlock(getWorkspaceBlockID()).getNumSockets()-1];
				}
				
				int sockIndex = 0;
				for (BlockConnector socket : getWorkspaceBlock().getSockets()) {
					if (socket != null && socket.hasBlock()){
						try{
		                    double value = 0;
		                    int index = SLBlockCompiler.getCompiler().getGlobalVariableIndex(Block.getBlock(socket.getBlockID()).getBlockLabel());
		                    if(index > -1){
		                    	value = WorkspaceController.getObserver().getGlobalVariable(index);
		                    }else{
		                    	value = SLBlockCompiler.getCompiler().getRecorderValue(socket.getBlockID());
		                    }
		                    data[sockIndex] = value;
						}catch(RuntimeException e ){
							e.printStackTrace();
						}
					}
					sockIndex++;
				}
			}
			lastUpdateTime = currentTime;
			dataReady = true;
		}
	}

	/**
	 * Updates the image for this graph, should be scheduled on the AWT thread
	 */
	public void updateImage(double currentTime) {
		synchronized(dataReady) {
			if (dataReady) {
				for (int i=0; i<data.length; i++) {
					BlockConnector socket = getWorkspaceBlock().getSocketAt(i);
					bargraph.updateValues((i+1)+". "+socket.getLabel(), data[i]);
				}
				bargraph.updateImage();
				dataReady = false;
			}
		}
	}

	
	/**
	 * clears the data associated with this line graph's series.
	 */
	public void clearChart() {
		bargraph.clearChart();
		bargraph.updateImage();   // the small graph also gets cleared
	}
	/**
	 * DIsplays the output frame.
	 */
	void outsourceGraph(){
		if(outputFrame == null){
			outputFrame = new BarGraphOutput(getBlock().getBlockLabel());
		}
		outputFrame.setVisible(true);
	}
	/**
	 * An output frame has two mirrors the image found in the Runtime version, only bigger
	 * and whose size can expand to the screen size.  The corresponding runtime recorder should
	 * be notified of any user generated action such as clearing the graph, or setting the period.
	 * An output frame also serves the purpose of giving the users  way to export data.  When the
	 * users attempts to export data either as an image or a csv file, the runtime block is put on
	 * LOCK; that is, ant attempts to access or modify the data is rejected.
	 */
	private class BarGraphOutput extends JFrame implements ActionListener{
		private static final long serialVersionUID = 328149080402L;
		/** The button the user presses to save the image */
		private JComponent image;
		/** The button the user presses to save the data */
		private JComponent data;
		/** The button the user presses to clear the data */
		private JComponent clear;
		/**
		 * constructs a new output frame
		 * @param name
		 */
		private BarGraphOutput(String name){
			super(name);
			JPanel panel = new JPanel(new BorderLayout());
			ChartPanel c = bargraph.getOutputPanel();
			image = RuntimeOutputFrame.getSaveImageButton(this);
			data = RuntimeOutputFrame.getSaveDataButton(this);
			clear = RuntimeOutputFrame.getClearButton(this);
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setBackground(getBLockColor());
			buttonPanel.setLayout(new FlowLayout());
			buttonPanel.add(image);
			buttonPanel.add(data);
			buttonPanel.add(clear);
			
			panel.add(buttonPanel, BorderLayout.NORTH);
			panel.add(c, BorderLayout.CENTER);
			this.getContentPane().add(panel, BorderLayout.CENTER);
			this.setSize(640, 480);
			this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		}
		/**
		 * Performs the various functions of the output frame:
		 * saving image, saving data, clearing, or setting period.
		 */
		public void actionPerformed(ActionEvent e){
			lock = true;
			if(e.getSource().equals(image)){
				CFileHandler.writeToFile((CButton)e.getSource(), bargraph.getBufferedImage(600, 400));
			}else if(e.getSource().equals(data)){
				CFileHandler.writeToFile((CButton)e.getSource(), bargraph.getCSV());
			}else if(e.getSource().equals(clear)){
				clearChart();
			}
			lock = false;
		}
	}
	
	/**
	 * Updates all the series names of the chart within this recorder
	 */
	public void updateChartSeriesNames() {
		for (int i = 0; i< getWorkspaceBlock().getNumSockets()-1; i++) {
			BlockConnector socket = getWorkspaceBlock().getSocketAt(i);
			if (socket != null && socket.hasBlock()){
				try{
					bargraph.updateSeriesNamesAt((i+1)+". "+socket.getLabel(), i);
				}catch(RuntimeException e ){
					e.printStackTrace();
				}
			}
		}
		bargraph.updateImage();
		if (outputFrame != null) {
			outputFrame.repaint(); 
			outputFrame.validate();
		}
	}
}