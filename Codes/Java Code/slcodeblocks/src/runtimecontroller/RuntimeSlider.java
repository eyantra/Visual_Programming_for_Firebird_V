package runtimecontroller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import slcodeblocks.RuntimeWorkspace;
import slcodeblocks.SLBlockCompiler;
import slcodeblocks.SLBlockProperties;
import slcodeblocks.WorkspaceController;

import codeblocks.Block;
import codeblockutil.CSliderPane;

/**
 * RuntimeSliders reside within the RuntimeWorkspace and result  
 * from a global variable number block being connected to a
 * workspace slider.  The RuntimeSlider is linked to a slider block
 * and global variable decl connected in the workspace.  Slider buttons have 
 * a slider widget to modify the value of its associated global variable.
 * 
 * @specfield minium : float // the minimum value of this abstract bounded range model
 * @specfield maximum : float // the maximum value of this abstract bounded range model
 * @specfield value : float // the abstract value of this abstract bounded range model
 * 
 */
public class RuntimeSlider extends RuntimeWidget implements PropertyChangeListener{
	private static final long serialVersionUID = 328149080407L;
	/**
	 * A copy of the slider block widget of this RuntimeSlider
	 */
	private CSliderPane sliderWidget;
	private Long globalNumberBlockID;
	
	/**
	 * @param blockID - the block id of this RuntimeSlider
	 * @param workspaceblockID - the block id of the associated workspace block
	 * 
	 * @effects constructs new RuntimeSlider with a CSliderPane
	 * as the block widget of this.
	 */
	public RuntimeSlider(Long blockID, Long workspaceblockID, Long globalNumberBlockID, RuntimeWorkspace runtimeWorkspace) {
    	super (blockID, workspaceblockID, runtimeWorkspace);
		this.globalNumberBlockID = globalNumberBlockID;
		
		//notify global variable that the values have changes.
		float value;
		int index = SLBlockCompiler.getCompiler().getGlobalVariableIndex(Block.getBlock(globalNumberBlockID).getBlockLabel());
		if(index > -1){
			value = (float)WorkspaceController.getObserver().getGlobalVariable(index);
			if(value == 0){
		        SLBlockCompiler.getCompiler().compileBlockStack(getWorkspaceBlockID());
				value = this.getPropertyValue(SLBlockProperties.BOUNDING_VALUE, "0");
				String globalVarName = Block.getBlock(globalNumberBlockID).getBlockLabel();
				WorkspaceController.getObserver().setGlobalVariable(globalVarName, value);
			}
		}else{
	        SLBlockCompiler.getCompiler().compileBlockStack(getWorkspaceBlockID());
			value = this.getPropertyValue(SLBlockProperties.BOUNDING_VALUE, "0");
			String globalVarName = Block.getBlock(globalNumberBlockID).getBlockLabel();
			WorkspaceController.getObserver().setGlobalVariable(globalVarName, value);
		}
		float min = this.getPropertyValue(SLBlockProperties.BOUNDING_MIN, "0");
		float max = this.getPropertyValue(SLBlockProperties.BOUNDING_MAX, "10");
		
		//set up graphics
		sliderWidget = new CSliderPane(Math.min(min, value), Math.max(max, value), value);
		sliderWidget.setBounds(0,0,150,65);
		super.setBlockWidget(sliderWidget);
		super.repaintBlock();
		
		//set listeners
		sliderWidget.addMouseListener(this);
		sliderWidget.addMouseMotionListener(this);
		sliderWidget.addPropertyChangeListener(this);
	}
	
	private float getPropertyValue(String property, String defaultValue){
		//First, try to grab the property value from workspace slider.
		Block workspaceSlider = Block.getBlock(getWorkspaceBlockID());
		if(workspaceSlider.getProperty(property) == null){
			//If the property does not exist in the workspace slider,
			//try the global-variable-number instead.
			String newProperty = Block.getBlock(this.globalNumberBlockID).getProperty(property);
			//If the property does not exist in the global-variable-number
			//then just use the default property value, "defaultValue"
			newProperty = newProperty == null? defaultValue : newProperty;
			workspaceSlider.setProperty(property, newProperty);
		}
		try{
			return Float.parseFloat(workspaceSlider.getProperty(property));
		}catch (NumberFormatException ex){
			ex.printStackTrace();
			return 0;
		}
	}
	
