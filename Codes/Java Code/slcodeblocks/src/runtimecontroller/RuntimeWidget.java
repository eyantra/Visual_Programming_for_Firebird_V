package runtimecontroller;

import java.awt.Cursor;
import java.awt.PopupMenu;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import javax.swing.SwingUtilities;

import renderable.RenderableBlock;
import slcodeblocks.RuntimeWorkspace;
import slcodeblocks.SLBlockCompiler;
import slcodeblocks.WorkspaceController;
import workspace.ContextMenu;

import codeblocks.Block;

/**
 * RuntimeButtons reside within the RuntimeWorkspace and result  
 * from a block or series of blocks within the Workspace.  Pressing 
 * a runtime button or interacting with the widgets within a runtime button
 * can trigger a set of actions or modify a global variable.  Example of 
 * runtime buttons are:
 * - forever/run buttons: linked to forever/run block stacks in the workspace
 * - setup buttons: linked to setup block stacks in the workspace
 * - sliders: linked to a slider block and global variable decl connected in the workspace.  Slider buttons have 
 *   a slider widget to modify the value of its associated global variable.
 * - graphs: linked to graph blocks and their connected blocks in the workspace.  Graph buttons display
 *   a small image of real time data.  Double-clicking on the graph button produces an enlarged image of the graph.
 */
public class RuntimeWidget extends RenderableBlock{
	private static final long serialVersionUID = 328149080403L;
	/** Constant Cursor instances for wait and default cursors*/
	private static final Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
	private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
	
    /** A universal hashmap of all the RenderableBlock instances*/
    private static HashMap<Long, RuntimeWidget> ALL_RUNTIME_BUTTONS= new HashMap<Long,RuntimeWidget>();
    private static final String X_LOCATION = "x_loc_of_run_button";
    private static final String Y_LOCATION = "y_loc_of_run_button";

    /** the vertical orientation X location */
    private int verticalX;
    /** the vertical orientation Y location */
    private int verticalY;
    /** the horizontal orientation X location */
    private int horizontalX;
    /** the horizontal orientation Y location */
    private int horizontalY;
    
    /** the RuntimeWorkspace this RuntimeWidget belongs to */
    private RuntimeWorkspace runtimeWorkspace;
    
    //the blockID of its associated workspace block
    private Long workspaceBlockID;
  
    private boolean dragging = false;
    
    /**
     * Constructs a new RuntimeBlock instance with Long blockID of its associated runtime block and blockID of its associated 
     * workspace block
     * @param blockID Long Block id of associated with this
     */
    public RuntimeWidget(Long blockID, Long workspaceblockID, RuntimeWorkspace runtimWorkspace){    
        super(null, blockID);
        this.runtimeWorkspace = runtimWorkspace;
        setWorkspaceBlockID(workspaceblockID);
        try{
	        Block workspaceBlock = getWorkspaceBlock();
	        int xLoc = Integer.valueOf(workspaceBlock.getProperty(X_LOCATION));
	        int yLoc = Integer.valueOf(workspaceBlock.getProperty(Y_LOCATION));
	        if(xLoc > 0 && yLoc > 0){
	        	this.setLocation(xLoc, yLoc);
	        }
        }catch (NumberFormatException ex){
        }
    }
    
    
    /**
     * Clears all renderable block instances and all
     * block instances
     */
    public static void reset(){
        //System.out.println("reseting all renderable blocks");
        ALL_RUNTIME_BUTTONS.clear();
    } 
    
    /////////////////////////////////////
    // MOUSE LISTENER & MOTION METHODS //
    /////////////////////////////////////
    
    public void mouseClicked(MouseEvent e) {
        //TODO for some reason mouseclicked events are not being picked up...
    }
    
    public void mousePressed(MouseEvent e) {
    		getDragHandler().mousePressed(e);
    		//dragging = true;
    		//if (e.isShiftDown())
    		//	SLBlockCompiler.getCompiler().compileBlocks(wsblockID);
    }

