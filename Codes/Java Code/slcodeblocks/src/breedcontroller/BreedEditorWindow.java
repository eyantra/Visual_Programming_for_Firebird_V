package breedcontroller;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import slcodeblocks.AvailableShapes;
import slcodeblocks.BreedManager;
import slcodeblocks.StarLogoShape;
import codeblockutil.CButton;
import codeblockutil.CGraphiteButton;
import codeblockutil.CWheeler;
import codeblockutil.Canvas;
import codeblockutil.Explorer;
import codeblockutil.TabbedExplorer;

/**
 * The GUI component that allow users to make edits to the breeds.
 * The changes are buffered and passed to teh breedManager.  It is the
 * responsibility of the BreedManger to actually make the changes.
 */
public class BreedEditorWindow implements BreedChangeListener, ActionListener{
    /** The default shape of any breed.  defaultShape is assigned if a
     * breed shape is not specified for a breed
     */
    private static final String defaultShape = "animals/turtle-default";
    /** Set of breed tags */
    private List<BreedTag> breedtags;
    /** JDialog that contains all the GUI component in this */
    private JDialog breedEditorDialog;
    /** The add button */
    private CButton addButton;
    /** The delete button */
    private CButton deleteButton;
    /** The ok button */
    private CButton okButton;
    /** The cancel button */
    private CButton cancelButton;
    /** The wheel pane */
    private CWheeler wheelPane;
    /** True if and only if breed editor is no longer active and OK was selcted */
    private boolean isActionCenceled = false;

