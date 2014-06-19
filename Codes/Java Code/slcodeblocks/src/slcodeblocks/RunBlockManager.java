
package slcodeblocks;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.ImageIcon;

import renderable.BlockImageIcon;
import renderable.RenderableBlock;
import renderable.BlockImageIcon.ImageLocation;

import codeblocks.Block;
import codeblocks.BlockConnector;
import codeblocks.BlockConnectorShape;

import workspace.Page;
import workspace.Workspace;
import workspace.WorkspaceEvent;
import workspace.WorkspaceListener;
import workspace.WorkspaceWidget;

import static slcodeblocks.SLBlockProperties.RuntimeType.*;
import static slcodeblocks.SLBlockProperties.isCollision;

/**
 * RunBlockManager manages the behavior and TNG-specific data of run blocks
 * such as forever, run for some time, and run once.  RBM does the following: 
 * - Listens to the switches of run blocks such as forevers to manage the 
 * "switch" icon that appears on the blocks.
 * - Reports whether a run block is "running" or not given a block id
 * - Updates highlighting of blocks when run state changes
 */

public class RunBlockManager implements MouseListener, WorkspaceListener{

    private Workspace workspace;
    
    private final static Color HIGHLIGHT = new Color(120, 255, 100);
    
    /** Maps from runblock block long ids to a boolean flag that if true means its corresponding block is currently "running"*/
    private static HashMap<Long, Boolean> runBlockIsRunning = new HashMap<Long, Boolean>();
    
    private final static ImageLocation FOREVER_SWITCH_LOCATION = ImageLocation.SOUTHWEST;
    
    //THESE GENUS NAMES ARE DUPLICATED IN LANG_DEF.XML
    //make sure these names are in sync with what's indicated in the xml
    private final static String FOREVER_GENUS_NAME = FOREVER.getString();
    private final static String RUNONCE_GENUS_NAME = RUNONCE.getString();
    private final static String RUN_GENUS_NAME = RUNFORSOMETIME.getString();
    
    private final static ArrayList<String> listOfBreeds = new ArrayList<String>();
    
    /** The collision blocks in the workspace. */
    private static final Set<Long> myCollisionBlocks = new HashSet<Long>();
    /** The number of blocks currently running in our map. */
    private static int myBlocksRunningCount = 0;
    
    //off "switch" imageicon
    private static ImageIcon offIcon;
    private static ImageIcon onIcon;
    
    public RunBlockManager(Workspace workspace){
        this.workspace = workspace;
        
        // initialize switch icons
        initSwitchIcons();
        
        //populate runBlockIsRunning map
        initRunBlockMapping();
    }
    
    /**
     * Returns true iff the run block with the specified blockID is running;
     * false otherwise
     * @param blockID the Long blockID of the run block
     * @return true iff the run block with the specified blockID is running;
     * false otherwise
     */
    public static boolean isBlockRunning(Long blockID){
        return runBlockIsRunning.containsKey(blockID) &&
               runBlockIsRunning.get(blockID).booleanValue();
    }
    
    /** Returns true if any blocks are running. */
    public static boolean isAnyBlockRunning() {
        return myBlocksRunningCount > 0;
    }
    
    /**
     * Returns true if the Block with the specified Block id is a "run" block; false otherwise. 
     * Run blocks include forever, run, and runonce blocks.  
     * @param blockID Long Block id of Block to check
     * @return true if the Block with the specified Block id is a "run" block; false otherwise.
     */
    public static boolean isRunBlock(Long blockID){
        Block block = Block.getBlock(blockID);
        return block.getGenusName().equals(FOREVER_GENUS_NAME) || block.getGenusName().equals(RUN_GENUS_NAME) ||
            block.getGenusName().equals(RUNONCE_GENUS_NAME);
    }
    
    /** Returns all the collision blocks in the workspace. */
    public static Iterable<Long> getCollisionBlocks() {
        return myCollisionBlocks;
    }
    
