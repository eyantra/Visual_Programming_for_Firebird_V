package breedcontroller;

import javax.swing.ImageIcon;

/**
 * Wraps a shape's image icon and name.  It is IMMUTABLE
 * @author An Ho
 */
class ShapeTag{
	/**  */
	private String name;
	/**  */
	private ImageIcon icon;
	/**
	 * Constructor 
	 * @param name
	 * @param icon
	 */
	ShapeTag(String name, ImageIcon icon){
		this.name=name;
		this.icon=icon;
	}
	/**
	 * @return this shape's icon
	 */
	ImageIcon getIcon(){
		return this.icon;
	}
	/**
	 * @return this shapes's name
	 */
	String getName(){
		return this.name;
	}
}
