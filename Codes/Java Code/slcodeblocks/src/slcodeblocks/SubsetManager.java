package slcodeblocks;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import renderable.BlockUtilities;
import renderable.FactoryRenderableBlock;
import renderable.RenderableBlock;
import renderable.TextualFactoryBlock;
import workspace.Subset;
import workspace.Workspace;
import codeblocks.Block;
import codeblocks.BlockGenus;
import codeblocks.BlockStub;
import codeblockutil.CButton;
import codeblockutil.CGraphiteButton;
import codeblockutil.TabbedExplorer;
import codeblockutil.XMLStringWriter;

/**
 * SubsetManager manages both the Edit Subsets dialog window and the creation and deletion of 
 * subsets for a project.
 */
public class SubsetManager extends JDialog implements ComponentListener, ActionListener{
	private static final long serialVersionUID = 328149080306L;
	private static final int MARGIN = 10;
	private static TabbedExplorer explorer;
	private static CButton cancel;
	private static CButton ok;
	private static CButton addSubset ;
	private static CButton deleteSubset;
	private static JCheckBox includeFactory;
	private static JCheckBox includeSubsets;
	
	private static List<SubsetCanvas> subsets;
	
	/**
	 * Creates a new Edit Subsets dialog using the specified frame as the parent
	 * frame.  The subsets dialog will include existing subsets in the project.
	 * @param frame
	 */
	public SubsetManager(Frame frame){
		super(frame, "Edit Block Subsets", true);
		//set up buttons
		addSubset = new CGraphiteButton("Add Subset");
		addSubset.addActionListener(this);
		deleteSubset = new CGraphiteButton("Delete Subset");
		deleteSubset.addActionListener(this);
        cancel = new CGraphiteButton("Cancel");
        cancel.addActionListener(this);
        ok = new CGraphiteButton("OK");
        ok.addActionListener(this);
        includeFactory = new JCheckBox();
        includeFactory.setSelected(true);
        includeFactory.setText("Include Factory and MyBlock");
        includeFactory.setForeground(Color.white);
        includeFactory.setOpaque(false);
    	includeSubsets = new JCheckBox();
    	includeSubsets.setText("Include Subsets");
    	includeSubsets.setForeground(Color.white);
    	includeSubsets.setOpaque(false);
    	includeSubsets.setSelected(true);
    	
        explorer = new TabbedExplorer();
        subsets = new ArrayList<SubsetCanvas>();
        //if subsets already exist, add to dialog window
        //otherwise add one empty subset canvas
        Collection<Subset> existingSubsets = Workspace.getInstance().getFactoryManager().getSubsets();
		if(existingSubsets.size() > 0) {
			for(Subset subset: existingSubsets) {
				addSubsetCanvasFromSubset(subset);
	        }
		} else {
			subsets.add(new SubsetCanvas());
		}
		explorer.setDrawersCard(subsets);
        //columsn, rows
        double[][] constraints = {{MARGIN,100,100,TableLayoutConstants.FILL,100,100,MARGIN},
				  {MARGIN,35,35,TableLayoutConstants.FILL,35,MARGIN}};
        this.setLayout(new TableLayout(constraints));
        this.setBackground(Color.black);
        this.add(addSubset, "1, 1");
        this.add(deleteSubset, "2, 1");
        this.add(includeFactory, "4, 1, 5, 1");
        this.add(includeSubsets, "4, 2, 5, 2");
        this.add(explorer.getJComponent(), "1, 3, 5, 3");
        this.add(cancel, "4, 4");
        this.add(ok, "5, 4");
        
        this.getContentPane().setBackground(Color.black);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setBounds(50,50,425,500);
		this.setResizable(true);
		this.validate();
		this.repaint();
		this.setVisible(true);
		explorer.getJComponent().addComponentListener(this);
		explorer.reformView();
		
	}
    public void actionPerformed(ActionEvent e) {
    	if (e.getSource().equals(ok)){
    		closeEditor(true);
    	}else if (e.getSource().equals(cancel)){
    		closeEditor(false);
    	}else if (e.getSource().equals(addSubset)){
    		this.addSubsetCanvas();
    	}else if (e.getSource().equals(deleteSubset)){
    		this.deleteSubset();
    	}
    }
    
    /**
     * Closes the Edit Subsets dialog window iff valid is true
     * @param valid boolean flag that designates if the dialog window should close
     */
	private void closeEditor(boolean valid){
		this.dispose();
		if(valid){
			Collection<Subset> blocksubsets = new ArrayList<Subset>();
			for(SubsetCanvas tag : subsets){
				List<RenderableBlock> subset = new ArrayList<RenderableBlock>();
				for(RenderableBlock block : tag.getBlocks()){
					String genus = Block.getBlock(block.getBlockID()).getGenusName();
	    			assert BlockGenus.getGenusWithName(genus) != null : "Unknown BlockGenus: "+genus;
					//don't need to create new block instances for factory block - just use current block instance 
					//underlying the related factory block
					FactoryRenderableBlock frb = new FactoryRenderableBlock(null, block.getBlockID());
					subset.add(frb);
				}
				blocksubsets.add(new Subset(tag.getName(), tag.getColor(), subset));
			}
			Workspace.getInstance().setupSubsets(blocksubsets, includeFactory.isSelected(), includeSubsets.isSelected());
		}
	}
	
