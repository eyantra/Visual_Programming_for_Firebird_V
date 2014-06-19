package runtimecontroller;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

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
import codeblockutil.CButton;
import codeblockutil.CFileHandler;
import codeblockutil.CLineGraph;
import codeblockutil.CTextField;

/**
 * A RuntimeLineGraph is a RuntimeWidget (with all 
 * socket/label restrictions; see documentation for 
 * RuntimeWidget).  It has a corresponding workspace 
 * block which will be referred to as the getWorkspaceBlock().  
 * 
 * RuntimeLineGraphs should have the following property:
 * this.pageLabel = null
 * this.socketLabels = empty set;
 * this.sockets = empty set;
 * this.plug = null;
 * this.afterConnector = null;
 * this.beforeConnector = null;
 * 
 * A RuntimeLineGraph serves the purpose of displaying 
 * output data in a line graph. To this this, it must 
 * keep a set of data points and also a GUI that renders 
 * these data points in some logical linear progression.  
 * To do this, it uses a CLineGraph to both store, and 
 * display the data.
 * 
 * A RuntimeLineGraph wraps a set of monitorables, whose 
 * members equal the set of blocks connected to its 
 * children sockets.  The set of monitorables will be 
 * referred to as the "domain" of this line graph.  
 * From time to time, the domain of a RuntimeLineGraph 
 * changes such that the number of series in which it 
 * must consider changes (often a change in the number 
 * of sockets translate into a change in the domain).  
 * To synchronize the domain changes to the line graph's 
 * series data, the user should invoke the UpdateDomain 
 * method.
 * 
 * A RuntimeLineGraph will only update its series once 
 * ever at least PERIOD amount of time has passed.  After PERIOD 
 * number of seconds have passed, the graph will make 
 * a call to the observer and asks for the current 
 * numerical state of its domain.  The RuntimeLineGraph 
 * will then pass this information to the CLineGraph 
 * widget to store, process, and render the newly updated 
 * information.
 * 
 * A RuntimeLineGraph can export its data in two ways.  
 * The first is sending the data to an output frame, 
 * which is really an expandable JFrame that renders 
 * the same data using much more screen space.  The 
 * output frame provides the user with four core 
 * functionality: setting the period, clearing the graph, 
 * saving the data, and saving the an image of the graph.
 * 
 */
public class RuntimeLineGraph extends RuntimeWidget implements RuntimeRecorder {
	private static final long serialVersionUID = 328149080404L;
	/** the minimum acceptable period */
	private static final double MINIMUM_PERIOD = 0.2;

	/** Number of seconds needed to pass before updating */
	private double PERIOD = 2.0;
	/** the last time this graph was updated */
	private double lastUpdateTime = -PERIOD;
	/** if in lock mode, then a call to update will have no effects */
	private boolean lock = false;
	/** the line graph widget that stores, processes, and renders the data */
	private CLineGraph linegraph;
	/** The output frame of this */
	private JFrame outputFrame;
	/**
	 * Constant String that separates the series number from the series name in
	 * the graph legend
	 */
	private final static String PERIOD_STRING = ". ";

	/** should the table be cleared */
	private boolean needsClear;

	/** the data to be added to the table */
	private ArrayList<double[]> data;

	/**
	 * Constructs a new RuntimeLineGraph with an EMPTY DOMAIN!
	 * 
	 * @param blockID
	 * @param workspaceblockID
	 * 
	 * @requires workspaceBlockID != Block.NULL && != null
	 */
	public RuntimeLineGraph(Long blockID, Long workspaceblockID,
			RuntimeWorkspace runtimeWorkspace) {
		super(blockID, workspaceblockID, runtimeWorkspace);
		Block workspaceGraph = Block.getBlock(workspaceblockID);
		linegraph = new CLineGraph(workspaceGraph.getBlockLabel(),
				workspaceGraph.getNumSockets() - 1, this.getBLockColor());
		this.setBlockWidget(linegraph);
		this.repaintBlock();
		updateDomain();
		data = new ArrayList<double[]>();
	}

	/**
	 * show output frame if mouse is clicked
	 */
	public void mouseClicked(MouseEvent e) {
		outsourceGraph();
	}
	/**
	 * Disposes of the RuntimeLineGraph and it's output frame.
	 */
	public void disposeWidget() {
		if (outputFrame != null)
			outputFrame.dispose();
	}
	/**
	 * Updates the domain of this lines graph (or the set of monitorables)
	 */
	public void updateDomain() {
		// If there are compile errors, don't change anything.
		// XXX - display to user?
		if (!SLBlockCompiler.getCompiler().compileBlocks(getWorkspaceBlockID())) {
			return;
		}
		Block workspaceGraph = getWorkspaceBlock();
		linegraph.updateDomain(workspaceGraph.getBlockLabel(), workspaceGraph
				.getNumSockets() - 1, this.getBLockColor());
	}

