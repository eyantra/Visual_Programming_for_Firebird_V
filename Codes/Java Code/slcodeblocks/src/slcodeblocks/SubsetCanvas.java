package slcodeblocks;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import renderable.BlockUtilities;
import renderable.RenderableBlock;
import renderable.TextualFactoryBlock;
import codeblockutil.CColorChooser;
import codeblockutil.CGraphite;
import codeblockutil.CScrollPane;
import codeblockutil.CTextField;
import codeblockutil.CTracklessScrollPane;
import codeblockutil.Canvas;
import codeblockutil.DefaultCanvas;

/**
 * SubsetCanvas is a visible panel in the Edit Subsets dialog window that 
 * represents one Subset.
 */
public class SubsetCanvas extends DefaultCanvas implements DocumentListener, KeyListener, MouseListener, PropertyChangeListener{
	private static final long serialVersionUID = 328149080302L;
	private static final int MARGIN = 10;
	private final Color background = new Color(75,75,75);
	private final Font font= new Font("Ariel", Font.BOLD, 12);;
	private final CTextField editor;
	private final JList menu;
	private final JList list;
	private final CTextField nameField;
	private final CColorChooser colorField;
	private final ArrayList<RenderableBlock> blocks = new ArrayList<RenderableBlock>();
	
	/**
	 * Constructs a new SubsetCanvas which is the panel that represents an empty subset, 
	 * which means that it has no name, color, or blocks associated with the subset.
	 */
	public SubsetCanvas(){
		super();
		JLabel name = new JLabel("Name  ", SwingConstants.RIGHT);
		name.setFont(font);
		name.setForeground(Color.white);
		nameField = new CTextField();
		nameField.addDocumentListener(new DocumentListener(){
			public void changedUpdate(DocumentEvent e){
				SubsetCanvas.this.firePropertyChange(Canvas.LABEL_CHANGE, 0, 1);
			}
			public void insertUpdate(DocumentEvent e){
				SubsetCanvas.this.firePropertyChange(Canvas.LABEL_CHANGE, 0, 1);
			}
			public void removeUpdate(DocumentEvent e){
				SubsetCanvas.this.firePropertyChange(Canvas.LABEL_CHANGE, 0, 1);
			}
		});
		
		JLabel color = new JLabel("Color  ", SwingConstants.RIGHT);
		color.setFont(font);
		color.setForeground(Color.white);
		colorField = new CColorChooser(CGraphite.blue);
		colorField.addPropertyChangeListener(CColorChooser.COLOR_CHANGE, this);
		
		JLabel add = new JLabel("Add  ", SwingConstants.RIGHT);
		add.setFont(font);
		add.setForeground(Color.white);
		editor = new CTextField();
		editor.setFont(font);
		editor.setBackground(background);
		editor.addDocumentListener(this);
		editor.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e) {
				if(e.getKeyChar() == KeyEvent.VK_ENTER){
					menu.setSelectedIndex(0);
					addBlock();
					editor.setText("");
				}else if (e.getKeyCode() == KeyEvent.VK_DOWN){
					menu.setSelectedIndex(0);
					menu.requestFocus();
					menu.scrollRectToVisible(new Rectangle(0,0,0,0));
				}
			}
		});
		
		menu= new JList();
		menu.setFont(font);
		menu.setBackground(background);
		menu.setLayoutOrientation(JList.VERTICAL);
		CScrollPane menuPane = new CTracklessScrollPane(menu,
				7, CGraphite.blue, background){
			private static final long serialVersionUID = 328149080304L;
			public Insets getInsets(){
				return new Insets(MARGIN,MARGIN,MARGIN,MARGIN);
			}
			public void paint(Graphics g){
				int w = this.getWidth();
				int h = this.getHeight();
				g.setColor(background);
				g.fillRoundRect(0,0,w-1,h-1,MARGIN*2,MARGIN*2);
				g.setColor(Color.gray);
				g.drawRoundRect(0,0,w-1,h-1,MARGIN*2,MARGIN*2);
				
				super.paint(g);
			}
		};
		menuPane.setBackground(background);
		menuPane.setOpaque(false);
		menu.setCellRenderer(new QueryCellRenderer());
		menu.addMouseListener(this);
		menu.addKeyListener(this);
		
		list = new JList();
		list.setFont(font);
		list.addKeyListener(this);
		list.setBackground(Color.gray);
		
		//columsn, rows
		double[][] constraints = {{MARGIN,50,150,TableLayoutConstants.FILL,MARGIN},
								  {MARGIN,20,MARGIN,30,MARGIN,20,MARGIN,100,MARGIN,TableLayoutConstants.FILL,MARGIN}};
		this.setLayout(new TableLayout(constraints));
		this.setBackground(Color.darkGray);
		this.add(name, "1, 1");
		this.add(nameField, "2, 1");
		this.add(color, "1, 3");
		this.add(colorField, "2, 3");
		this.add(add, "1, 5");
		this.add(editor, "2, 5");
		this.add(menuPane, "2, 7");
		this.add(list, "1, 9, 2, 9");
		
		this.revalidate();
	}
	
	/**
	 * Gets the Color associated with this
	 */
	public Color getColor(){
		if(colorField.getColor() == null){
			return CGraphite.blue;
		}else{
			return colorField.getColor();
		}
	}
	
	/**
	 * Sets the color associated with this 
	 * @param subsetColor the desired Color to set
	 */
	public void setColor(Color subsetColor) {
		colorField.setColor(subsetColor);
	}
	
	/**
	 * Returns the name of this subset
	 * @return the name of this subset
	 */
	public String getName(){
		return nameField.getText();
	}
	
	/**
	 * Sets the name of this subset to the specified subsetName
	 * @param subsetName the desired String name of this
	 */
	public void setName(String subsetName) {
		nameField.setText(subsetName);
	}
	
	/**
	 * Returns an Iterable of all the blocks within this subset as RenderableBlock instances
	 * @return an Iterable of all the blocks within this subset as RenderableBlock instances
	 */
	public Iterable<RenderableBlock> getBlocks() {
		return blocks;
	}
	
	/**
	 * Adds the List of RenderableBlock instances to this subset
	 * @param blocks RenderableBlock instances to add to this subset
	 */
	public void addBlocks(Iterable<RenderableBlock> blocks) {
		for(RenderableBlock block : blocks) {
			this.blocks.add(block);
		}
		updateListData();
	}
	
	/**
	 * Updates the JList data list using the data in the blocks collection
	 */
	private void updateListData(){
		String[] listData = new String[blocks.size()];
		for(int i = 0; i < listData.length; i++) {
			listData[i] = BlockUtilities.disambiguousStringRep(blocks.get(i));
		}
		
		this.list.setListData(listData);
	}
	
	/**
	 * Adds a new block to this and updates the list data
	 */
	private void addBlock(){
		Object obj = menu.getSelectedValue();
		if (obj != null && obj instanceof TextualFactoryBlock){
			if(!blocks.contains(obj)) {
				//TODO when typeblocking is redesigned so that matches also include
				//renderable blocks, should not assume that objects in the menu
				//are TextualFactoryBlocks
				this.blocks.add(((TextualFactoryBlock)obj).getfactoryBlock());
			}
			updateListData();
		}
	}
	
	/**
	 * Removes a block from this and updates the list data
	 */
	private void removeBlock(){
		int index = list.getSelectedIndex();
		if(index >= 0 && index < blocks.size()){
			blocks.remove(index);
			updateListData();
		}
	}
	
	/**
	 * Updates the list of blocks that match the search query in the Edit Subsets Dialog
	 */
	private void updateMenu(){
		String text = editor.getText().trim();
		Collection<TextualFactoryBlock> matchingBlocks = BlockUtilities.getAllMatchingBlocks(text);
		menu.setModel(new DefaultComboBoxModel(matchingBlocks.toArray()));
		menu.revalidate();
		menu.repaint();
	}
	
    public void propertyChange(PropertyChangeEvent e){
    	if (e.getPropertyName().equals(CColorChooser.COLOR_CHANGE)){
    		this.firePropertyChange(Canvas.LABEL_CHANGE, 0, 1);
    	}
    }
	
	public void changedUpdate(DocumentEvent e){
		updateMenu();
	}
	public void insertUpdate(DocumentEvent e){
		updateMenu();
	}
	public void removeUpdate(DocumentEvent e){
		updateMenu();
	}
	public void keyPressed(KeyEvent e) {
		if (e.getSource().equals(menu)){
			if(e.getKeyChar() == KeyEvent.VK_ENTER){
				addBlock();
			}
		}else if(e.getSource().equals(list)){
			if(e.getKeyChar() == KeyEvent.VK_DELETE){
				removeBlock();
			}else if(e.getKeyChar() == KeyEvent.VK_BACK_SPACE){
				removeBlock();
			}
		}
	}
	public void keyTyped(KeyEvent e) {}
	public void keyReleased(KeyEvent e){}
	public void mouseClicked(MouseEvent e){
		if(e.getClickCount()==2){
			addBlock();
		}
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	
	/**
	 * CellRenderer of this.menu
	 */
	private class QueryCellRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 328149080305L;
		/**Color matching query red*/
		public void paint (Graphics g){
			//initialize string data
			String query = editor.getText().toLowerCase().trim();
			String item = this.getText().toLowerCase();
			FontMetrics metrics = g.getFontMetrics();
			
			//draw cell background
			if(this.getBackground()!=null){
				g.setColor(this.getBackground());
				g.fillRect(0, 0, this.getWidth(), this.getHeight());
			}
			
			//draw block's label
			g.setColor(Color.white);
			g.drawString(item, 2, this.getHeight()- metrics.getDescent());
			
			//hgihlight query red
			int index = item.indexOf(query);
			if(index!=-1){
				g.setColor(Color.red);
				g.drawString(
						query,
						(int)metrics.getStringBounds(item.substring(0, index), g).getWidth()+2,
						this.getHeight()- metrics.getDescent());
			}
		}
	}
}