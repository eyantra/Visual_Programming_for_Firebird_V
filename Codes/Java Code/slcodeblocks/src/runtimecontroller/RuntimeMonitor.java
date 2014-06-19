package runtimecontroller;

import slcodeblocks.RuntimeWorkspace;
import slcodeblocks.SLBlockCompiler;
import slcodeblocks.WorkspaceController;
import codeblocks.Block;

/**
 * A RuntimeMonitor is a RuntimeButton (with all socket/label 
 * restrictions; see documentation for RuntimeButton).  
 * It has a corresponding workspace block which will be 
 * referred to as the wsblock.  RuntimeLineGraphs should 
 * have the following property:
 * this.blockLabel = wsblock.blockLabel;
 * this.pageLabel = null;
 * this.socketLabels = empty set;
 * this.sockets = empty set;
 * this.plug = null;
 * this.afterConnector = null;
 * this.beforeConnector = null;
 * 
 * A RuntimeMonitor serves the purpose of displaying 
 * output data as a string by wrapping a monitorable 
 * equal to the block connected to wsblock.sockets.  
 * The monitorable will be refered to as the "domain" 
 * of this RuntimeMonitor.  From time to time, the domain 
 * of a RuntimeLineGraph changes such that the number of 
 * series in which it must consider changes (often a 
 * change in the number of sockets translate into a 
 * change in the domain).  To synchronize the domain 
 * changes to the line graph's series data, the user 
 * should invoke the UpdateDomain method.
 * 
 * A RuntimeMonitor will only update its series 
 * once ever PERIOD amout of time has passed.  After 
 * PERIOD number of seconds have passed, the graph 
 * will make a call to the observer and asks for the 
 * current numerical state of its domain.
 */
public class RuntimeMonitor extends RuntimeWidget implements RuntimeRecorder {
	private static final long serialVersionUID = 328149080406L;
	/** Number of decimals places to show based on the amount of error (1/epsilon)*/
	private static final double EPSILON = 1000;

	/** the label text for this monitor set by updateChart read by updateImage */
	private String label;
	
	/** Boolean switch when updateImage should execute */
	private Boolean dataReady;

	/**
	 * Constructs a new RuntimeMonitor with an EMPTY DOMAIN!
	 * 
	 * @param blockID
	 * @param workspaceblockID
	 * 
	 * @requires workspaceBlockID != Block.NULL && != null
	 */
	public RuntimeMonitor(Long blockID, Long workspaceblockID,
			RuntimeWorkspace runtimeWorkspace) {
		super(blockID, workspaceblockID, runtimeWorkspace);
		updateDomain();
		dataReady = false;
	}

	/**
	 * Updates the domain of this lines graph (or the set of monitorables)
	 */
	public void updateDomain() {
		SLBlockCompiler.getCompiler().compileBlocks(getWorkspaceBlockID());
	}

	/**
	 * Updates the the data by storing, processing, and rendering the changes,
	 * Only do this if the the data is not being locked and one period cycle has passed
	 */
	public void updateChart(double currentTime) {
		Block workspaceBlock = getWorkspaceBlock();
		label = "" + workspaceBlock.getBlockLabel() + " : ";
		synchronized (dataReady) {
			if (workspaceBlock.getSocketAt(0) != null
					&& workspaceBlock.getSocketAt(0).hasBlock()) {
				// get the socket block that this RuntimeMonitor will observe
				// note that the socket block, or "ARTICLE", may not be null since
				// hasBlock() returned true
				Block socketBlock = Block.getBlock(workspaceBlock.getSocketAt(0)
						.getBlockID());
				if (socketBlock.getPlugKind().equals("number")
						|| socketBlock.getPlugKind().equals("number-inv")) {
					double value;
					if (socketBlock.isVariableDeclBlock()) {
						int index = SLBlockCompiler
								.getCompiler()
								.getGlobalVariableIndex(socketBlock.getBlockLabel());
						if (index > -1) {
							value = WorkspaceController.getObserver()
									.getGlobalVariable(index);
						} else
							value = 0;
					} else {
						value = SLBlockCompiler.getCompiler().getRecorderValue(
								socketBlock.getBlockID());
					}
					if (Math.abs(value - Double.MAX_VALUE) < 1) {
						label += "Infinity"; // if ten units away from largest
						// number
					} else if (Math.abs(value + Double.MAX_VALUE) < 1) {
						label += "-Infinity"; // if ten units away from smalles
						// number
					} else {
						label += Math.round(value * EPSILON) / EPSILON; // nearest
						// thousandth
					}
				} else if (socketBlock.getPlugKind().equals("boolean")) {
					boolean value = SLBlockCompiler.getCompiler()
							.getRecorderBooleanValue(socketBlock.getBlockID());
					label += value ? "True" : "False";
				} else if (socketBlock.getPlugKind().equals("string")) {
					label += SLBlockCompiler.getCompiler().getRecorderStringValue(
							socketBlock.getBlockID());
				}
				dataReady = true;
			}
		}
	}

	/**
	 * Updates the image for this graph, should be scheduled on the AWT thread
	 */
	public void updateImage(double currentTime) {
		synchronized (dataReady) {
			if (dataReady) {
				Block runtimeBlock = getBlock();
	
				runtimeBlock.setBlockLabel(label);
				this.repaintBlock();
				this.repaint();
				dataReady = false;
			}
		}
	}

	/**
	 * Empty implemented interface method.
	 */
	public void updateChartSeriesNames() {
	}
}