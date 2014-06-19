package breedcontroller;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import codeblockutil.CGraphite;
import codeblockutil.Canvas;

/**
 * A CategoryTag wraps the set of available shapes under this category.
 * Technically it's internal data is IMMUTABLE, though it's GUI may chnage
 * based on user interactions
 */
class CategoryTag extends JList implements Canvas, MouseListener, ListSelectionListener{
	private static final long serialVersionUID = 328149080410L;
	/**
	 * Constructor
	 * @param name
	 * @param shapeTags
	 */
	CategoryTag(String name, Collection<ShapeTag> shapeTags){
		super(shapeTags.toArray());
        this.setName(name);
		this.setBackground(Color.black);
		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		this.setVisibleRowCount(-1);
		this.setCellRenderer(new ShapeListRenderer());
		//DO NOT USE listselectionlistener because if you
		//change two breed's shape to the same shape
		//consecutively, you get no action.
		this.addListSelectionListener(this);
		this.addMouseListener(this);
	}
	/**
	 * Update the selected Breed that it's shape may have changed
	 */
	public void valueChanged(ListSelectionEvent e){
		ShapeTag shapetag = (ShapeTag)this.getSelectedValue();
		BreedChangeEventManager.fireShapeChangedEvent(shapetag.getName());
	}
	/**
	 * Update the selected breed that it's shape may have changed
	 */
	public void mousePressed(MouseEvent e) {
		ShapeTag shapetag = (ShapeTag)this.getSelectedValue();
		BreedChangeEventManager.fireShapeChangedEvent(shapetag.getName());
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	/** Part of the Canvas Specification */
	public JComponent getJComponent(){
		return this;
	}
	/** Part of the Canvas Specification */
	public Color getColor(){
		return CGraphite.blue;
	}
	/** Part of the Canvas Specification */
	public int getButtonHeight(){
		return 25;
	}
	/** Part of the Canvas Specification */
	public Color getHighlight(){
		return Color.yellow;
	}
	/**
	 * Draw blue border around the image icon of the shapes when selected
	 */
	private static class ShapeListRenderer extends JLabel implements ListCellRenderer{
		private static final long serialVersionUID = 328149080411L;
		public ShapeListRenderer(){
	        setOpaque(true);
	        setHorizontalAlignment(SwingConstants.CENTER);
	        setVerticalAlignment(SwingConstants.CENTER);
	    }
	    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus){
	        if(isSelected){
	            setBackground(list.getSelectionBackground());
	            setForeground(list.getSelectionForeground());
	        }else{
	            setBackground(list.getBackground());
	            setForeground(list.getForeground());
	        }
	        ImageIcon icon = ((ShapeTag)value).getIcon();
	        setIcon(icon);
	        setPreferredSize(new Dimension(icon.getIconWidth() + 12, icon.getIconHeight() + 12));
	        return this;
	    }
	}
}