    /**
     * Resets all internal data collected and maintained by this.
     */
    public void reset(){
        runBlockIsRunning.clear();
        listOfBreeds.clear();
        myCollisionBlocks.clear();
        myBlocksRunningCount = 0;
    }
    
    /**
     * Loads the off switch icon for run blocks.
     */
    private static void initSwitchIcons(){
        String workingDirectory = ((System.getProperty("application.home") != null) ?
                System.getProperty("application.home") :
                    System.getProperty("user.dir"));
        
        offIcon = new ImageIcon(workingDirectory + "/support/forever_switch_off.png");
        onIcon  = new ImageIcon(workingDirectory + "/support/forever_switch_on.png");
    }
    
    /**
     * Adds the specified block to the runBlockIsRunning mapping.  
     * Will also add this as a mouse listener to the switch image on the run block.
     * @param block
     */
    private void addNewRunBlock(RenderableBlock block){
        //TODO need to check if run block is up to date
        Block b = Block.getBlock(block.getBlockID());
        int indexToAdd = 0;
        boolean blockUpdated = false;
        for(BlockConnector socket : b.getSockets()){
            //check the socket types of the block and find the first command block
            if(socket.getKind().equals(BlockConnectorShape.getCommandShapeName())){ 
                //grab the block connector command kind from BCS,
                //if its the first command type, then just replace it with the first breed, then 
                //add the rest as new connectors
                if(indexToAdd < listOfBreeds.size() && !socket.getLabel().equals(listOfBreeds.get(indexToAdd))){
                    socket.setLabel(listOfBreeds.get(indexToAdd));
                }
                //update index to add
                indexToAdd++;
            }
            
            if(!blockUpdated)
                blockUpdated = true;
        }
        
        //now add the remaining breeds to add if there are any
        while(indexToAdd < listOfBreeds.size()){
            b.addSocket(BlockConnectorShape.getCommandShapeName(), BlockConnector.PositionType.SINGLE, listOfBreeds.get(indexToAdd), false, false, Block.NULL);
            indexToAdd++;
            if(!blockUpdated)
                blockUpdated = true;
        }
        
        if(blockUpdated && block.isVisible())
            block.repaintBlock();
        
        runBlockIsRunning.put(block.getBlockID(), Boolean.FALSE);
        System.out.println("adding new run block: "+block);
        //only add mouse listener if block is actually in a page
        if(block.getParentWidget() instanceof Page){
            block.getImageIconAt(FOREVER_SWITCH_LOCATION).addMouseListener(this);
        }
    }
    
    /**
     * Retrieves all the workspace blocks that are run blocks from the workspace
     * and populates the runBlockIsRunning mapping accordingly
     */
    private void initRunBlockMapping(){
        for(RenderableBlock block : workspace.getRenderableBlocksFromGenus(FOREVER_GENUS_NAME)){
            addNewRunBlock(block);
        }
    }

