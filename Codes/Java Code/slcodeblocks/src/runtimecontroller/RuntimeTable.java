package runtimecontroller;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import slcodeblocks.RuntimeWorkspace;
import slcodeblocks.SLBlockCompiler;
import slcodeblocks.WorkspaceController;
import codeblocks.Block;
import codeblocks.BlockConnector;
import codeblockutil.CButton;
import codeblockutil.CFileHandler;
import codeblockutil.CTable;
import codeblockutil.CTextField;

/**
 * A RuntimeTable is a RuntimeButton (with all 
 * socket/label restrictions; see documentation for 
 * RuntimeButton).  It has a corresponding workspace 
 * block which will be referred to as the getWorkspaceBlock().  
 * 
 * RuntimeBarGraph should have the following property:
 * this.pageLabel = null
 * this.socketLabels = empty set;
 * this.sockets = empty set;
 * this.plug = null;
 * this.afterConnector = null;
 * this.beforeConnector = null;
 * 
 * A RuntimeTable serves the purpose of displaying 
 * output data in a table. To this this, it must 
 * keep a set of data points and also a GUI that renders 
 * these data points in some logical stacked progression.  
 * To do this, it uses a CTable to both store, and 
 * display the data.
 * 
 * A RuntimeTable wraps a set of monitorables, whose 
 * members equals the set of blocks connected to its 
 * children sockets.  The set of monitorables will be 
 * referred to as the "domain" of this graph.  
 * From time to time, the domain of a RuntimeTable 
 * changes such that the number of series in which it 
 * must consider changes (often a change in the number 
 * of sockets translate into a change in the domain).  
 * To synchronize the domain changes to the graph's 
 * series data, the user should invoke the UpdateDomain 
 * method.
 * 
 * A RuntimeTable will only update its series once 
 * at least PERIOD amount of time has passed.  After PERIOD 
 * number of seconds have passed, the graph will make 
 * a call to the observer and asks for the current 
 * numerical state of its domain.  The RuntimeTable 
 * will then pass this information to the CTable 
 * widget to store, process, and render the newly updated 
 * information.
 * 
 * A RuntimeTable can export its data in two ways.  
 * The first is is sending the data to an output frame, 
 * which is really an expandable JFrame that renders 
 * the same data using much more screen space.  The 
 * output frame provides the user with four core 
 * functionality: setting the period, clearing the graph, 
 * saving the data, and saving the an image of the graph.
 * 
 */
public class RuntimeTable extends RuntimeWidget implements RuntimeRecorder {
	private static final long serialVersionUID = 328149080408L;
	/** the minimum acceptable period */
	private static final double MINIMUM_PERIOD = 0.2;
	/** Number of seconds needed to pass before updating */
	private double PERIOD = 2.0;
	/** the last time table was updated */
	private double lastUpdateTime = -PERIOD;
	/** the table widget that stores, processes, and renders the data */
	private CTable table;
	/** The table widget of output frame of this */
	private CTable outputTable;
	/** The output frame of this */
	private JFrame outputFrame;
	/** Constant string for the time column */
	private final static String TIME = "TIME";

	/** should the table be cleared */
	private boolean needsClear;

	/** the data to be added to the table */
	private ArrayList<double[]> data;

