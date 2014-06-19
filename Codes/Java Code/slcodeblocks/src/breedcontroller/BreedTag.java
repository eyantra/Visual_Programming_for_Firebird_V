package breedcontroller;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

import slcodeblocks.AvailableShapes;
import slcodeblocks.StarLogoShape;
import codeblockutil.CGraphite;
import codeblockutil.CWheelItem;

/**
 * A BreedTag is a wrapper class that synchronizes information about
 * a particular breed being edited.
 * 
 * ***************How Breeds Tags Are Passed******************
 * When the user presses the "Edit Breed"
 * button, the BreedManager first creates a set of BreedTags from it's BreedToShapeMapping.
 * THe set of breedtags are passed to teh BreedEditor that uses teh set to
 * construct a GUI components to visual display each breed (and their attributes).
 * As the user interacts with the GUI, the system updates the backend
 * data stored in each BreedTag.  What all is complete, the BreedEditor
 * then passes the Set of BreedTags (which may be much different from the original set).
 * back to the BreedManager.  The BreedManager then compares the set of breed tags with
 * its own BreedToShapeMapping adn responds accordingly.
 * 
 * *****************What Are Breed Tags***********************
 * A BreedTag is a wrapper class that synconizes information about
 * a particular Breed.  At the heart of every Breedtag is an ID.
 * Two BreedTags are teh same Tag if their IDs are the same.  As a result,
 * we may never have two BreedTagsa with the same ID but different data.
 * 
 * Next to teh ID, a BreedTag also has holds a set of attributes including:
 * name, shape, shape icon, text field to edit the name, the label to visually
 * display the shape.  In addition it also wraps a StarLogoShape that it uses
 * to access even more information about this particular breed's shape.
 *
 */
public class BreedTag implements ActionListener, FocusListener, MouseListener{
	/** Unique ID.  If two Breeds have the same ID, then they are equal */
	private final String ID;
	/** Name of this */
	private String name;
	/** Displays the name of this */
	private JTextField field;
	/** Displays the icon of this breed */
	private JLabel label;
	/** The GUI representation of this */
	private CWheelItem item;
	/** Holds information about this breed's shape */
	private StarLogoShape shape;
	/** True if this breed is selected as active. */
	private boolean selected;
	/**
	 * COnstructor
	 * @param ID
	 * @param name
	 * @param shape
	 * 
	 * @requires ID, name, shape != null
	 */
	public BreedTag(String ID, String name, String shape){
		if(ID == null || ID.length()==0) throw new RuntimeException("nameID may not be null");
		this.ID= ID;
		this.name=name;
		this.selected = false;
		this.field = new JTextField(ID);
		this.field.setFont(new Font("Ariel", Font.BOLD, 13));
		this.field.setBorder(BorderFactory.createMatteBorder(3,3,3,3, Color.blue));
		this.field.setBackground(Color.white);
		this.field.addActionListener(this);
		this.field.addFocusListener(this);
		this.shape=AvailableShapes.getShape(shape);
		ImageIcon icon = new FocusableIcon(this.shape.icon);
		this.label = new JLabel(icon, SwingConstants.CENTER);
		this.label.setBackground(new Color(50,50,50));
		this.label.addMouseListener(this);
		this.item = new CWheelItem();
		item.setBackground(new Color(50,50,50));
		this.item.add(label, BorderLayout.CENTER);
		this.item.add(field, BorderLayout.SOUTH);
	}
	/** Rename Breed when the user presses enter on a text field */
    public void actionPerformed(ActionEvent arg0) {
    	//BreedChangeEventManager.fireBreedRenamedEvent(this);
    	this.item.requestFocus();
    }
    /** Selects this breed */
    public void focusGained(FocusEvent arg0) {
    	BreedChangeEventManager.fireBreedSelectedEvent(this);
    }
    /** Rename Breed when the user moves away from a text field */
    public void focusLost(FocusEvent arg0) {
    	BreedChangeEventManager.fireBreedRenamedEvent(this);
    }
    /** Selects this breed */
	public void mousePressed(MouseEvent e) {
		BreedChangeEventManager.fireBreedSelectedEvent(this);
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	/**
	 * Sets the name of this
	 * @param name
	 */
	void setName(String name){
		this.field.setText(name.trim());
		this.name=name;
	}
	/**
	 * @return name of this
	 */
	public String getName(){
		return this.name;
	}
	/**
	 * @return the text field that is used to edit name
	 */
	JTextComponent getQueryField(){
		return this.field;
	}
	/**
	 * sets this breed's shape
	 * @param shape
	 */
	void setShape(String shape){
		this.shape=AvailableShapes.getShape(shape);
		ImageIcon icon = new FocusableIcon(this.shape.icon);
		this.label.setIcon(icon);
	}
	/**
	 * @return shape of this
	 */
	public String getShape(){
		return this.shape.fullName();
	}
	/**
	 * @return the image icon that represents this breed's shape
	 */
	public Image getImage(){
		return this.shape.icon;
	}
	/**
	 * @return the uinque immutable ID of this
	 */
	public String getID(){
		return this.ID;
	}
	/**
	 * @return the WHeelItem representation of this
	 */
	JComponent getJComponent(){
		return this.item;
	}
	/**
	 * Vidual update this breedtags's GUI to show that it is being selected
	 * @param selected
	 */
	void setSelected(boolean selected){
		if(selected){
			this.selected=true;
			this.label.repaint();
			this.field.setBorder(BorderFactory.createMatteBorder(3,3,3,3, CGraphite.blue));
			this.field.setBackground(Color.white);
			this.field.requestFocus();
			//this.field.setEditable(true);
		}else{
			this.selected=false;
			this.label.repaint();
			this.field.setBorder(BorderFactory.createMatteBorder(3,3,3,3, new Color(50,50,50)));
			this.field.setBackground(Color.gray);
			//this.field.setEditable(false);
		}
	}
	/**
	 * @return true if this breed is being edited
	 */
	boolean isSelected(){
		return this.selected;
	}
	/**
	 * The image of this breed that lights up or dims out when selected
	 */
	private class FocusableIcon extends ImageIcon{
		private static final long serialVersionUID = 328149080412L;
		private FocusableIcon(BufferedImage image){
			super(image);
		}
		public void paintIcon(Component c, Graphics g, int arg0, int arg1){
			if(selected){
				super.paintIcon(c, g, arg0, arg1);
			}else{
				Graphics2D g2 = (Graphics2D) g;
				AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.2f);
				g2.setComposite(alpha);
				super.paintIcon(c, g2, arg0, arg1);
			}
		}
	};
}