package slcodeblocks;

import static slcodeblocks.SLBlockProperties.hasProperty;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import renderable.Comment;
import renderable.RenderableBlock;
import runtimecontroller.RuntimeBarGraph;
import runtimecontroller.RuntimeMonitorUpdater;
import runtimecontroller.RuntimeWidget;
import runtimecontroller.RuntimeLineGraph;
import runtimecontroller.RuntimeMonitor;
import runtimecontroller.RuntimeRecorder;
import runtimecontroller.RuntimeSlider;
import runtimecontroller.RuntimeTable;

import workspace.Page;
import workspace.RBParent;
import workspace.Workspace;
import workspace.WorkspaceEvent;
import workspace.WorkspaceListener;
import codeblocks.Block;
import codeblocks.BlockConnector;

public class RuntimeWorkspace extends JLayeredPane
	implements RBParent,WorkspaceListener, ComponentListener
{
	private static final long serialVersionUID = 328149080417L;
    /** Maintains minimum buffer between blocks */
    private static final int BLOCK_GAP = 20;
	
    /** The popup menu of this workspace */
    private JPopupMenu popupmenu;
    /** The autopositioning item: selected if autopositioing turned on */
    private JCheckBoxMenuItem autopositioner;
    
    /** toggles between vertical and horizontal orientation for layout */
    private boolean verticalOrientation;
    
	/** Update Flag: flag to indicate if the runtimeworkspace should be updated */
    private boolean updateRW = false;
    /** True if clock is ticking (update recorders) */
    private boolean running = true;
    
    /** Mapping from Workspace RunBlocks to Runtime RunBlocks */
    private Map<Block, Block> runBlocks;
    /** Mapping from Workspace RecorderBlocks to Runtime RecorderBlocks */
    private Map<Block, Block> recorders;
    /** Mapping from Workspace SliderBlocks to Runtime SliderBlocks */
    private Map<Block, Block> sliders;
    /** HashMap from runtime BlockID to RuntimeButtons */
    private Map<Long, RuntimeWidget> rbs = new HashMap<Long, RuntimeWidget>();
    /** HashMap from workspace blockID to runtime blockID */
    private Map<Long, Long> workspaceRuntimeIDMap = new HashMap<Long, Long>();
    
    public static final Color RUNTIMEBACKGROUNDCOLOR = new Color(40, 40, 40);
    
    
    public RuntimeWorkspace(){
        // set up workspace JComponent
        this.setLayout(null);
        this.setOpaque(true);
        this.setBackground(RUNTIMEBACKGROUNDCOLOR);
        this.setPreferredSize(new Dimension(1000,1000)); //JBT TODO this should grow according to blocks positions
        this.popupmenu = new JPopupMenu();
        this.autopositioner = new JCheckBoxMenuItem("Automate Block Positioning", false);
        this.autopositioner.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				WorkspaceController.getObserver().markChanged();
			}        	
        });
        this.addJMenuItem(autopositioner);
        
        
        // initialize mappings
        runBlocks = new LinkedHashMap<Block, Block>();
        recorders = new HashMap<Block, Block>();
        sliders = new HashMap<Block, Block>();
        
        // add listeners
        this.addComponentListener(this);
        WorkspaceController.getInstance().addWorkspaceListener(this);
        this.addMouseListener(new MouseAdapter(){
            public void mousePressed(MouseEvent e){
            	if (e.isPopupTrigger()){
                	popupmenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            public void mouseReleased(MouseEvent e){
            	if (e.isPopupTrigger()){
            		popupmenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
    
    
    public void addJMenuItem(JMenuItem item){
    	popupmenu.add(item);
    }

    public void componentHidden(ComponentEvent e)
    {
        stopMonitors();
    }

    public void componentMoved(ComponentEvent e)
    {
    }

    public void componentResized(ComponentEvent e)
    {
        if (isActive()) {
            updateRuntimeWorkspace();
        }
        else {
            stopMonitors();
        }
    }

    public void componentShown(ComponentEvent e)
    {
        updateRuntimeWorkspace();
    }

    /**
     * @return true if the runtime workspace is visible to the user
     */
    public boolean isActive()
    {
        if (!isShowing())
            return false;
        return (this.getWidth() * this.getHeight() > 10);
    }

    /**
     * Clears all the internal static data structures of this
     */
    public void reset()
    {
        System.out.println("Resetting RuntimeWorkspace");
        runBlocks.clear();
        recorders.clear();
        sliders.clear();
        rbs.clear();
        removeAll();
        RuntimeWidget.reset(); // clear out old blocks from the map
        autopositioner.setSelected(true);
        revalidate();
        repaint();
    }
    
    public boolean isAutoPositioned(){
    	return autopositioner.isSelected();
    }
    
    public void setAutoPositioned(boolean isAutomated){
    	autopositioner.setSelected(isAutomated);
    	this.positionBlocks(rbs.values());
    }

    private void positionBlocks(Collection<RuntimeWidget> blocks){
    	Rectangle horizontalPositioner = new Rectangle(BLOCK_GAP, BLOCK_GAP, BLOCK_GAP, 0);
    	Rectangle verticalPositioner = new Rectangle(BLOCK_GAP, BLOCK_GAP, 0, BLOCK_GAP);
    	for (RuntimeWidget block : blocks) {
    		if (this.isAutoPositioned()) {
    			//HORIZONTAL
    			if (horizontalPositioner.x + BLOCK_GAP + block.getWidth() > this.getWidth()) {
    				horizontalPositioner.setBounds(
    						BLOCK_GAP,
    						horizontalPositioner.y+horizontalPositioner.height+BLOCK_GAP,
    						BLOCK_GAP,
    						0);
    			}
    			block.setHorizontalX(horizontalPositioner.x);
    			block.setHorizontalY(horizontalPositioner.y);
    			horizontalPositioner.setBounds(
    					horizontalPositioner.x + block.getWidth() + BLOCK_GAP,
    					horizontalPositioner.y,
    					BLOCK_GAP,
    					Math.max(horizontalPositioner.height,block.getHeight()));    		

    			//VERTICAL
    			if (verticalPositioner.y + BLOCK_GAP + block.getHeight() > this.getHeight()) {
    				verticalPositioner.setBounds(
    						verticalPositioner.x+verticalPositioner.width+BLOCK_GAP,
    						BLOCK_GAP,
    						BLOCK_GAP,
    						0);
    			}
    			block.setVerticalX(verticalPositioner.x);
    			block.setVerticalY(verticalPositioner.y);
    			verticalPositioner.setBounds(
    					verticalPositioner.x,
    					verticalPositioner.y + block.getHeight() + BLOCK_GAP,
    					Math.max(verticalPositioner.width,block.getWidth()),
    					BLOCK_GAP);    			
    		}
    		
			if (isVerticalOrientation()) {
				block.setLocation(block.getVerticalX(), block.getVerticalY());
			} else {
				block.setLocation(block.getHorizontalX(), block.getHorizontalY());
			}
    	}
    }

    public void updateRuntimeWorkspace()
    {
        if (!WorkspaceController.isWorkspaceLoading()) {
            //System.out.println("\nUpdating runtime workspace");
            // need to call WorkspaceController for active workspace

            updateRunBlocks();
            updateRecorders();
            updateSliders();

            this.positionBlocks(rbs.values());
            running = true;

            this.revalidate();
            this.repaint();
            this.updateRW = false;
        }
    }

    private void initiateRuntimeWidget(RuntimeWidget button, String label)
    {
        // add graphically
        this.add(button,-1);
        button.setHighlightParent(this);
        this.revalidate();
        // add internally
        rbs.put(button.getBlockID(), button);
        workspaceRuntimeIDMap.put(button.getWorkspaceBlockID(), button.getBlockID());
        
        // set label is label not null
        if (label != null) {
            Block.getBlock(button.getBlockID()).setBlockLabel(label);
            button.redrawFromTop();
            //button.repaint();
        }
    }

    /** updates all runtime RUN blocks: forever, run, runforsometime */
    private void updateRunBlocks()
    {
        for (Block workspaceBlock : runBlocks.keySet()) {
        	RenderableBlock block = RenderableBlock.getRenderableBlock(workspaceBlock.getBlockID());
        	Page p = Workspace.getInstance().getCurrentPage(block);
        	if(p != null){
        		p.reformBlockPosition(block);
        	}
            Block runBlock = runBlocks.get(workspaceBlock);
            if (!rbs.containsKey(runBlock.getBlockID())) {
                // We need to create a new RuntimeBUTTON for this RuntimeBLOCK.
                initiateRuntimeWidget(new RuntimeWidget(runBlock.getBlockID(),
                        workspaceBlock.getBlockID(), this), workspaceBlock.getBlockLabel());
            }
        }
    }

    /** Update all recorders (monitors, line graphs, bar graphs */
    private void updateRecorders()
    {
        for (Block workspaceBlock : recorders.keySet()) {
            // TODO: we should not be checking that socket at 0 has a block,
            // this MUST be true
            // or else it wouldn't update like this. So throw an exception
            if (workspaceBlock.getSocketAt(0).hasBlock()) {
                Block runblock = recorders.get(workspaceBlock);
                if (!rbs.containsKey(runblock.getBlockID())) {
                    if (isBarGraph(workspaceBlock)) {
                        initiateRuntimeWidget(new RuntimeBarGraph(runblock
                                .getBlockID(), workspaceBlock.getBlockID(), this),
                                workspaceBlock.getBlockLabel());
                    }
                    else if (isLineGraph(workspaceBlock)) {
                        initiateRuntimeWidget(new RuntimeLineGraph(runblock
                                .getBlockID(), workspaceBlock.getBlockID(), this),
                                workspaceBlock.getBlockLabel());
                    }
                    else if (isMonitor(workspaceBlock)) {
                        initiateRuntimeWidget(new RuntimeMonitor(runblock
                                .getBlockID(), workspaceBlock.getBlockID(), this),
                                workspaceBlock.getBlockLabel());
                    }else if(isTable(workspaceBlock)){
                        initiateRuntimeWidget(new RuntimeTable(runblock
                                .getBlockID(), workspaceBlock.getBlockID(), this),
                                workspaceBlock.getBlockLabel());
                    }else {
                        throw new RuntimeException(
                                "Need some type of RuntimeRecorder");
                    }
                }
            }
        }
    }

    /** Updates all Runtime SliderBlocks */
    private void updateSliders()
    {
        for (Block workspaceSlider : sliders.keySet()) {
            Block workspaceGlobalNumberBlock = Block.getBlock(workspaceSlider
                    .getSocketAt(0).getBlockID());
            Block runtimeSlider = sliders.get(workspaceSlider);
            if (!rbs.containsKey(runtimeSlider.getBlockID())) {
                // we need to create a new runtimeBUTTON for this runtimeBLOCK
                initiateRuntimeWidget(new RuntimeSlider(runtimeSlider
                        .getBlockID(), workspaceSlider.getBlockID(),
                        workspaceGlobalNumberBlock.getBlockID(), this),
                        workspaceGlobalNumberBlock.getBlockLabel());
            }
        }
    }

    public void stopMonitors()
    {
        running = false;
    }

    /**
     * setGlobalVariable checks if there are any sliders associated to the
     * global variable name.
     * 
     * @returns float[0] is -1 if val < s.minValue, 1 if val > s.minValue, and 0
     *          if val is valid value for the slider or s doesn't exist (0
     *          implies no further action needed) float[1] is s.minValue if s
     *          exists float[2] is s.maxValue if s exists
     */
    public float[] setGlobalVariable(String varName, double value)
    {
        float[] sliderBoundaryCheck = { 0, 0, 0 };
        RuntimeSlider s = null;
        for (Block sliderBlock : sliders.keySet()) {
            String sliderVarName = Block.getBlock(
                    sliderBlock.getSocketAt(0).getBlockID()).getBlockLabel();
            if (sliderVarName.equals(varName)) {
                s = (RuntimeSlider) rbs.get(sliders.get(sliderBlock)
                        .getBlockID());
            }
        }
        if (s != null) {
            // then a slider exists
            sliderBoundaryCheck[1] = s.getMinimum();
            sliderBoundaryCheck[2] = s.getMaximum();
            if (value < s.getMinimum()) {
                sliderBoundaryCheck[0] = -1;
                if (s.getValue() != s.getMinimum()) {
                	s.setValue(s.getMinimum());
                	repaint();
                }
            }
            else if (value > s.getMaximum()) {
                sliderBoundaryCheck[0] = 1;
                if (s.getValue() != s.getMaximum()) {
                	s.setValue(s.getMaximum());
                	repaint();
                }
            }
            else {
            	if(s.getValue() != value) {
            		s.setValue((float) value);
            		repaint();
            	}
            }
        }
        return sliderBoundaryCheck;
    }

    /**
     * Sets the runtime block associated with the specified workspace block
     * (wsBlock) to the running status described by the boolean isRunning. If
     * isRunning is set to true, then the block is given a visually running
     * status, by being highlighted green. If isRunning is false, then the
     * block's running status is reset and the highlight is removed.
     * 
     * @param wsBlockId
     *            the Workspace Block associated with a runtime block
     * @param isRunning
     */
    public void toggleRunBlock(Long wsBlockId, boolean isRunning,
            Color highlight)
    {

        Block rwBlock = runBlocks.get(Block.getBlock(wsBlockId));
        RuntimeWidget rb = rbs.get(rwBlock.getBlockID());
        if (rb != null) {
            if (isRunning)
                rb.setBlockHighlightColor(highlight);
            else
                rb.resetHighlight();
        }
    }

    /**
     * Sets the running status of all runtime blocks to false and then recompiles.
     * Then resets all highlighting for those blocks.
     * (forwards call to RunBlockManager)
     */
    public void turnOffAllBlocks() {
    	RunBlockManager.turnOffAllBlocks();
    }
    
    private boolean isForever(Block block)
    {
        return hasProperty(block, SLBlockProperties.RUNTIME_TYPE);
    }

    private boolean isSetup(Block block)
    {
        return hasProperty(block, SLBlockProperties.IS_SETUP);
    }

    private boolean isMonitor(Block block)
    {
        return hasProperty(block, SLBlockProperties.IS_MONITOR);
    }

    private boolean isSlider(Block block)
    {
        return hasProperty(block, SLBlockProperties.IS_SLIDER);
    }

    private boolean isLineGraph(Block block)
    {
        return hasProperty(block, SLBlockProperties.IS_LINE_GRAPH);
    }

    private boolean isBarGraph(Block block)
    {
        return hasProperty(block, SLBlockProperties.IS_BAR_GRAPH);
    }
    
    private boolean isTable(Block block)
    {
        return hasProperty(block, SLBlockProperties.IS_TABLE);
    }

    public void workspaceEventOccurred(WorkspaceEvent event)
    {
        if (event.getEventType() == WorkspaceEvent.BLOCK_ADDED) {
            Block workspaceBlock = Block.getBlock(event.getSourceBlockID());
            if (workspaceBlock == null)
                return;
            if (workspaceBlock.getBlockID().equals(Block.NULL))
                return;
            if (!hasProperty(workspaceBlock, SLBlockProperties.HAS_RUNTIME_EQUIVALENT))
                return;
            if (!(event.getSourceWidget() instanceof Page))
                return;
            this.processBlockAdded(workspaceBlock);
        }
        else if (event.getEventType() == WorkspaceEvent.BLOCK_REMOVED) {
            Block workspaceBlock = Block.getBlock(event.getSourceBlockID());
            blockRemoved(workspaceBlock);
        }
        else if (event.getEventType() == WorkspaceEvent.BLOCK_RENAMED) {
            Block workspaceBlock = Block.getBlock(event.getSourceBlockID());
            if (workspaceBlock == null)
                return;
            if (workspaceBlock.getBlockID().equals(Block.NULL))
                return;
            // if(workspaceBlock.getProperty(SLBlockProperties.HAS_RUNTIME_EQUIVALENT)
            // == null &&
            // !workspaceBlock.isVariableDeclBlock()) return;
            this.processBlockRenamed(workspaceBlock);
        }
        else if (event.getEventType() == WorkspaceEvent.BLOCKS_DISCONNECTED) {
            Block workspaceBlock = Block.getBlock(event.getSourceLink()
                    .getSocketBlockID());
            if (workspaceBlock == null)
                return;
            if (workspaceBlock.getBlockID().equals(Block.NULL))
                return;
            if (!hasProperty(workspaceBlock, SLBlockProperties.HAS_RUNTIME_EQUIVALENT))
                return;
            this.processBlockDisconnected(workspaceBlock);
        }
        else if (event.getEventType() == WorkspaceEvent.BLOCKS_CONNECTED) {
            Block workspaceBlock = Block.getBlock(event.getSourceLink()
                    .getSocketBlockID());
            if (workspaceBlock == null)
                return;
            if (workspaceBlock.getBlockID().equals(Block.NULL))
                return;
            if (!hasProperty(workspaceBlock, SLBlockProperties.HAS_RUNTIME_EQUIVALENT))
                return;
            this.processBlockConnected(workspaceBlock);
        }
        if (updateRW && !WorkspaceController.isWorkspaceLoading()) {
            updateRuntimeWorkspace();
        }
    }
    
    /**
     * Process any removed blocks - this method is called when no event
     * is generated for the given block (such as removing the entire page).
     */
    void blockRemoved(Block block) {
        if (block == null)
            return;
        if (block.getBlockID().equals(Block.NULL))
            return;
        if (!hasProperty(block, SLBlockProperties.HAS_RUNTIME_EQUIVALENT))
            return;
        this.processBlockRemoved(block);
    }

    /**
     * assumes workspaceBlock, genusName, adn WRMapping are interralated and
     * interconnected corredtly
     * 
     * @param workspaceBlock
     * @param genusName
     * @param WRMapping
     */
    private void addRuntimeBlock(Block workspaceBlock, String genusName,
            Map<Block, Block> WRMapping)
    {
        if (!WRMapping.containsKey(workspaceBlock)) {
            WRMapping.put(workspaceBlock, new Block(genusName));
            updateRW = true;
        }
    }

    /**
     * BLock != null and blockID != null an dblockID != Block.null
     * 
     * @param workspaceBlock
     */
    private void processBlockAdded(Block workspaceBlock)
    {
        if (isForever(workspaceBlock)) {
            addRuntimeBlock(workspaceBlock, "runtime-forever", runBlocks);
            // runtimeBlock.setBlockLabel(workspaceBlock.getBlockLabel());
        }
        else if (isSetup(workspaceBlock)) {
            addRuntimeBlock(workspaceBlock, "runtime-setup", runBlocks);
            // runtimeBlock.setBlockLabel(workspaceBlock.getBlockLabel());
        }
        else if (isSlider(workspaceBlock)) {
            if (workspaceBlock.getSocketAt(0) != null
                    && workspaceBlock.getSocketAt(0).hasBlock()) {
                addRuntimeBlock(workspaceBlock, "runtime-slider", sliders);
            }
        }
        else if (isMonitor(workspaceBlock)) {
            if (workspaceBlock.getSocketAt(0) != null
                    && workspaceBlock.getSocketAt(0).hasBlock()) {
                addRuntimeBlock(workspaceBlock, "runtime-monitor", recorders);
            }
        }
        else if (isBarGraph(workspaceBlock)) {
            if (workspaceBlock.getSocketAt(0) != null
                    && workspaceBlock.getSocketAt(0).hasBlock()) {
                addRuntimeBlock(workspaceBlock, "runtime-bar-graph", recorders);
            }
        }
        else if (isLineGraph(workspaceBlock)) {
            if (workspaceBlock.getSocketAt(0) != null
                    && workspaceBlock.getSocketAt(0).hasBlock()) {
                addRuntimeBlock(workspaceBlock, "runtime-line-graph", recorders);
            }
        }
        else if (isTable(workspaceBlock)) {
            if (workspaceBlock.getSocketAt(0) != null
                    && workspaceBlock.getSocketAt(0).hasBlock()) {
                addRuntimeBlock(workspaceBlock, "runtime-table", recorders);
            }
        }
    }

    /**
     * * assumes workspaceBlock,adn WRMapping are interralated and
     * interconnected corredtly
     * 
     * @param workspaceBlock
     * @param WRMapping
     */
    private void removeRuntimeBlock(Block workspaceBlock,
            Map<Block, Block> WRMapping)
    {
        Block runtimeBlock = WRMapping.get(workspaceBlock);
        if (runtimeBlock != null
                && !runtimeBlock.getBlockID().equals(Block.NULL)) {
            RuntimeWidget rb = rbs.get(runtimeBlock.getBlockID());
            if (rb != null) {
                this.remove(rb);
                rb.disposeWidget();
                rbs.remove(rb.getBlockID());
                workspaceRuntimeIDMap.remove(rb.getWorkspaceBlockID());
            }
            WRMapping.remove(workspaceBlock);
            updateRW = true;
        }
    }

    private void processBlockRemoved(Block workspaceBlock)
    {
        if (isForever(workspaceBlock)) {
            removeRuntimeBlock(workspaceBlock, runBlocks);
        }
        else if (isSetup(workspaceBlock)) {
            removeRuntimeBlock(workspaceBlock, runBlocks);
        }
        else if (isSlider(workspaceBlock)) {
            removeRuntimeBlock(workspaceBlock, sliders);
        }
        else if (isMonitor(workspaceBlock)) {
            removeRuntimeBlock(workspaceBlock, recorders);
        }
        else if (isBarGraph(workspaceBlock)) {
            removeRuntimeBlock(workspaceBlock, recorders);
        }
        else if (isLineGraph(workspaceBlock)) {
            removeRuntimeBlock(workspaceBlock, recorders);
        }
        else if (isTable(workspaceBlock)) {
            removeRuntimeBlock(workspaceBlock, recorders);
        }
    }

    private void renameRuntimeBlock(Block workspaceBlock, String newName,
            Map<Block, Block> WRMapping)
    {
        // get runtime instance
        Block runtimeBlock = WRMapping.get(workspaceBlock);
        // make sure we're not dealing with a null runtime instance
        if (runtimeBlock == null)
            return;
        // reset the runtimeblock's label
        runtimeBlock.setBlockLabel(newName);
        // get renderable
        RuntimeWidget rb = rbs.get(runtimeBlock.getBlockID());
        // make sure we're not dealing with a null renderable instance
        if (rb == null)
            return;
        // repaint renderable
        rb.repaintBlock();
        updateRW = true;
    }

    /**
     * Handles a block renamed event from the workspace triggered from the 
     * specified workspaceBlock.
     * @param workspaceBlock the Block instance in the workspace that triggered the 
     * block renamed event
     */
    private void processBlockRenamed(Block workspaceBlock)
    {
        if (isForever(workspaceBlock)) {
            renameRuntimeBlock(workspaceBlock, workspaceBlock.getBlockLabel(),
                    runBlocks);
        }
        else if (isSetup(workspaceBlock)) {
            renameRuntimeBlock(workspaceBlock, workspaceBlock.getBlockLabel(),
                    runBlocks);
        }
        else if (isSlider(workspaceBlock)) {
            // sliders name's do not equate in ws and rt
            // renameRuntimeBlock(workspaceBlock,
            // workspaceBlock.getBlockLabel(), runBlocks);
        }
        else if (isMonitor(workspaceBlock)) {
            renameRuntimeBlock(workspaceBlock, workspaceBlock.getBlockLabel(),
                    recorders);
        }
        else if (isBarGraph(workspaceBlock)) {
            renameRuntimeBlock(workspaceBlock, workspaceBlock.getBlockLabel(),
                    recorders);
            // Update series names
            if(recorders.get(workspaceBlock) != null){
            	RuntimeRecorder renderable = (RuntimeRecorder)rbs.get(recorders.get(workspaceBlock).getBlockID());
            	renderable.updateDomain();
            	renderable.updateChartSeriesNames();
            }
        }
        else if (isLineGraph(workspaceBlock)) {
            renameRuntimeBlock(workspaceBlock, workspaceBlock.getBlockLabel(),
                    recorders);
            // Update series names
            if(recorders.get(workspaceBlock) != null){
            	RuntimeRecorder renderable = (RuntimeRecorder)rbs.get(recorders.get(workspaceBlock).getBlockID());
            	renderable.updateDomain();
            	renderable.updateChartSeriesNames();
            }
        }
        else if (isTable(workspaceBlock)) {
            renameRuntimeBlock(workspaceBlock, workspaceBlock.getBlockLabel(),
                    recorders);
            // Update series names
            if(recorders.get(workspaceBlock) != null){
            	RuntimeRecorder renderable = (RuntimeRecorder)rbs.get(recorders.get(workspaceBlock).getBlockID());
            	renderable.updateDomain();
            	renderable.updateChartSeriesNames();
            }
        }
        else if (workspaceBlock.isVariableDeclBlock()) {
        	// NOTE: this doesn't really do anything right now
            for (Block workspaceSlider : sliders.keySet()) {
                if (workspaceSlider.getSocketAt(0).getBlockID().equals(
                        workspaceBlock.getBlockID())) {
                    renameRuntimeBlock(workspaceSlider, workspaceBlock
                            .getBlockLabel(), sliders);
                    return;
                }
            }
            
            for (Block workspaceRecorder : recorders.keySet()) {
            	//need to check if stubs of the changed declaration block are attached to any of the recorders.
                for (BlockConnector socket : workspaceRecorder.getSockets()) {
                    if (socket.hasBlock()) {
                    	Block socketBlock = Block.getBlock(socket.getBlockID());
                    	boolean updateRecorder = false;
                        if (socket.getBlockID().equals(
                                workspaceBlock.getBlockID())) {
                        	updateRecorder = true;
                        	RuntimeRecorder recorder = (RuntimeRecorder) rbs
                            .get(recorders.get(workspaceRecorder)
                                    .getBlockID());
                        	if (recorder != null) {
                        		System.out.println("update domain of recorder");
                        		recorder.updateDomain();
                        	}
                        } else if (SLBlockProperties.getParent(socketBlock) != null) {
                        	Block parent = SLBlockProperties.getParent(socketBlock);
                        	if(parent.getBlockID().equals(workspaceBlock.getBlockID())) {
                        		updateRecorder = true;
                        		//note from ria: could not find a way through JFreeChart to update
                        		//the series name of a bar graph!!
                        		//so instead i'm removing the bargraph from the runtime workspace and
                        		//re-adding it again with the updated workspace block
                        		//bargraph does this same process when a block is disconnected and connected from it
                        		if (isBarGraph(workspaceRecorder)) {
                                    this.removeRuntimeBlock(workspaceRecorder, recorders);
                                    this.addRuntimeBlock(workspaceRecorder, "runtime-bar-graph",
                                                    recorders);
                                } else {
	                        		RuntimeRecorder recorder = (RuntimeRecorder) rbs
	                                .get(recorders.get(workspaceRecorder)
	                                        .getBlockID());
	                            	if (recorder != null) {
	                            		//recorder.updateDomain();
	                            	}
                                }
                        	}
                        }

                        if(updateRecorder) {
                        	
                        	break;
                        }
                    }
                }
            }
        }
        else if (workspaceBlock.isDataBlock()) {
        	// Should find a way to make it so the domain updates correctly when a data block changes values.
            for (Block workspaceRecorder : recorders.keySet()) {
                for (BlockConnector socket : workspaceRecorder.getSockets()) {
                    if (socket.hasBlock()) {
                        if (socket.getBlockID().equals(
                                workspaceBlock.getBlockID())) {
                            RuntimeRecorder recorder = (RuntimeRecorder) rbs
                                    .get(recorders.get(workspaceRecorder)
                                            .getBlockID());
                            if (recorder != null) {
//                                System.out.println("update domain of recorder");
//                                recorder.updateDomain();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Handles a block disconnected even that occurred in the workspace
     * from the specified workspaceBlock
     * @param workspaceBlock the Block instance that triggered a block disconnection event
     */
    private void processBlockDisconnected(Block workspaceBlock)
    {
        if (isSlider(workspaceBlock)) {
            removeRuntimeBlock(workspaceBlock, sliders);
        }
        else if (isMonitor(workspaceBlock)) {
            removeRuntimeBlock(workspaceBlock, recorders);
        }
        else if (isBarGraph(workspaceBlock)) {
        	// If no block in first socket, remove the graph from the runtime workspace
        	if(workspaceBlock.getSocketAt(0).getBlockID() == -1){
        		this.removeRuntimeBlock(workspaceBlock,recorders);
        	} else {
        		RuntimeRecorder renderable = (RuntimeRecorder) rbs.get(recorders.get(workspaceBlock).getBlockID());
        		renderable.updateDomain();
        		renderable.updateChartSeriesNames();
        	}
        }
        else if (isLineGraph(workspaceBlock)) {
        	// If no block in first socket, remove the graph from the runtime workspace
        	if(workspaceBlock.getSocketAt(0).getBlockID() == -1){
        		this.removeRuntimeBlock(workspaceBlock,recorders);
        	} else {
        		RuntimeRecorder renderable = (RuntimeRecorder) rbs.get(recorders.get(workspaceBlock).getBlockID());
        		renderable.updateDomain();
        		renderable.updateChartSeriesNames();
        	}
        }
        else if (isTable(workspaceBlock)) {
        	// If no block in first socket, remove the table from the runtime workspace
        	if(workspaceBlock.getSocketAt(0).getBlockID() == -1){
        		this.removeRuntimeBlock(workspaceBlock,recorders);
        	} else {
        		RuntimeRecorder renderable = (RuntimeRecorder) rbs.get(recorders.get(workspaceBlock).getBlockID());
        		renderable.updateDomain();
        		renderable.updateChartSeriesNames();
        	}
        }
    }

    /**
     * Handles a block connection event occuring from the specified workspace block
     * @param workspaceBlock the Block instance in the workspace that triggered a block 
     * connection event
     */
    private void processBlockConnected(Block workspaceBlock)
    {
        if (workspaceBlock == null)
            return;
        if (isSlider(workspaceBlock)) {
            addRuntimeBlock(workspaceBlock, "runtime-slider", sliders);
        }
        else if (isMonitor(workspaceBlock)) {
            addRuntimeBlock(workspaceBlock, "runtime-monitor", recorders);
        }
        else if (isBarGraph(workspaceBlock)) {
            // If no block in runtime workspace for this graph, make one
        	if(recorders.get(workspaceBlock) == null){
        		this.addRuntimeBlock(workspaceBlock, "runtime-bar-graph", recorders);
        	} else {
        		RuntimeRecorder renderable = (RuntimeRecorder) rbs.get(recorders.get(workspaceBlock).getBlockID());
        		renderable.updateDomain();
        		renderable.updateChartSeriesNames();
        	}
        }
        else if (isLineGraph(workspaceBlock)) {
            // If no block in runtime workspace for this graph, make one
        	if(recorders.get(workspaceBlock) == null){
        		this.addRuntimeBlock(workspaceBlock, "runtime-bar-graph", recorders);
        	} else {
        		RuntimeRecorder renderable = (RuntimeRecorder) rbs.get(recorders.get(workspaceBlock).getBlockID());
        		renderable.updateDomain();
        		renderable.updateChartSeriesNames();
        	}
        }
        else if (isTable(workspaceBlock)) {
            // If no block in runtime workspace for this table, make one
        	if(recorders.get(workspaceBlock) == null){
        		this.addRuntimeBlock(workspaceBlock, "runtime-bar-graph", recorders);
        	} else {
        		RuntimeRecorder renderable = (RuntimeRecorder) rbs.get(recorders.get(workspaceBlock).getBlockID());
        		renderable.updateDomain();
        		renderable.updateChartSeriesNames();
        	}
        }
    }

    /**
     * Refreshes the values of all runtime monitors and graphs
     * 
     * @param currentTime current StarLogo time
     */
    public void refreshMonitorsAndGraphs(double currentTime)
    {
        for (Block runtimeRecorder : recorders.values()) {
            // make sure we're not dealing with a null block instance
            if (runtimeRecorder == null)
                continue;
            // get renderable
            RuntimeRecorder renderable = (RuntimeRecorder) rbs
                    .get(runtimeRecorder.getBlockID());
            // make sure renderable equivalent has been instantiated
            if (renderable == null)
                continue;
            // update monitor
            renderable.updateChart(currentTime);
            
            // update monitor on the Swing thread
            SwingUtilities.invokeLater(new RuntimeMonitorUpdater(renderable, currentTime));
        }
    }

    /** The Application should call this method when the VM ticks. */
    public void vmTicked(double slTime) {
        if (running) {
            refreshMonitorsAndGraphs(slTime);
        }
    }

    // //////////////////////
    // SAVING AND LOADING //
    // //////////////////////  
    /**
     * returns a save string for this RuntimeWorkspace
     */
    public String getSaveString(ControlPanelVals vals) {
        StringBuffer saveString = new StringBuffer();
        saveString.append("<RuntimeWorkspace autoposition=\"" + this.isAutoPositioned() +"\">");
        
        for (RuntimeWidget runtimeblock : this.rbs.values()) {
        	saveString.append(runtimeblock.getSaveString());
        }
    	saveString.append("<RightWidth>" + vals.controlPanelRightWidth + "</RightWidth>");
    	saveString.append("<BottomHeight>" + vals.controlPanelBottomHeight + "</BottomHeight>");
    	if (this.isVerticalOrientation()) {
        	saveString.append("<SplitPane>horizontal</SplitPane>");        		
    	} else {
        	saveString.append("<SplitPane>vertical</SplitPane>");        		
    	}
        saveString.append("</RuntimeWorkspace>");
        return saveString.toString();
    }

    /**
     * Returns a ControlPanelVals and loads block positions and orientation information for a RuntimeWorkspace 
     * @param data
     * @param vals
     * @return
     */
    public  ControlPanelVals loadStringData(String data, ControlPanelVals vals) {
    	//below we attempt to load the runtime workspace from a string of data

    	int start = data.indexOf("<RuntimeWorkspace");
        if (start < 0)
            return vals;
        int end = data.indexOf("`terrains`", start);
        if (end < 0)
            end = data.length();
        data = data.substring(start, end);
        
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document doc;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(new InputSource(new StringReader(data)));
            Element root = doc.getDocumentElement();
            if ( root.getAttribute("autoposition").equals("false")) {
            	setAutoPositioned(false);
            } else {
            	setAutoPositioned(true);            	
            }
            if (root.getNodeName().equals("RuntimeWorkspace")){
            	NodeList nodeList = root.getChildNodes(); 
            	Node node;
            	long blockID;
            	RenderableBlock rb;
            	RuntimeWidget rw;
            	
            	for (int i = 0; i<nodeList.getLength(); i++) {
            		node = nodeList.item(i);            		
            		if (node.getNodeName().equals("RuntimeWidget")) {
                		blockID = Long.parseLong(node.getAttributes().getNamedItem("id").getNodeValue());
                		rb = RenderableBlock.getRenderableBlock(blockID);
                		rw = this.rbs.get(this.workspaceRuntimeIDMap.get(rb.getBlockID()));
                		rw.setLocation(0,0);
                		NodeList rwNodeList = node.getChildNodes();
                		Node rwChildNode;
                		for (int j=0; j<rwNodeList.getLength(); j++) {
                			rwChildNode = rwNodeList.item(j);
                			if (rwChildNode.getNodeName().equals("Location")) {
                				//This is the case where there is only one location for both vertical and horizontal
                				if (!isAutoPositioned()) {
	                				Point location = new Point();
	                				RenderableBlock.extractLocationInfo(rwChildNode, location);
	                				rw.setVerticalX(location.x);
	                				rw.setVerticalY(location.y);
	                				rw.setHorizontalX(location.x);
	                				rw.setHorizontalY(location.y);
                				}
                			} else if (rwChildNode.getNodeName().equals("VerticalLocation")) {
                				Node locationNodeChild;
                        		for (int k=0; k<rwChildNode.getChildNodes().getLength(); k++) {
                        			locationNodeChild = rwChildNode.getChildNodes().item(k);
                        			if (locationNodeChild.getNodeName().equals("Location")) {
                        				if (!isAutoPositioned()) {
        	                				Point location = new Point();
        	                				RenderableBlock.extractLocationInfo(locationNodeChild, location);
        	                				rw.setVerticalX(location.x);
        	                				rw.setVerticalY(location.y);
                        				}                        				
                        			}
                        		}
                			} else if (rwChildNode.getNodeName().equals("HorizontalLocation")) { 
                				Node locationNodeChild;
                        		for (int k=0; k<rwChildNode.getChildNodes().getLength(); k++) {
                        			locationNodeChild = rwChildNode.getChildNodes().item(k);
                        			if (locationNodeChild.getNodeName().equals("Location")) {
                        				if (!isAutoPositioned()) {
        	                				Point location = new Point();
        	                				RenderableBlock.extractLocationInfo(locationNodeChild, location);
        	                				rw.setHorizontalX(location.x);
        	                				rw.setHorizontalY(location.y);
                        				}                        				
                        			}
                        		}
                			} else if  (rwChildNode.getNodeName().equals("Comment")) {
                				rw.setComment(Comment.loadComment(rwChildNode.getChildNodes(), rw));
                                if (rw.getComment() != null) {
                                    rw.getComment().setParent(rw.getParent());
                                }
                			} else {
                				System.out.println("Unknown Node in RuntimeWidget: " + rwChildNode);
                			}
                		}
            		} else if (node.getNodeName().equals("RightWidth")){
            			int width = Integer.parseInt(node.getTextContent());
            			if (width>0) {
            				vals.controlPanelRightWidth = (width);
            			}
            		} else if (node.getNodeName().equals("BottomHeight")){
            			int height = Integer.parseInt(node.getTextContent());
            			if (height>0) {
            				vals.controlPanelBottomHeight = (height);            			
            			}
            		} else if (node.getNodeName().equals("SplitPane")) {
            			if (node.getTextContent().contains("vertical")) {
            				this.setVerticalOrientation(false);
            			} else if (node.getTextContent().contains("horizontal")) {
            				this.setVerticalOrientation(true);
            			}
            		} else {
            			System.out.println("Unknown Node: " + node);
            		}
            	} 
            }
            
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 

        return vals;
    }

    // ////////////////////////////////
    // RBParent implemented methods //
    // ////////////////////////////////
    private static final Integer BLOCK_HIGHLIGHT_LAYER = new Integer(0);
    private static final Integer BLOCK_LAYER = new Integer(1);

    public void addToBlockLayer(Component c)
    {
        this.add(c, BLOCK_LAYER);
    }

    public void addToHighlightLayer(Component c)
    {
        this.add(c, BLOCK_HIGHLIGHT_LAYER);
    }


	/**
	 * @return the verticalOrientation
	 */
	public boolean isVerticalOrientation() {
		return verticalOrientation;
	}


	/**
	 * @param verticalOrientation the verticalOrientation to set
	 */
	public void setVerticalOrientation(boolean verticalOrientation) {
		this.verticalOrientation = verticalOrientation;
	}


}