    public void workspaceEventOccurred(WorkspaceEvent event) {
        Long id = null;
        
        switch(event.getEventType()){
        case WorkspaceEvent.BLOCK_ADDED:
            //check if block added is a forever block
            if(isRunBlock(event.getSourceBlockID())){
                addNewRunBlock(RenderableBlock.getRenderableBlock(event.getSourceBlockID()));
            }
            else if (isCollision(event.getSourceBlockID())) {
                myCollisionBlocks.add(event.getSourceBlockID());
            }
            break;
        case WorkspaceEvent.BLOCK_REMOVED:
            //check if block removed is a forever block
            if(isRunBlock(event.getSourceBlockID())){
                runBlockIsRunning.remove(event.getSourceBlockID());
                //TODO remove as listener as well
            }
            else if (isCollision(event.getSourceBlockID())) {
                myCollisionBlocks.add(event.getSourceBlockID());
            }
            break;
        case WorkspaceEvent.PAGE_ADDED:
            //iterate through all run blocks currently active and add socket
            String breedName = ((Page)event.getSourceWidget()).getPageName();
            
            // only update sockets if the page added corresponds to a breed
            if(BreedManager.isValidBreedName(breedName) && BreedManager.isExistingBreed(breedName)){
                //System.out.println("received page added event(rbm): "+breedName);
                if(!WorkspaceController.isWorkspaceLoading())
                    updateRunBlockSockets(breedName, true);
                listOfBreeds.add(breedName);
            }
            break;
        case WorkspaceEvent.PAGE_REMOVED:
            //iterate through all run blocks currently active and remove socket
            //corresponding to this page.  The page may not be a breed page,
        	//but if not updateRunBlockSockets will simply have no effect
            breedName = ((Page)event.getSourceWidget()).getPageName();
            updateRunBlockSockets(breedName, false);
            listOfBreeds.remove(breedName);
            break;
        case WorkspaceEvent.PAGE_RENAMED:
            //update the socket labels of run blocks
            breedName = ((Page)event.getSourceWidget()).getPageName();
            updateRunBlockSockets(event.getOldNameOfSourceWidget(), breedName);
            if (listOfBreeds.contains(event.getOldNameOfSourceWidget()))
            	listOfBreeds.set(listOfBreeds.indexOf(event.getOldNameOfSourceWidget()), breedName);
            break;
        case WorkspaceEvent.BLOCKS_CONNECTED:
        case WorkspaceEvent.BLOCKS_DISCONNECTED:
            if (event.getSourceLink() != null) {
                id = SLBlockProperties.getTopBlockID(event.getSourceLink().getSocketBlockID());
                DependencyManager.updateDependencies(id, id);
            }
            break;
        case WorkspaceEvent.BLOCK_RENAMED:
            Block b = Block.getBlock(event.getSourceBlockID());
            if (b.isDataBlock())
                id = SLBlockProperties.getTopBlockID(event.getSourceBlockID());
            break;
        case WorkspaceEvent.BLOCK_GENUS_CHANGED:
            id = SLBlockProperties.getTopBlockID(event.getSourceBlockID());
            break;
        }
        
        // Now turn off any running blocks that are dependent on 
        // stacks that were changed.
        boolean toggled = false;    // any block run states toggled?
        if (id != null) {
            if (SLBlockProperties.isForeverRunBlock(id) && isBlockRunning(id)) {
                toggled = true;
                toggleBlock(id, false);
            }
            else if (isCollision(id)) {
                // Turn off ALL running blocks
                for (Long blockID : runBlockIsRunning.keySet()) {
                    if (isBlockRunning(blockID)) { 
                        toggleBlock(blockID, false);
                        toggled = true;
                    }
                }
            }
            else {
                Set<Long> rbs = DependencyManager.getDependentRunBlocks(id);
                for (Long block : rbs) {
                    if (isBlockRunning(block)) {
                        toggleBlock(block, false);
                        toggled = true;
                    }
                }
            }
            
            if (toggled)
                SLBlockCompiler.getCompiler().compile();
        }
        
        // Unhighlight any disconnected blocks.
        if (toggled && event.getEventType() == WorkspaceEvent.BLOCKS_DISCONNECTED)
            traverseStackAndHighlight(RenderableBlock.getRenderableBlock(event.getSourceLink().getPlugBlockID()), false);
    }
    
    /**
     * Sets the running status of all runtime blocks to false and then recompiles.
     * Then resets all highlighting for those blocks.
     */
    public static void turnOffAllBlocks() {
    	boolean toggled = false;
    	
        // Turn off ALL running blocks
        for (Long blockID : runBlockIsRunning.keySet()) {
            if (isBlockRunning(blockID)) { 
                toggleBlock(blockID, false);
                toggled = true;
            }
        }
        
        // recompile if any blocks were toggled
        if (toggled)
            SLBlockCompiler.getCompiler().compile();
    }
    