	/**
	 * Adds an empty SubsetCanvas instance to the Edit Subsets dialog
	 */
	public void addSubsetCanvas(){
    	SubsetCanvas newSubset = new SubsetCanvas();
    	subsets.add(newSubset);
		explorer.setDrawersCard(subsets);
		explorer.reformView();
		this.validate();
	}
	
	/**
	 * Adds a SubsetCanvas instance to the Edit Subsets Dialog using the information
	 * stored in the specified subset
	 * @param subset Subset to add to the Edit Subsets Dialog
	 */
	public static void addSubsetCanvasFromSubset(Subset subset) {
		SubsetCanvas newSubset = new SubsetCanvas();
		newSubset.setName(subset.getName());
		newSubset.setColor(subset.getColor());
		newSubset.addBlocks(subset.getBlocks());
    	subsets.add(newSubset);
		explorer.setDrawersCard(subsets);
		explorer.reformView();
		//validate();
	}
	
	/**
	 * Deletes the subset whose subset canvas currently has focus in the Edit Subsets dialog.
	 */
	public void deleteSubset(){
    	subsets.remove(subsets.get(explorer.getSelectedIndex()));
		explorer.setDrawersCard(subsets);
		explorer.reformView();
		this.validate();
	}
    public void componentResized(ComponentEvent e){
    	explorer.reformView();
    }
	public void componentHidden(ComponentEvent e){}
	public void componentMoved(ComponentEvent e){}
    public void componentShown(ComponentEvent e){}
       
    /**
     * Returns true iff the specified block genus belongs in the set of genuses that every 
     * breed has
     * @param block Block instance to test
     */
    private static boolean isDefaultBreedBlockGenus(String genusName) {
    	return genusName.equals("breed-string") || 
    	genusName.equals("create-and-do") || 
    	genusName.equals("scatter-breeds") || 
    	genusName.equals("create-agents") ||
    	genusName.equals("count-breeds") || 
    	genusName.equals("count-breeds-with");
    }
    
    /**
     * Returns the save String description of the subset factory managed by this.  
     * Save String contains block drawer subset information.  
     * Static drawers are contained in the lang_def file.  
     * @return the save String description of the subset factory managed by this.
     */
    public static String getSaveString() {
    	XMLStringWriter xml = new XMLStringWriter();
    	
    	xml.beginXMLString("BlockDrawerSubsetSets");
    	xml.beginElement("BlockDrawerSubsetSet", true);
    	xml.addAttribute("name", "subset");
    	xml.addAttribute("type", "stack");
    	xml.addAttribute("showFactoryDrawers", "true");
    	xml.addAttribute("showDynamicDrawers", "true");
    	xml.endAttributes();
    	
    	//iterate through and save subset drawers
    	for(Subset subset : Workspace.getInstance().getFactoryManager().getSubsets()) {
    		xml.beginElement("BlockDrawer", true);
    		xml.addAttribute("name", subset.getName());
    		xml.addAttribute("type", "factory");
    		xml.addAttribute("button-color", subset.getColor().getRed()+" "+
    				subset.getColor().getGreen()+" "+subset.getColor().getBlue());
    		xml.endAttributes();
    		//iterate through blocks in drawer
    		for(RenderableBlock block : subset.getBlocks()) {
    			if (block.getBlock() instanceof BlockStub) {
    				xml.beginElement("BlockGenusMember", true);
    				xml.addAttribute("stub-parent", ((BlockStub)block.getBlock()).getParentName());
    				xml.endAttributes();
    				xml.addElementTextData(block.getBlock().getGenusName());
    				xml.endElement("BlockGenusMember");
    			} else if (isDefaultBreedBlockGenus(block.getBlock().getGenusName())) {
    				xml.beginElement("BlockGenusMember", true);
    				xml.addAttribute("breed-name", block.getBlock().getProperty(SLBlockProperties.BREED_NAME));
    				xml.endAttributes();
    				xml.addElementTextData(block.getBlock().getGenusName());
    				xml.endElement("BlockGenusMember");
    			} else {
    				xml.addDataElement("BlockGenusMember", block.getBlock().getGenusName());
    			}
    		}
    		xml.endElement("BlockDrawer");
    	}
    	xml.endElement("BlockDrawerSubsetSet");
    	xml.endXMLString();
    	
    	
    	return xml.toString();
    }