	/**
	 * Constructs a new table with an EMPTY DOMAIN!
	 * 
	 * @param blockID
	 * @param workspaceblockID
	 * 
	 * @requires workspaceBlockID != Block.NULL && != null
	 */
	public RuntimeTable(Long blockID, Long workspaceblockID,
			RuntimeWorkspace runtimeWorkspace) {
		super(blockID, workspaceblockID, runtimeWorkspace);
		String[] names = new String[getWorkspaceBlock().getNumSockets()];
		names[0] = TIME;
		for (int i = 0; i < getWorkspaceBlock().getNumSockets(); i++) {
			BlockConnector socket = getWorkspaceBlock().getSocketAt(i);
			if (socket != null && socket.hasBlock()) {
				names[i + 1] = socket.getLabel();
			}
		}
		this.table = new CTable(3);
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				outsourceGraph();
			}
		});
		table.setOpaque(false);
		table.setColumns(names);
		table.setBounds(0, 15, 200, 100);
		this.outputTable = new CTable();
		outputTable.setColumns(names);
		this.setBlockWidget(table);
		this.repaintBlock();
		updateDomain();
		data = new ArrayList<double[]>();
	}

	/**
	 * Disposes of the RuntimeLineGraph and it's output frame.
	 */
	public void disposeWidget() {
		if (outputFrame != null)
			outputFrame.dispose();
		table.setEnabled(false);
		outputTable.setEnabled(false);
		table = null;
		outputTable = null;
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
	}

	/**
	 * Updates the the data by storing, processing, and rendering the changes,
	 * Only do this if the the data is not being locked and one period cycle has
	 * passed
	 */
	public void updateChart(double currentTime) {
		synchronized (data) {
			currentTime = Math.round(currentTime * 5) / 5.0; 
	
			// Check if we have reset the time since the last update
			if (currentTime <= lastUpdateTime) {
				lastUpdateTime = -PERIOD;
				needsClear = true;
				data.clear();
			}
			
			if (currentTime - lastUpdateTime - PERIOD > EPSILON) {
				double[] newData = new double[Block.getBlock(getWorkspaceBlockID()).getNumSockets()];
				newData[0] = currentTime;
				
				for (int i = 0; i < Block.getBlock(getWorkspaceBlockID()).getNumSockets(); i++) {
					BlockConnector socket = Block.getBlock(getWorkspaceBlockID()).getSocketAt(i);
					if (socket != null && socket.hasBlock()) {
						try {
							double value = 0;
							int index = SLBlockCompiler.getCompiler()
									.getGlobalVariableIndex(
											Block.getBlock(socket.getBlockID())
													.getBlockLabel());
							if (index > -1) {
								value = WorkspaceController.getObserver()
										.getGlobalVariable(index);
							} else {
								value = SLBlockCompiler.getCompiler()
										.getRecorderValue(socket.getBlockID());
							}
							newData[i + 1] = value;
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

	/**
	 * Updates the image for this graph, should be scheduled on the AWT thread
	 */
	public void updateImage(double currentTime) {
		synchronized (data) {
			if (needsClear) {
				table.clearTable();
				outputTable.clearTable();
				needsClear = false;
			}
			
			for (double[] newData : data) {
				table.addRow(newData);
				outputTable.addRow(newData);
			}
			this.repaint();
			data.clear();
		}
	}

	/**
	 * Updates all the series names of the chart within this recorder
	 */
	public void updateChartSeriesNames() {
		String[] names = new String[getWorkspaceBlock().getNumSockets()];
		names[0] = TIME;
		for (int i = 1; i < getWorkspaceBlock().getNumSockets(); i++) {
			// i = 0 is reserved for TIME
			BlockConnector socket = getWorkspaceBlock().getSocketAt(i - 1);
			if (socket != null && socket.hasBlock()
					&& socket.getLabel() != null) {
				names[i] = socket.getLabel();
			}
		}
		table.setColumns(names);
		table.updateColumns(names);
		if (outputTable != null) {
			outputTable.setColumns(names);
			outputTable.updateColumns(names);
			this.repaint();
		}
	}

	/**
	 * DIsplays the output frame.
	 */
	private void outsourceGraph() {
		if (outputFrame == null) {
			outputFrame = new TableOutput(getBlock().getBlockLabel());
		}
		outputFrame.setName(getBlock().getBlockLabel());
		outputFrame.setVisible(true);
	}

	/**
	 * An output frame has two mirrors the imae found in the Runtime version, only bigger
	 * and whose size can expand to the screen size.  The corresponding runtime recorder should
	 * be notified of any user generated action such as clearing hte graph, or setting the period.
	 * An output frame also serves the purpose of giving the users  way to export data.  When the
	 * users attempts to export data either as an image or a csv file, the runtime block is put on
	 * LOCK; that is, ant attempts to access or modify the data is rejected.
	 */
	private class TableOutput extends JFrame implements ActionListener {
		private static final long serialVersionUID = 328149080409L;
		/** The button the user presses to save the data */
		private JComponent data;
		/** The button the user presses to clear the data */
		private JComponent clear;
		/** Label to for this period setter */
		private JComponent label;
		/** Button to set the period of graph */
		private CTextField period;

		/**
		 * constructs a new output frame
		 * 
		 * @param name
		 */
		private TableOutput(String name) {
			super(name);
			JPanel panel = new JPanel(new BorderLayout());
			data = RuntimeOutputFrame.getSaveDataButton(this);
			clear = RuntimeOutputFrame.getClearButton(this);
			label = RuntimeOutputFrame.getIntevalLabel();
			period = RuntimeOutputFrame.getPeriodField(this);
			period.setText(Double.toString(PERIOD));

			JPanel buttonPanel = new JPanel();
			buttonPanel.setBackground(getBLockColor());
			buttonPanel.setLayout(new FlowLayout());
			buttonPanel.add(data);
			buttonPanel.add(clear);
			buttonPanel.add(label);
			buttonPanel.add(period);

			outputTable.setBackground(getBLockColor());

			panel.add(buttonPanel, BorderLayout.NORTH);
			panel.add(outputTable, BorderLayout.CENTER);
			this.getContentPane().add(panel, BorderLayout.CENTER);
			this.setSize(640, 480);
			this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		}

		/**
		 * Peforms the various functions of the output frame:
		 * saving image, saving data, clearing, or setting period.
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource().equals(data)) {
				CFileHandler.writeToFile((CButton) e.getSource(), table
						.getCSV());
			} else if (e.getSource().equals(clear)) {
				outputTable.clearTable();
				table.clearTable();
				lastUpdateTime = -PERIOD;
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
		}
	}
}