    /**
     * Constructor
     * @param breedtags
     * @param frame
     */
	public BreedEditorWindow(List<BreedTag> breedtags, Frame frame){
        this.breedtags = breedtags;
		BreedChangeEventManager.addBreedChangeListener(this);
        breedEditorDialog = new JDialog(frame, "Create, Manage, and Delete Breeds", true);
        breedEditorDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        
        ////////////////////////////////////////
        //Breed Editing Pane: add breeed buuton,
        //delete button, and Breed List
        addButton = new CGraphiteButton("New");
        addButton.setPreferredSize(new Dimension(80,35));
        deleteButton = new CGraphiteButton("Delete");
        deleteButton.setPreferredSize(new Dimension(80,35));
        addButton.addActionListener(this);
        deleteButton.addActionListener(this); 
        JPanel buttonPane1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        buttonPane1.setBackground(Color.black);
        buttonPane1.add(addButton);
        buttonPane1.add(deleteButton);
        List<JComponent> breedWheelItems = new ArrayList<JComponent>();
        for(BreedTag tag : breedtags){
        	breedWheelItems.add(tag.getJComponent());
        }
        wheelPane = new CWheeler(breedWheelItems);
        wheelPane.setMaximumSize(new Dimension(1000,130));
        wheelPane.setPreferredSize(new Dimension(450,130));
        //there must be at least one breed tag ALWAYS
        if(!breedtags.isEmpty()){
        	BreedChangeEventManager.fireBreedSelectedEvent(breedtags.get(0));
        }
        
        ////////////////////////////////////////////////
        //Shape Explorer
        List<Canvas> categoryJComponents = new ArrayList<Canvas>();
        Iterator<String> categoryIt = AvailableShapes.categoryIterator();
        while (categoryIt.hasNext()) {
        	String category = categoryIt.next();
        	List<ShapeTag> shapetags = new ArrayList<ShapeTag>();
            Iterator<StarLogoShape> shapeIt = AvailableShapes.getShapeIterator(category);
            assert shapeIt.hasNext() : "Empty category";
            while (shapeIt.hasNext()) {
                StarLogoShape shape = shapeIt.next();
                shapetags.add(new ShapeTag(shape.fullName(), new ImageIcon(shape.icon)));
            }
            categoryJComponents.add(new CategoryTag(category, shapetags));
        }
        Explorer shapePane = new TabbedExplorer();
        shapePane.setDrawersCard(categoryJComponents);
        shapePane.getJComponent().setPreferredSize(new Dimension(550, 250));
        
        ////////////////////////////////////////////////////
        //OK-CANCEL Buttons
        okButton = new CGraphiteButton("OK");
        okButton.setPreferredSize(new Dimension(80,35));
        cancelButton = new CGraphiteButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(80,35));
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        JPanel buttonPane2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 3));
        buttonPane2.setBackground(Color.black);
        String lcOSName = System.getProperty("os.name").toLowerCase();
        boolean MAC_OS_X = lcOSName.startsWith("mac os x");
        if(MAC_OS_X){
        	buttonPane2.add(cancelButton);
        	buttonPane2.add(okButton);
        }else{
        	buttonPane2.add(okButton);
        	buttonPane2.add(cancelButton);
        }
        
        /////////////////////////////////////////////
        //Put all the Panels Together: buttonpane1,
        //breed list, shape explorer, buttonpane2
        JPanel mainPanel = new JPanel(){
        	private static final long serialVersionUID = 32814908040013L;
        	public Insets getInsets(){
        		return new Insets(10,10,10,10);
        	}
        };
        BoxLayout boxLayout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
        mainPanel.setBackground(Color.black);
        mainPanel.setLayout(boxLayout);
        mainPanel.add(buttonPane1);
        mainPanel.add(wheelPane);
        mainPanel.add(shapePane.getJComponent());
        mainPanel.add(buttonPane2);
        
        //initialize dialog box
        breedEditorDialog.add(mainPanel);
        breedEditorDialog.setBounds(50,50,450,450);
        breedEditorDialog.pack();
        breedEditorDialog.setVisible(true);
    }
	public void actionPerformed(ActionEvent e){
		if (e.getSource() == null){
			return;
		}else if (e.getSource().equals(addButton)){
			this.invokeAdd();
		}else if (e.getSource().equals(deleteButton)){
			this.invokeDelete();
		}else if (e.getSource().equals(okButton)){
			this.invokeOK();
		}else if (e.getSource().equals(cancelButton)){
			this.invokeCancel();
		}
	}
	/**
	 * exit breed editor BUT DO NOT apply changes
	 */
    private void invokeCancel(){
    	this.isActionCenceled = false;
        breedEditorDialog.setVisible(false);
        BreedChangeEventManager.clearBreedChangeListeners();
    }
    /**
     * Exit breed editor and apply changes
     */
    private void invokeOK(){
    	BreedTag tag = null;
    	for(BreedTag t : breedtags){
    		if(t.isSelected()){
    			tag=t;
    		}
    	}
    	
    	String newName = tag.getQueryField().getText();
        
        if(newName.equals("")){
            JOptionPane.showMessageDialog(new JFrame(),
                              "Please enter a name for seleted breed.",
                              "No breed name",
                              JOptionPane.WARNING_MESSAGE);
            tag.setName(tag.getName());
            return;
        }

        for(BreedTag t2 : breedtags){
        	if (!t2.equals(tag)){
        		if(t2.getName().equals(newName)){
        			JOptionPane.showMessageDialog(breedEditorDialog,
        					"Cannot rename breeds! \""+newName+"\" is not unique.  Please enter another name.");
        			tag.setName(tag.getName());
        			return;
                    
        		}
        	}
        }
        
        tag.setName(newName);
    	this.isActionCenceled = true;
    	BreedChangeEventManager.clearBreedChangeListeners();
        breedEditorDialog.setVisible(false);
    }
    /**
     * Add a new Breed tag
     */
    private void invokeAdd(){
        //generate new name;
    	String newName = getNewBreedName();
        //create new breed tag
    	BreedTag newTag = new BreedTag(newName, newName, defaultShape);
    	
    	//NOTE: WE DO NOT USE the code below because
    	//we want to add breeds to the ends ALWAYS
    	//
    	//insert new tag in correct index
/*    	for(int i = 0; i<breedtags.size() ; i++){
        	BreedTag tag = breedtags.get(i);
        	if(tag.isSelected()){
        		breedtags.add(i, newTag);
        		break;
        	}
        }*/
    	breedtags.add(newTag);
        
        //update wheel pane
        List<JComponent> breedWheelItems = new ArrayList<JComponent>();
        for(BreedTag tag : breedtags){
        	breedWheelItems.add(tag.getJComponent());
        }
        wheelPane.setElements(breedWheelItems);
        BreedChangeEventManager.fireBreedSelectedEvent(newTag);
        wheelPane.scrollToWheelItem(newTag.getJComponent());
    }
    /**
     * Delete a breed tag
     */
    private void invokeDelete(){
    	//make sure number breedtags has at least two elements
    	//so that when you delete one, one will still be left
    	if(breedtags.size() <2){
            JOptionPane.showMessageDialog(breedEditorDialog,
            		"You must have at least one breed.",
            		"No Breeds",
            		JOptionPane.WARNING_MESSAGE);
            return;
    	}
    	
    	//find selcted index
    	int index = 0;
    	for(int i = 0; i<breedtags.size() ; i++){
        	BreedTag tag = breedtags.get(i);
        	if(tag.isSelected()){
        		index=i;
        		break;
        	}
        }
    	
    	//remove selcted index
        int buttonClicked = JOptionPane.showConfirmDialog(breedEditorDialog,
                "Delete "+breedtags.get(index).getName()+" breed and all associated blocks?",
                "Delete Breed",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if(buttonClicked == JOptionPane.CANCEL_OPTION){
        	return;
        }
        breedtags.remove(index);
    	
        //update wheel pane
        List<JComponent> breedWheelItems = new ArrayList<JComponent>();
        for(BreedTag tag : breedtags){
        	breedWheelItems.add(tag.getJComponent());
        }
        wheelPane.setElements(breedWheelItems);
        
        //find adjacent tag and select it
        if(index == 0){
            BreedChangeEventManager.fireBreedSelectedEvent(breedtags.get(0));
        }else{
            BreedChangeEventManager.fireBreedSelectedEvent(breedtags.get(index-1));
        }
	}
    /**
     * Invoked whenever an action occurs ("pressing enter/return key") within the 
     * breedNameTextField or when it loses focus.  This method will update the 
     * current breed name at the selected index from current breeds panel 
     * with the text within the breedNameTextField.
     */
    public void changeBreedName(BreedTag tag){
        String newName = tag.getQueryField().getText();
        
        if(newName.equals("")){
        	JOptionPane.showMessageDialog(new JFrame(),
                              "Please enter a name for selected breed.",
                              "No breed name",
                              JOptionPane.WARNING_MESSAGE);
            tag.setName(tag.getName());
            return;
        }
        else if (newName.indexOf(BreedManager.NETWORK_BREED_CHAR) >= 0) {
            JOptionPane.showMessageDialog(null, 
                "Invalid character '" + BreedManager.NETWORK_BREED_CHAR + "' in selected breed name.",
                "Invalid Breed Name",
                JOptionPane.WARNING_MESSAGE);
            tag.setName(tag.getName());
            return;
        }

        for(BreedTag t2 : breedtags){
        	if (!t2.equals(tag)){
        		if(t2.getName().equals(newName)){
        			JOptionPane.showMessageDialog(breedEditorDialog,
        					"Breed names must be unique.  \""+newName+"\" is not unique.  Please enter another name.");
        			tag.setName(tag.getName());
        			return;
                    
        		}
        	}
        }
        
        tag.setName(newName);
        return;
    }
    /**
     * Change the shape of the selected Tag
     */
    public void changeBreedShape(String shape){
    	for(int i = 0; i<breedtags.size() ; i++){
        	BreedTag tag = breedtags.get(i);
        	if(tag.isSelected()){
        		tag.setShape(shape);
        		break;
        	}
        }
    }
    /**
     * Make all tags unselected and then make the new tag selected
     */
    public void changeBreedSelection(BreedTag tag){
    	for(BreedTag t : breedtags){
    		t.setSelected(false);
    	}
    	tag.setSelected(true);
    }
    /**
     * @return current collection of breed tags 
     */
    public List<BreedTag> getBreedTags(){
    	return this.breedtags;
    }
    /**
     * True if the breed editor is not longer active and "OK" was selected
     */
    public boolean isActionCenceled(){
    	return this.isActionCenceled;
    }
    /**
     * Returns a unique breed name using "New Breed" as a default name concating 
     * an appropriate number at the end.  
     * @return a unique breed name using "New Breed" as a default name concating 
     * an appropriate number at the end. 
     */
    private String getNewBreedName(){
    	List<String> names = new ArrayList<String>();
    	for (BreedTag tag : breedtags){
    		names.add(tag.getName());
    		names.add(tag.getID());
    	}
        String newBreedName = "New Breed";
        if(names.contains(newBreedName)){
        	for(int i = 2; ; i++){
                newBreedName = "New Breed " + Integer.toString(i);
                if(!names.contains(newBreedName)){
                	return newBreedName;
                }
        	}
        }else{
        	return newBreedName;
        }
    }
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        BreedEditorWindow panel = new BreedEditorWindow(new ArrayList<BreedTag>(), null);
    }
}