	private static Pattern attrExtractor = Pattern.compile("\"(.*)\"");
    private static String getNodeValue(Node node, String nodeKey){
        Node opt_item = node.getAttributes().getNamedItem(nodeKey);
        if(opt_item != null){
        	Matcher nameMatcher = attrExtractor.matcher(opt_item.toString());
            if (nameMatcher.find()){
            	return nameMatcher.group(1);
            	
            }
        }
        return null;
    }
    
    /**
     * Load the subsets from the specified root into the specified FactoryManager manager
     * @param root Element containing subset information to load
     * @param manager FactoryManager instance to load subset information into
     */
    public static void loadBlockDrawerSubsetSets(Element root) {
    	
    	//TODO ria - this code design sucks (basically just copied the above method) but does the trick for now
    	System.out.println("loading subsets");
    	Pattern attrExtractor=Pattern.compile("\"(.*)\"");
    	Matcher nameMatcher;
    	NodeList drawerSetNodes = root.getElementsByTagName("BlockDrawerSubsetSet");
    	Node drawerSetNode;
    	for(int i=0; i<drawerSetNodes.getLength(); i++){
    		drawerSetNode = drawerSetNodes.item(i);
    		if(drawerSetNode.getNodeName().equals("BlockDrawerSubsetSet")){
    			NodeList drawerNodes=drawerSetNode.getChildNodes();
    			Node drawerNode;
    			//retreive drawer information of this bar
    			for(int j=0; j<drawerNodes.getLength(); j++){
    				drawerNode = drawerNodes.item(j);
    				if(drawerNode.getNodeName().equals("BlockDrawer")){
    					String drawerName = null; 
    					Color buttonColor = Color.blue;
    					StringTokenizer col;
    					nameMatcher=attrExtractor.matcher(drawerNode.getAttributes().getNamedItem("name").toString());
    					if (nameMatcher.find()) {//will be true
    						drawerName = nameMatcher.group(1);
    					}
    					
    					//get drawer's color:
    					Node colorNode = drawerNode.getAttributes().getNamedItem("button-color");
//    					if(colorNode == null){
//    						buttonColor = Color.blue;
//    						System.out.println("Loading a drawer without defined color: ");
//    						for(int ai=0; ai<drawerNode.getAttributes().getLength(); ai++){
//        						System.out.println("\t"+drawerNode.getAttributes().item(ai).getNodeName()+
//        								", "+drawerNode.getAttributes().item(ai).getNodeValue());
//        					}
//    					}else{    	
    					if (colorNode != null) {
	                        nameMatcher=attrExtractor.matcher(colorNode.toString());
	                        if (nameMatcher.find()){ //will be true
	                            col = new StringTokenizer(nameMatcher.group(1));
	                            if(col.countTokens() == 3){
	                            	buttonColor = new Color(Integer.parseInt(col.nextToken()), Integer.parseInt(col.nextToken()), Integer.parseInt(col.nextToken()));
	                            }else{
	                            	buttonColor = Color.BLACK;
	                            }
	                        }
    					}
    					
    					Workspace.getInstance().getFactoryManager().addSubsetDrawer(drawerName, buttonColor);
						
						//get block genuses in drawer and create blocks
						NodeList drawerBlocks = drawerNode.getChildNodes();
						Node blockNode;
						ArrayList<RenderableBlock> drawerRBs = new ArrayList<RenderableBlock>();
						for(int k=0; k<drawerBlocks.getLength(); k++){
							blockNode = drawerBlocks.item(k);
							if(blockNode.getNodeName().equals("BlockGenusMember")){
								String genusName = blockNode.getTextContent();
								assert BlockGenus.getGenusWithName(genusName) != null : "Unknown BlockGenus: "+genusName;
								String parentName = getNodeValue(blockNode, "stub-parent");
								String breedName = getNodeValue(blockNode, "breed-name");
								if (parentName != null) {
									for(TextualFactoryBlock block : BlockUtilities.getAllMatchingBlocks(parentName)) {
										if(block.getfactoryBlock().getBlock().getGenusName().equals(genusName)) {
											drawerRBs.add(new FactoryRenderableBlock(null, block.getfactoryBlock().getBlockID()));
											break;
										}
									}
								} else if (isDefaultBreedBlockGenus(genusName)) {
									Block newBlock = new Block(genusName, false); 	 
                                    newBlock.setBlockLabel(breedName); 	 
                                    newBlock.setProperty("breed-name", breedName); 	 
                                    drawerRBs.add(new FactoryRenderableBlock(null, newBlock.getBlockID()));
								}
								else {
									Block newBlock;
									//don't link factory blocks to their stubs because they will 
									//forever remain inside the drawer and never be active
									newBlock = new Block(genusName, false);
									drawerRBs.add(new FactoryRenderableBlock(null, newBlock.getBlockID()));
								}
							}
						}
						//System.out.println("loading drawer with number of blocks: "+drawerRBs.size());
						Workspace.getInstance().getFactoryManager().addSubsetBlocks(drawerRBs, drawerName);
    				}
    			}
    		}
    	}
    }
}
