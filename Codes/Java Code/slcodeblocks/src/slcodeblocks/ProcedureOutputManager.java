package slcodeblocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import renderable.RenderableBlock;

import codeblocks.Block;
import codeblocks.BlockConnector;
import codeblocks.BlockLink;
import codeblocks.BlockStub;

import workspace.Workspace;
import workspace.WorkspaceEvent;
import workspace.WorkspaceWidget;

/**
 * This class manages all procedure to output type mappings. This class
 * makes best-effort attempts to synchronize/update output block sockets.
 * This class also tries to update procedure callers when such things happen.
 * 
 * <p>Note that even with this class, we still need to implement separate 
 * procedure output checking in the compiler, since we make no guarantees 
 * about code branches or even output types (since we don't modify blocks
 * that are already connected to something). 
 */
public class ProcedureOutputManager
{   
	private static Workspace workspace;
	
    /** Default starting space for output blocks. */
    private final static int DEFAULT_SIZE = 5;
    
    /**
     * We use this class to encapsulate the procedure output information.
     * This includes the procedure block, all output blocks contained in it,
     * and whether it has a type.
     */
    private static class OutputInfo {
        private final List<Long> outputs = new ArrayList<Long>(DEFAULT_SIZE);
        private String type = null;
        
        /** The number of output blocks that are connected to something. */
        private int numTyped = 0;
    }
    
    /** The procedure block to output type mapping. */
    private final static Map<Long, OutputInfo> myProcInfo = 
        new HashMap<Long, OutputInfo>();
    
    /** 
     * Constructs a ProcedureOutputManager instance, which is required to deal with
     * adding and removing outputs from a procedure by keeping track of myProcInfo.
     * @param wworkspace: the Workspace instance to manage the Procedure Outputs.
     * 	(needs to be static because we call it in some static methods)
     */
    public ProcedureOutputManager(Workspace wworkspace) {
    	workspace = wworkspace;
    }
    
    // Event Management =========================
    // MUST BE CALLED AFTER POLYRULE
    /**
     * Similar to a WorkspaceListener, but not an actual listener because it is
     * never added to the list of listeners and can only be called after PolyRule.
     * This is because output is a poly block that needs to be handled before
     * attaching an output to a procedure can occur (if the output type is not
     * dealt with first, we can't use it to determine the procedure type).
     * 
     * @param event: WorkspaceEvent holds the information about the block that is
     * being dealt with (BlockID, Link, Widget, etc.)
     * TODO: Maybe we can abstract this even more from listener by only passing info
     * necessary for the update instead of a whole event, but the issue is that
     * I think the event is necessary to call the other listeners in the case that
     * outputs don't match and we have to disconnect blocks...
     */
    public static void procedureUpdateInfo(WorkspaceEvent event) {
        Block b = getBlock(event.getSourceBlockID());
        BlockLink link = event.getSourceLink();
        
        switch (event.getEventType()) {
        case WorkspaceEvent.BLOCKS_CONNECTED:
            if (link != null) {
                blocksConnected(event.getSourceWidget(),
                                link.getSocketBlockID(), 
                                link.getPlugBlockID());
            }
            return;
            
        case WorkspaceEvent.BLOCKS_DISCONNECTED:
            if (link != null) {
                blocksDisconnected(event.getSourceWidget(),
                                   link.getSocketBlockID(), 
                                   link.getPlugBlockID());
            }
            return;
            
        case WorkspaceEvent.BLOCK_ADDED:
            if (b != null) {
                if (b.isProcedureDeclBlock()) {
                    // Create a new entry for this proc 
                    myProcInfo.put(b.getBlockID(), new OutputInfo());
                }
            }
            return;
            
        case WorkspaceEvent.BLOCK_REMOVED:
            if (b != null && b.isProcedureDeclBlock()) {
            	// System.out.println("procedure of type "+myProcInfo.get(b.getBlockID()).type+" removed.");
                // Remove our entry.
                myProcInfo.remove(b.getBlockID());
                if (link != null) {
                	blocksDisconnected(event.getSourceWidget(),
                			link.getSocketBlockID(),
                			link.getPlugBlockID());
                }
            }
            return;
        }
    }
    