    public void mouseReleased(MouseEvent e) {
        getDragHandler().mouseReleased(e);
        if(!dragging && !SwingUtilities.isRightMouseButton(e) && getDragHandler().mPressedX == e.getX() && getDragHandler().mPressedY == e.getY()){
            //technically this should be in mouseclicked but it was not being picked up
        	
        	/* create a separate thread to do the compiling so that users can still
        	 * click on other things (like the emergency stop button) while it's
        	 * working.  This makes highlights show up much earlier, but the wait
        	 * cursor lets people know if it's not done compiling yet. */
        	Thread compThread = new Thread(new Runnable() {
        		public void run() {
                	Cursor oldc = RuntimeWidget.this.getCursor();
                	// turning on a runtime widget may take a while so we need to put the wait cursor up
                	RuntimeWidget.this.setCursor(WAIT_CURSOR);
                    SLBlockCompiler.getCompiler().compileAndToggleBlocks(getWorkspaceBlockID());        			
                    RuntimeWidget.this.setCursor(oldc);
        		}
        	});
        	compThread.start();
        }
        dragging = false;
        
    	if(e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e) || e.isControlDown()){
    		//add context menu at right click location to provide functionality
    		//for adding new comments and removing comments
    		PopupMenu popup = ContextMenu.getContextMenuFor(this);
    		add(popup);
    		popup.show(this, e.getX(), e.getY());
    	}

    }

    public void mouseDragged(MouseEvent e) {
        getDragHandler().mouseDragged(e);
        dragging = true;
        Block workspaceBlock = getWorkspaceBlock();
        workspaceBlock.setProperty(X_LOCATION, Integer.toString(this.getX()));
        workspaceBlock.setProperty(Y_LOCATION, Integer.toString(this.getY()));
        
        if (runtimeWorkspace.isVerticalOrientation()) {
        	setVerticalX(this.getX());
        	setVerticalY(this.getY());
        } else {        
        	setHorizontalX(this.getX());
        	setHorizontalY(this.getY());
        }
        
        WorkspaceController.getObserver().markChanged();
    }

    public void mouseEntered(MouseEvent arg0) {
    	RenderableBlock workspaceRenderable = getWorkspaceRenderableBlock();
    	if (workspaceRenderable != null && workspaceRenderable.hasComment()){
    		this.setBlockToolTip(workspaceRenderable.getComment().getText());
    	}else{
    		this.setBlockToolTip(null);
    	}
    }
    public void mouseExited(MouseEvent arg0) {}
    public void mouseMoved(MouseEvent arg0) {}
    
    ////////////////
    //SAVING AND LOADING
    ////////////////
    
    /**
     * Returns the save string of this
     * @return the save string of this
     */
    public String getSaveString(){
    	StringBuffer sb = new StringBuffer();
    	sb.append("<RuntimeWidget id=\"" + getWorkspaceBlockID() + "\">");
    	sb.append("<VerticalLocation><Location><X>" + getVerticalX() + "</X><Y>" + getVerticalY() + "</Y></Location></VerticalLocation>");
    	sb.append("<HorizontalLocation><Location><X>" + getHorizontalX() + "</X><Y>" + getHorizontalY() + "</Y></Location></HorizontalLocation>");
    	if (this.hasComment()) sb.append(this.getComment().getSaveString());
    	sb.append("</RuntimeWidget>");
        
        return sb.toString();
    }
    
    /**
     * Take care of removing anything that should no longer exist after this widget is removed
     * such as an output frame,  should be called before removing this widget
     */
    public void disposeWidget() {
    	//pass,  nothing to dispose of by default
    }
    
    /**
     * returns human friendly printable representation of this widget for debug purposes
     */
    public String toString(){
        StringBuffer buf = new StringBuffer();
        buf.append("RuntimeButton "+getBlockID()+" workspaceID: " + getWorkspaceBlockID() + ": "+getBlock().getBlockLabel());
        return buf.toString();
    }


	/**
	 * The workspace Block ID corresponding to this runtimeWidget
	 * @return The workspace Block ID corresponding to this runtimeWidget
	 */
	public Long getWorkspaceBlockID() {
		return workspaceBlockID;
	}


	/**
	 * set workspace block ID that corresponds to this RuntimeWidget
	 * @param workspaceBlockID the workspaceBlockID to set
	 */
	void setWorkspaceBlockID(Long workspaceBlockID) {
		this.workspaceBlockID = workspaceBlockID;
	}
	
	/**
	 * Get the Workspace Block that corresponds to this RuntimeWidget
	 * @return Get the Workspace Block that corresponds to this RuntimeWidget
	 */
	Block getWorkspaceBlock() {
		return Block.getBlock(getWorkspaceBlockID());
	}
	
	/**
	 * Get the RenderableBlock from the workspace that corresponds to this RuntimeWidget
	 * @return Get the RenderableBlock from the workspace that corresponds to this RuntimeWidget
	 *
	 */
	RenderableBlock getWorkspaceRenderableBlock() {
		return RenderableBlock.getRenderableBlock(getWorkspaceBlockID());
	}


	/**
	 * The location of this block when in vertical or right mode
	 * @return the verticalX
	 */
	public int getVerticalX() {
		return verticalX;
	}


	/**
	 * The location of this block when in vertical or right mode
	 * @param verticalX the verticalX to set
	 */
	public void setVerticalX(int verticalX) {
		this.verticalX = verticalX;
	}


	/**
	 * The location of this block when in vertical or right mode
	 * @return the verticalY
	 */
	public int getVerticalY() {
		return verticalY;
	}


	/**
	 * The location of this block when in vertical or right mode
	 * @param verticalY the verticalY to set
	 */
	public void setVerticalY(int verticalY) {
		this.verticalY = verticalY;
	}


	/**
	 * The location of this block when in horizontal or bottom mode
	 * @return the horizontalX
	 */
	public int getHorizontalX() {
		return horizontalX;
	}


	/**
	 * The location of this block when in horizontal or bottom mode
	 * @param horizontalX the horizontalX to set
	 */
	public void setHorizontalX(int horizontalX) {
		this.horizontalX = horizontalX;
	}


	/**
	 * The location of this block when in horizontal or bottom mode
	 * @return the horizontalY
	 */
	public int getHorizontalY() {
		return horizontalY;
	}


	/**
	 * The location of this block when in horizontal or bottom mode
	 * @param horizontalY the horizontalY to set
	 */
	public void setHorizontalY(int horizontalY) {
		this.horizontalY = horizontalY;
	}
}