    /**
     * Updates all the visible run blocks with the specified breed name.  If breedAdded is true, 
     * then it adds a new socket to all run blocks.  If breedAdded is false, then it removes the
     * socket in each run block that has the specified breedName as its label.
     * @param breedName
     * @param breedAdded boolean value to indicate if a breed has just been added or removed
     */
    private void updateRunBlockSockets(String breedName, boolean breedAdded){
        int initIndexOf = -1;
        int indexOf = -1;
        if(!breedAdded){
            initIndexOf = listOfBreeds.indexOf(breedName);
            indexOf = initIndexOf;
        }
        for(Long id : runBlockIsRunning.keySet()){
            Block block = Block.getBlock(id);
            if(breedAdded){
                block.addSocket(BlockConnectorShape.getCommandShapeName(), BlockConnector.PositionType.SINGLE, breedName, false, false, Block.NULL);
            }else{
                indexOf = initIndexOf;
                while(indexOf < block.getNumSockets()){
                    if(block.getSocketAt(indexOf).getLabel().equals(breedName)){
                        block.removeSocket(indexOf);
                        System.out.println("removing socket at "+indexOf+" of block: "+block);
                    }
                    indexOf++;
                }
            }
            
            RenderableBlock.getRenderableBlock(id).repaintBlock();
        }
    }
    
    /**
     * Updates the socket labels of all run blocks with the specified newBreedName. 
     */
    private void updateRunBlockSockets(String oldBreedName, String newBreedName){
    	int index;
        for(Long id : runBlockIsRunning.keySet()){
            Block block = Block.getBlock(id);
            index = listOfBreeds.indexOf(oldBreedName);
            if(block.getGenusName().equals(RUN_GENUS_NAME))
            	index = index+1;  //because it has the "secs" socket
            block.getSocketAt(index).setLabel(newBreedName);
            RenderableBlock.getRenderableBlock(id).repaintBlock();
        }
    }
    
    /**
     * Returns the default highlight color associated with a running
     * block.
     */
    public static Color getHighlightRunColor(){
        return HIGHLIGHT;
    }
    
    /**
     * Alerts the RunBlockManager that the vm has finished running a block.
     * @return True if all runOnce stacks are done
     */
    public static boolean runForSomeTimeBlockDone(Long blockID) {
        if (runBlockIsRunning.get(blockID) != null) {
            toggleBlock(blockID, false);
        }
        return SLBlockCompiler.getCompiler().runForSomeTimeDone(blockID);
    }
    
    /**
     * Toggles the running status of a run block with the specified block ID.
     * @param blockID block ID of the desired run block
     */
    public static void toggleBlock(Long blockID) {
        toggleBlock(blockID, !isBlockRunning(blockID));
    }
    
    /**
     * Toggles the "running" status of a run block with the specified block ID.
     * Updates its "running" status internally and graphically
     * @param on Whether the block should be on
     * @param blockID Long blockID of the desired run block
     */
    synchronized public static void toggleBlock(Long blockID, boolean on) {
    	boolean isRunning = !on;
        RenderableBlock block = RenderableBlock.getRenderableBlock(blockID);
        BlockImageIcon icon = block.getImageIconAt(FOREVER_SWITCH_LOCATION);
        
        // Update run status first!
        Boolean wasRunning = runBlockIsRunning.get(blockID);
        if ((wasRunning == null || !wasRunning) && on)
            myBlocksRunningCount++;
        else if (wasRunning && !on)
            myBlocksRunningCount--;
        
        runBlockIsRunning.put(blockID, on);
        if(isRunning){
            //toggle the run block associated with this run block
            WorkspaceController.getObserver().toggleRunBlock(blockID, false);
        	
            //get running icon from block and set to on icon 
            //onIcon should not be null as user must have turned on run block before needing to turn it on
            //unless we start saving "running" run blocks then its a problem
            icon.setIcon(onIcon);
            
            traverseStackAndHighlight(block, false);
            for (Long id : myCollisionBlocks)
                traverseStackAndHighlight(RenderableBlock.getRenderableBlock(id), false);
            
            //TODO would be nice to optimize this repaint since only
        }else{
            WorkspaceController.getObserver().toggleRunBlock(blockID, true);
            icon.setIcon(offIcon);
            traverseStackAndHighlight(block, true);
            
            // Don't highlight the collision blocks still in drawers.
            for (Long id : myCollisionBlocks) {
                WorkspaceWidget widget = 
                    RenderableBlock.getRenderableBlock(id).getParentWidget();
                if (widget instanceof Page) {
                    traverseStackAndHighlight(RenderableBlock.getRenderableBlock(id), true);
                }
            }
        }
    }
    