    /**
     * After loading canvas, does a final sweep of all procedures to update
     * myProcInfo. It does this by going through each output block and seeing
     * if that output contributes any information to a procedure.
     * Then it updates the procedure's stubs if the procedure type changes.
     * 
     * Called after regular loading is finished (WorkspaceController.LoadSaveString()).
     * 
     * Updating myProcInfo depends on one crucial assumption:
     * The types of all outputs should be consistent because they were handled
     * prior to the original saving with the POM (so the loaded version is correct).
	 * Therefore, we DO NOT need to:
	 * 		change the types of empty output sockets
	 *		revert the types of incorrect output sockets
	 *		disconnect incorrect output types
     */
    public static void finishLoad() {
    	
    	// Create new output info for each procedure on the canvas.
    	for(Block p : workspace.getBlocksFromGenus("procedure")) {
    		myProcInfo.put(p.getBlockID(), new OutputInfo());
    	}
    	
    	// Update myProcInfo by checking each output.
    	for(Block b : workspace.getBlocksFromGenus("output")) {
    		// Only handle the output blocks that are connected to a procedure.
    		Long top = SLBlockProperties.getTopBlockID(b.getBlockID());
    		if (top != null && Block.getBlock(top).isProcedureDeclBlock()) {
    			// System.out.println("procedure (top) number "+top);
    			OutputInfo info = myProcInfo.get(top);
    			
    			// Check that the output hasn't already been visited (shouldn't have but just to be safe...)
				if (!info.outputs.contains(b)) {
					
					// Change the procedure type if it has not been set and is not of the generic type poly.
    				if (info.type == null && !b.getSocketAt(0).getKind().equals("poly")) {
    					info.type = b.getSocketAt(0).getKind();
    					// Update stubs due to change in type
    					BlockStub.parentPlugChanged(top, info.type);
    				}
    				
    				// Increase the number of typed outputs (aka connected outputs without empty sockets).
    				if (b.getSocketAt(0).getBlockID() != -1) {
    					info.numTyped++;
    				}
    				
    				// Add all outputs that have not already been added (regardless of type or empty/nonempty sockets).
   					info.outputs.add(b.getBlockID());
   					
    			}
    			// System.out.println("numtyped "+info.numTyped+" type "+info.type);
    		}
    	}
    }
    
    /**
     * Clears the information in myProcInfo. This should only be called
     * when resetting the workspace (WorkspaceController.resetWorkspace()), 
     * which occurs when loading a project or creating a new one.
     */
    public static void reset() {
    	myProcInfo.clear();
    }
    
    /**
     * When blocks are connected, check to see whether we are in a proc
     * stack. If so, check to see whether we should update the output type,
     * and if any existing blocks are affected by this change. 
     */
    private static void blocksConnected(WorkspaceWidget w, Long socket, Long plug) {
        // Don't do anything if we're not in a procedure stack.
        Long top = SLBlockProperties.getTopBlockID(socket);

        if (top == null || !Block.getBlock(top).isProcedureDeclBlock()) return;

        // If the proc stack already has a type, change all outputs in the
        // new part to that type and disconnect anything that doesn't fit.
        OutputInfo info = myProcInfo.get(top);
        List<WorkspaceEvent> events = new ArrayList<WorkspaceEvent>();
        Block b = Block.getBlock(socket);

        // If the block was added to an output block,
        // then b is the parent block (socket = output) and add is false.
        // else b is the current block (plug) and add is true.
        // in changeType, we check if b is type output before proceeding.
        boolean add = true;
        if (isOutput(b))
            add = false;    // don't add the output twice to the list
        else 
            b = Block.getBlock(plug);
        
        if (info.type != null)
            changeType(add, b, info.type, info, w, events);
        else {
            // Examine the type. If there is a type in the new portion of
            // the stack, reset all the previous output blocks to that type.
            examineType(add, b, info);
            if (info.type != null) {
                changeType(info, w, events);
                BlockStub.parentPlugChanged(top, info.type);
            }
        }
        
        // Fire events.
        if (!events.isEmpty())
            WorkspaceController.getInstance().notifyListeners(events);
    }
    
    /**
     * When blocks are disconnected, update the proc stack and revert any
     * affected output blocks/callers.
     */
    private static void blocksDisconnected(WorkspaceWidget w, Long socket, Long plug) {
        // Don't do anything if we're not in a procedure stack.
        Long top = SLBlockProperties.getTopBlockID(socket);
        if (top == null || !Block.getBlock(top).isProcedureDeclBlock()) return;
        
        // Revert any output blocks in the disconnected stack. 
        OutputInfo info = myProcInfo.get(top);

        if (isOutput(Block.getBlock(socket)) && info.type != null) {
            // PolyRule reverts this to "poly" so we have to change it 
            // back to whatever type it should be.

            info.numTyped--;
            Block.getBlock(socket).getSocketAt(0).setKind(info.type);
            Block.getBlock(socket).notifyRenderable();
        }
        else
            revertType(Block.getBlock(plug), info, true);        
        
        // If there are no more connected blocks in this procedure, remove
        // the type and revert current output blocks.
        if (info.numTyped == 0) {
            info.type = null;
            revertType(Block.getBlock(top), info, false);
            BlockStub.parentPlugChanged(top, null);
        }
    }
    
    // Output Block Types ==============================
    
    /**
     * Traverse the new portion of the stack, looking for an output type. This
     * assigns the procedure the FIRST type it encounters, regardless of which
     * is more prevalent in the stack. Only call this method if the proc does
     * not yet have an output type. 
     * 
     * @param info Filled with new output block ids 
     */
    private static void examineType(boolean add, Block b, OutputInfo info) {
        if (isOutput(b)) {
            if (add) {
                info.outputs.add(b.getBlockID());
            }
            
            BlockConnector socket = b.getSocketAt(0);
            Block b2 = getBlock(socket.getBlockID());
            if (b2 != null) {
                // Found a type - set it and keep looking for blocks.
                info.numTyped++;
                if (info.type == null) 
                    info.type = socket.getKind();
            }
            else if (!socket.getKind().equals(socket.initKind())) {
                // Reset its type, regardless of what it was before.
                socket.setKind(socket.initKind());
                b.notifyRenderable();
            }
            
            // There are no blocks after outputs.
            return;
        }
        
        // Traverse sockets first.
        for (BlockConnector conn : b.getSockets()) {
            Block b2 = getBlock(conn.getBlockID());
            if (b2 != null)
                examineType(true, b2, info);
        }
        
        // Traverse after block.
        Block b2 = getBlock(b.getAfterBlockID());
        if (b2 != null)
            examineType(true, b2, info);
    }
    