	/**
	 * Updates all the series names of the chart within this recorder
	 */
	public void updateChartSeriesNames() {
		for (int i = 0; i < getWorkspaceBlock().getNumSockets() - 1; i++) {
			BlockConnector socket = getWorkspaceBlock().getSocketAt(i);
			if (socket != null && socket.hasBlock()) {
				try {
					linegraph.updateSeriesNameAt((i + 1) + PERIOD_STRING
							+ socket.getLabel(), i);
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
		}
		linegraph.updateImage();
		if (outputFrame != null) {
			outputFrame.repaint();
			outputFrame.validate();
		}
	}

	/**
	 * Updates the the data by storing, processing, and rendering the changes,
	 * Only do this if the the data is not being locked and one period cycle has passed
	 */
	public void updateChart(double currentTime) {
		synchronized (data) {
			currentTime = Math.round(currentTime * 5) / 5.0;

			// Check if we have reset the clock since the last update
			if (currentTime <= lastUpdateTime) {
				lastUpdateTime = -PERIOD;
				needsClear = true;
				data.clear();
			}
			
			if (!lock) {
				if (currentTime - lastUpdateTime - PERIOD > EPSILON) {
					double[] newData = new double[Block.getBlock(getWorkspaceBlockID()).getNumSockets()];
					newData[0] = currentTime;
					// loop through all but the last (presumed empty) socket
					for (int i = 0; i < getWorkspaceBlock().getNumSockets() - 1; i++) {
						BlockConnector socket = getWorkspaceBlock().getSocketAt(i);
						if (socket != null && socket.hasBlock()) {
							try {
								int index = SLBlockCompiler.getCompiler()
										.getGlobalVariableIndex(Block.getBlock(socket.getBlockID()).getBlockLabel());
								if (index > -1) {
									newData[i+1] = WorkspaceController.getObserver()
											.getGlobalVariable(index);
								} else {
									newData[i+1] = SLBlockCompiler.getCompiler()
											.getRecorderValue(socket.getBlockID());
								}
							} catch (RuntimeException e) {
								e.printStackTrace();
							}
						}
					}

					data.add(newData);
					lastUpdateTime = currentTime;
				}
			} 
		}
	}

	/**
	 * Updates the image for this graph, should be scheduled on the AWT thread
	 */
	public void updateImage(double currentTime) {
		synchronized (data) {
			if (needsClear) {
				linegraph.clearValues();
				needsClear = false;
			} 
			
			if (data.size()>0) {
				for (double[] newData : data) {
					for (int i = 1; i < newData.length; i++) {
						BlockConnector socket = getWorkspaceBlock().getSocketAt(i-1);
						if (socket != null && socket.hasBlock()) {
							linegraph.updateValues((i) + PERIOD_STRING
									+ socket.getLabel(), i-1, newData[0], newData[i]);
						}
					}
				}
				linegraph.updateImage();
				data.clear();
			}
		}
	}

	/**
	 * clears the data associated with this line graph's series.
	 */
	private void clearChart() {
		linegraph.clearChart();
		linegraph.updateImage(); // the small graph also gets cleared
	}

	/**
	 * Displays the output frame.
	 */
	private void outsourceGraph() {
		if (outputFrame == null) {
			outputFrame = new LineGraphOutput(getBlock().getBlockLabel());
		}
		outputFrame.setName(getBlock().getBlockLabel());
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
	private class LineGraphOutput extends JFrame implements ActionListener {
		private static final long serialVersionUID = 328149080405L;
		/** The button the user presses to save the image */
		private JComponent image;
		/** The button the user presses to save the data */
		private JComponent data;
		/** The button the user presses to clear the data */
		private JComponent clear;
		/** The period setter's label */
		private JComponent label;
		/** The textfield the user types in to clear data */
		private CTextField period;

		/**
		 * constructs a new output frame
		 * 
		 * @param name
		 */
		private LineGraphOutput(String name) {
			super(name);
			JPanel panel = new JPanel(new BorderLayout());
			ChartPanel c = linegraph.getOutputPanel();
			image = RuntimeOutputFrame.getSaveImageButton(this);
			data = RuntimeOutputFrame.getSaveDataButton(this);
			clear = RuntimeOutputFrame.getClearButton(this);
			label = RuntimeOutputFrame.getIntevalLabel();
			period = RuntimeOutputFrame.getPeriodField(this);
			period.setText(Double.toString(PERIOD));

			JPanel buttonPanel = new JPanel();
			buttonPanel.setBackground(getBLockColor());
			buttonPanel.setLayout(new FlowLayout());
			buttonPanel.add(image);
			buttonPanel.add(data);
			buttonPanel.add(clear);
			buttonPanel.add(label);
			buttonPanel.add(period);

			panel.add(buttonPanel, BorderLayout.NORTH);
			panel.add(c, BorderLayout.CENTER);
			this.getContentPane().add(panel, BorderLayout.CENTER);
			this.setSize(640, 480);
			this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		}

		/**
		 * Performs the various functions of the output frame: saving image,
		 * saving data, clearing, or setting period.
		 */
		public void actionPerformed(ActionEvent e) {
			lock = true;
			if (e.getSource().equals(image)) {
				CFileHandler.writeToFile((CButton) e.getSource(), linegraph
						.getBufferedImage(600, 400));
			} else if (e.getSource().equals(data)) {
				CFileHandler.writeToFile((CButton) e.getSource(), linegraph
						.getCSV());
			} else if (e.getSource().equals(clear)) {
				clearChart();
			} else if (e.getSource().equals(period)) {
				String text = period.getText();
				try {
					double newPeriod = Double.valueOf(text);
					if (newPeriod >= MINIMUM_PERIOD) {
						PERIOD = newPeriod;
					} else {
						period.setText(Double.toString(PERIOD));
					}
				} catch (NumberFormatException ex) {
					period.setText(Double.toString(PERIOD));
				}
				period.requestFocus();
			}
			lock = false;
		}
	}
}