	/**
	 * Update associated global variable VM value when a change to
	 * CSliderPane widget occurs
	 */
	public void propertyChange(PropertyChangeEvent e){
		if(e.getPropertyName().equals(CSliderPane.VALUE_CHANGED)){
			Block wsBlock = Block.getBlock(getWorkspaceBlockID());
			//only update value of global variable if they're still connected
			//after a variable gets disconnected from the slider block, the cslider will fire a change event,
			//however, the variable is no longer connected to it
			if (wsBlock != null){
				if(wsBlock.getSocketAt(0).hasBlock()) {
					String globalVarName = Block.getBlock(wsBlock.getSocketAt(0).getBlockID()).getBlockLabel();
					WorkspaceController.getObserver().setGlobalVariable(globalVarName, this.getValue());
					
					Block.getBlock(getWorkspaceBlockID()).setProperty(
							SLBlockProperties.BOUNDING_MIN, String.valueOf(sliderWidget.getMinimum()));
					Block.getBlock(getWorkspaceBlockID()).setProperty(
							SLBlockProperties.BOUNDING_MAX, String.valueOf(sliderWidget.getMaximum()));
					Block.getBlock(getWorkspaceBlockID()).setProperty(
							SLBlockProperties.BOUNDING_VALUE, String.valueOf(sliderWidget.getValue()));
				}
			}
		}
	}
	/**
	 * @return this.minimum
	 */
	public float getMinimum(){
		return sliderWidget.getMinimum();
	}
	/**
	 * @return this.maximum
	 */
	public float getMaximum(){
		return sliderWidget.getMaximum();
	}
	/**
	 * @return this.value
	 */
	public float getValue(){
		return sliderWidget.getValue();
	}
	/**
	 * @param min - new minimum value
	 * 
	 * @requires Integer.MIN_VALUE < max < Integer.MAX_VALUE
	 * @modifies this.sliderWidget
	 * @effects sets the maximum value of this RuntimeSlider block
	 * 			to "max"
	 */
	public void setMinimum(float min){
		sliderWidget.setMinimum(min);
		//save data in block
		Block.getBlock(getWorkspaceBlockID()).setProperty(
				SLBlockProperties.BOUNDING_MIN, String.valueOf(sliderWidget.getMinimum()));
		Block.getBlock(getWorkspaceBlockID()).setProperty(
				SLBlockProperties.BOUNDING_VALUE, String.valueOf(sliderWidget.getValue()));
	}
	/**
	 * @param max - new maximum value
	 * 
	 * @requires Integer.MIN_VALUE < value < Integer.MAX_VALUE
	 * @modifies this.sliderWidget
	 * @effects sets the minimum value of this RuntimeSlider block
	 * 			to "min"
	 */
	public void setMaximum(float max){
		sliderWidget.setMaximum(max);
		//save data in block
		Block.getBlock(getWorkspaceBlockID()).setProperty(
				SLBlockProperties.BOUNDING_MAX, String.valueOf(sliderWidget.getMaximum()));
		Block.getBlock(getWorkspaceBlockID()).setProperty(
				SLBlockProperties.BOUNDING_VALUE, String.valueOf(sliderWidget.getValue()));
	}
	/**
	 * @param value - new value
	 * 
	 * @requires Integer.MIN_VALUE < min < Integer.MAX_VALUE
	 * @modifies this.sliderWidget
	 * @effects sets the minimum value of this RuntimeSlider block
	 * 			to "min"
	 */
	public void setValue(float value){
		sliderWidget.setValue(value);
		//save data in block
		Block.getBlock(getWorkspaceBlockID()).setProperty(
				SLBlockProperties.BOUNDING_VALUE, String.valueOf(sliderWidget.getValue()));
	}
	
    public String toString(){
        return "RuntimeSlider "+getBlockID()+": "+getBlock().getBlockLabel();
    }
}