    /**
     * Changes the output block types in the given list, disconnecting
     * blocks if necessary.
     */
    private static void changeType(OutputInfo info, 
                                   WorkspaceWidget w, List<WorkspaceEvent> e) 
    {
        String type = info.type;
        for (Long id : info.outputs) {
            Block b = Block.getBlock(id);
            
            // If there is nothing connected to it, we just change the socket
            // type.
            BlockConnector socket = b.getSocketAt(0);
            Block b2 = getBlock(socket.getBlockID());
            if (b2 == null && !socket.getKind().equals(type)) {
                socket.setKind(type);
                b.notifyRenderable();
            }
            
            // Otherwise, we might have to disconnect what's already there.
            else if (!socket.getKind().endsWith(type)) {
                BlockLink link = BlockLink.getBlockLink(b, b2, socket, b2.getPlug());
                link.disconnect();
                RenderableBlock.getRenderableBlock(id).blockDisconnected(socket);
                e.add(new WorkspaceEvent(w, link, WorkspaceEvent.BLOCKS_DISCONNECTED));
            }
        }
    }

    /** 
     * Traverse the stack, changing any output types to the given type. 
     * Disconnect blocks if necessary.
     * 
     * @param info Filled with output block ids. 
     * @param events Filled with workspace events to be fired
     */
    private static void changeType(boolean add, 
                                   Block b, String type, OutputInfo info,
                                   WorkspaceWidget w, List<WorkspaceEvent> e) 
    {
        if (isOutput(b)) {
            if (add) {
                info.outputs.add(b.getBlockID());
            }
            
            // If there is nothing connected to it, we just change the socket
            // type.
            BlockConnector socket = b.getSocketAt(0);
            Block b2 = getBlock(socket.getBlockID());
            if (b2 == null && !socket.getKind().equals(type)) {
                socket.setKind(type);
                b.notifyRenderable();
            }
            
            // Otherwise, we might have to disconnect what's already there.
            else if (!socket.getKind().endsWith(type)) {
                // We increase the connections, even if it's the wrong type,
                // because the disconnected event will correct this.
                info.numTyped++;
                
                BlockLink link = BlockLink.getBlockLink(b, b2, socket, b2.getPlug());
                link.disconnect();
                RenderableBlock.getRenderableBlock(b.getBlockID()).blockDisconnected(socket);
                e.add(new WorkspaceEvent(w, link, WorkspaceEvent.BLOCKS_DISCONNECTED));
            }
            
            // Otherwise, we have a connected output with the right type.
            else {
                info.numTyped++;
            }
        
            // There is nothing after an output block.
            return;
        }
        
        // Traverse sockets first.
        for (BlockConnector conn : b.getSockets()) {
            Block b2 = getBlock(conn.getBlockID());
            if (b2 != null)
                changeType(true, b2, type, info, w, e);
        }
        
        // Traverse after block.
        Block b2 = getBlock(b.getAfterBlockID());
        if (b2 != null)
            changeType(true, b2, type, info, w, e);
    }
    
    /** 
     * Revert the output blocks to their original socket types, starting from
     * the given block. Updates the proc info.
     */
    private static void revertType(Block b, OutputInfo info, boolean remove) {
        if (isOutput(b)) {
            if (remove) {
                info.outputs.remove(b.getBlockID());
            }
            
            // If there is nothing connected to it, we just change the socket
            // type.
            BlockConnector socket = b.getSocketAt(0);
            Block b2 = getBlock(socket.getBlockID());
            if (b2 == null && !socket.getKind().equals(socket.initKind())) {
                socket.setKind(socket.initKind());
                b.notifyRenderable();
            }
            
            // Otherwise, decrement our connected counter.
            else if (b2 != null && remove) {
                info.numTyped--;
            }
        
            // There is nothing after an output block.
            return;
        }
        
        // Traverse sockets first.
        for (BlockConnector conn : b.getSockets()) {
            Block b2 = getBlock(conn.getBlockID());
            if (b2 != null)
                revertType(b2, info, remove);
        }
        
        // Traverse after block.
        Block b2 = getBlock(b.getAfterBlockID());
        if (b2 != null)
            revertType(b2, info, remove);
    }
    
    /** Returns the block if one exists, or null if not. */
    private static Block getBlock(Long id) {
        if (id == null || id.equals(Block.NULL)) 
            return null;
        else return Block.getBlock(id);
    }
    
    // Helper Methods =========================
    private static boolean isOutput(Block b) {
        return SLBlockProperties.isCmd(SLCommand.CMD_OUTPUT, b);
    }
}
