package slcodeblocks;

/**
 * Values to pass between Application and RuntimeWorkspace
 */
public class ControlPanelVals {
	public int controlPanelRightWidth;
	public int controlPanelBottomHeight;
	
	/**
	 * 
	 * @param controlPanelRightWidth
	 * @param controlPanelBottomHeight
	 * @param splitPaneVertical
	 */
	public ControlPanelVals(int controlPanelRightWidth, int controlPanelBottomHeight) {
		this.controlPanelRightWidth = controlPanelRightWidth;
		this.controlPanelBottomHeight = controlPanelBottomHeight;
	}
}