    private static void traverseStackAndHighlight(RenderableBlock block, boolean setRunning) {
        traverseStackAndHighlight(block, setRunning, new ArrayList<Long>());
    }
    
    /**
     * Uses a queue to visit each block. The traversal starts at the root block (the one
     * passed in) and goes until each block connected to it has been traversed. Upon
     * traversal, the block is put into a queue to be either highlighted or have its highlight
     * reset.
     * 
     * @param visited the procedure blocks that have already been traversed
     */
    private static void traverseStackAndHighlight(RenderableBlock block, 
        boolean setRunning, List<Long> visited)
    {
        Queue<RenderableBlock> blocks = new ConcurrentLinkedQueue<RenderableBlock>();
        blocks.add(block);
        while (!blocks.isEmpty()){
        	RenderableBlock curr_block = blocks.poll();
            Long id = curr_block.getBlockID();
        	if(setRunning && curr_block != null){
        		curr_block.setBlockHighlightColor(HIGHLIGHT);
        	}else{
                // If we can't unhighlight the top block, don't unhighlight 
                // anything else.
                if (DependencyManager.isInUse(id)) 
                    return;
                
                curr_block.resetHighlight();    
            }
            block.repaintBlock();
            //traverse afters
            Block b = Block.getBlock(id);
            if(!b.getAfterBlockID().equals(Block.NULL)){
                blocks.add(RenderableBlock.getRenderableBlock(b.getAfterBlockID()));
            }
            //traverse sockets
            for(BlockConnector socket : b.getSockets()){
                if(!socket.getBlockID().equals(Block.NULL)){
                	blocks.add(RenderableBlock.getRenderableBlock(socket.getBlockID()));
                }
            }
            //if this block is a procedure call block, branch to the procedure block's 
            //parent stack, however this may be prone to infinite loop if a procedure calls
            //itself, therefore pass in the procedure name
            if(SLBlockProperties.isProcedureCall(b) && !visited.contains(id)) {
                visited.add(id);
                Block parentBlock = SLBlockProperties.getParent(b);
                if(parentBlock != null) {
                	blocks.add(RenderableBlock.getRenderableBlock(parentBlock.getBlockID()));
                }
            }
        }        
    }

    //////////////////////////////////////////////////////
    // MOUSE METHODS LISTENING TO RUNBLOCK SWITCH ICONS //
    //////////////////////////////////////////////////////
    public void mouseClicked(MouseEvent e) {
        BlockImageIcon icon = (BlockImageIcon)e.getSource();
        RenderableBlock block = (RenderableBlock)icon.getParent();

        Thread compThread = new CompilerThread(block);
    	compThread.start();
        
        // Set focus to the canvas if user stops the run block using the switch icon
        if(!isBlockRunning(block.getBlockID())){
        	Workspace.getInstance().getFocusManager().setFocus(block.getBlock());
        }
    }

    /**
     * Utility thread used to make blocks feel more interactive while
     * compilation happens in the background.
     * @author Daniel
     *
     */
    private final class CompilerThread extends Thread {
    	private RenderableBlock block;
    	
    	/**
    	 * Creates a new CompilerThread that will compile and toggle the given
    	 * run block.
    	 * @param block the RenderableBlock to toggle and compile
    	 */
    	public CompilerThread(RenderableBlock block) {
    		this.block = block;
    	}
    	
    	/**
    	 * Toggles and compiles the block for this CompilerThread.
    	 * Changes the cursor to a wait cursor while it is compiling.
    	 * @override Thread.run()
    	 */
		public void run() {
        	Cursor oldc = block.getCursor();
        	block.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            SLBlockCompiler.getCompiler().compileAndToggleBlocks(block.getBlockID());        			
            block.setCursor(oldc);
		}    	
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
}
