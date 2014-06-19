package slcodeblocks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import renderable.FactoryRenderableBlock;
import renderable.RenderableBlock;

import workspace.FactoryManager;
import workspace.Page;
import workspace.Workspace;
import workspace.WorkspaceEvent;
import workspace.WorkspaceListener;
import workspace.WorkspaceWidget;
import breedcontroller.BreedEditorWindow;
import breedcontroller.BreedTag;
import codeblocks.Block;
import codeblocks.BlockStub;
import codeblockutil.CButton;
import codeblockutil.CGraphiteButton;

import static slcodeblocks.NetworkEventHandler.*;

/**
 * Notes about networking: Breeds are identified uniquely by their name. 
 * Remote breeds (those from the network) are imported into the local system
 * by appending the name with '@#'. For example, someone else's Turtles will be
 * imported as our Turtles@0 breed.
 * 
 * <p>When saving, network breeds are saved with their given host id. 
 * In addition, all of the local breeds are saved with the current host id.
 * No other information about remote breeds are stored. 
 */
public class BreedManager implements WorkspaceListener{

    // Network =====================================
    
    /** Any breed with this character in its name is from the network. */
    public static final char NETWORK_BREED_CHAR = '@'; 
    
    /** The host to breed mapping. Each host has a collection of breeds. */
    private static final Map<Long, List<String>> hostToBreeds = new HashMap<Long, List<String>>();
    
    /** The remote breed to host mapping. */
    private static final Map<String, Long> breedToHost = new HashMap<String, Long>();
    
    /** 
     * The breed mappings we are using for the next export.
     * Before every turtle heap export, we have to map the breed slnum to 
     * a <host, breedname> pair so that other hosts can recognize (and 
     * appropriately map their breeds) when they import the turtles.
     */
    private static final Map<Long, BreedInfo> breedSlnumToBreedInfo = new LinkedHashMap<Long, BreedInfo>(); 
    
    // ==============================================
    
    //breedNameToShape has a mapping between existing user-assigned breed names 
    //and starlogo shapes
    private static final LinkedHashMap<String, String> breedNameToShape = new LinkedHashMap<String, String>();
    
    //the list of default breed genuses that are initially added to a breed drawer
    private static final List<String> defaultBreedGenuses = new ArrayList<String>(6);
    {
        defaultBreedGenuses.add("create-agents");
        defaultBreedGenuses.add("create-and-do");
        defaultBreedGenuses.add("breed-string");
        defaultBreedGenuses.add("count-breeds");
        defaultBreedGenuses.add("count-breeds-with");
        defaultBreedGenuses.add("scatter-breeds");
    }
    
    //the Workspace instance, set on application startup
    private static Workspace workspace = null;
    
    //BREED EDITOR STUFF
    private CButton editBreedsButton;
    
    /** used for xml parsing */
    private static Pattern attrExtractor = Pattern.compile("\"(.*)\"");
    
    /** The runtime workspace instance. */
    private static RuntimeWorkspace rw;
    
    /**
     * Constructs a BreedManager instance that manages the breed editor, 
     * breed pages, breed-specific blocks and breed drawers.  
     * @param wworkspace the Workspace instance to manage the breeds of
     */
    public BreedManager(Workspace wworkspace){
        workspace = wworkspace;
        
        //initialize the edit breeds button
        editBreedsButton = new CGraphiteButton("Edit Breeds");
        editBreedsButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        	    List<BreedTag> breedtags = new ArrayList<BreedTag>();
        	    Set<String> origs = new HashSet<String>();
        		for(String name : breedNameToShape.keySet()){
        		    // Do not add any remote breeds! We can't modify
        		    // other people's stuff =)
        		    if (!isRemoteBreedName(name)) {
        		        breedtags.add(new BreedTag(name, name, breedNameToShape.get(name)));
        		        origs.add(name);
        		    }
                }
        		BreedEditorWindow editor = new BreedEditorWindow(breedtags, WorkspaceController.getObserver().getActiveFrame());
        		if(editor.isActionCenceled()){
        			processNewBreeds(origs, editor.getBreedTags());
                }
        		editor = null;
        	}
        });
    }
    
    /** Set the RuntimeWorkspace instance. */
    public static void setRuntimeWorkspace(RuntimeWorkspace runw) {
        rw = runw;
    }
    
    /**
     * Returns the breed shape associated with the breed name; null if breedName does not exist.
     * @param breedName  
     * @return the breed shape associated with the breed name; null if breedName does not exist.
     */
    public static String getShapeWithBreedName(String breedName){
        //projects may have similar workspaces
        return breedNameToShape.get(breedName);
    }
    
    /**
     * Returns true if this breed name indicates a remote breed.
     */
    private static boolean isRemoteBreedName(String breedName) {
        return breedName.indexOf(NETWORK_BREED_CHAR) >= 0;
    }
    
    /**
     * Returns the "Edit Breeds" button that launches the breed editor
     * @return the "Edit Breeds" button that launches the breed editor
     */
    public CButton getEditBreedsButton(){
        return editBreedsButton;
    }
    
    /**
     * @returns the current Factory Manager in the workspace
     */
    public static FactoryManager getFactory(){
    	return workspace.getFactoryManager();
    }
    
    /**
     * Returns true if the specified name is a valid name for a breed.  For example,
     * if the name is equal to either Setup, Runtime, Patch (which are administrative 
     * pages) or empty, this method returns false.
     * @param name the String name to validate
     * @return true if the specified name is a valid name for a breed; false otherwise.
     */
    public static boolean isValidBreedName(String name){
        return (!name.equals("Setup") && !name.equals("Runtime") && !name.equals("Everyone") &&
                !name.equals("Patches") && !name.equals("Collisions") && !name.equals(""));
    }
    
    /**
     * Returns true if the breed exists.
     */
    public static boolean isExistingBreed(String name) {
        return name != null && breedNameToShape.containsKey(name);
    }
    
    /**
     * Adds the default set of blocks from the genuses specified in 
     * defaultBreedGenuses to the specified drawer
     * @param drawer PageBlockDrawer to add the default blocks to
     */
    private static void addDefaultBlocksToBreedDrawer(String drawer, String breedName){
        ArrayList<RenderableBlock> blocksToAdd = new ArrayList<RenderableBlock>();
    	for(String genus : defaultBreedGenuses){
            Block block = new Block(genus);
            block.setBlockLabel(breedName);
            //set breed of this block
            block.setProperty(SLBlockProperties.BREED_NAME, breedName);
            //block.setBlockLabel()
            blocksToAdd.add(new FactoryRenderableBlock(getFactory(), block.getBlockID()));
        }
    	getFactory().addDynamicBlocks(blocksToAdd, drawer);
    }
    
    /**
     * Creates a new Block instance of genus collision with the specified collider and collidee
     * @param collider the breed performing the collision
     * @param collidee the breed being collided with
     * @return a new Block instance of genus collision with the specified collider and collidee
     */
    private static Block createCollisionBlock(String collider, String collidee){
        Block col = new Block("collision");
        col.getSocketAt(0).setLabel(collider);
        col.getSocketAt(1).setLabel(collidee);
        return col;
    }
    
    /**
     * Returns true if the specified block is of genus collision; false otherwise
     * @param block the Block instance to test
     * @return true if the specified block is of genus collision; false otherwise 
     */
    private static boolean isCollisionBlock(Block block){
        return block.getGenusName().equals("collision");
    }
    
    /**
     * Returns true iff this block is owned by a particular breed
     * @param block the Block instance to test
     * @return true iff this block is owned by a particular breed
     */
    private static boolean isBreedBlock(Block block){
        return SLBlockProperties.YES.equals(block.getProperty(SLBlockProperties.IS_OWNED_BY_BREED));
    }
    
    /**
     * Returns true if this block belongs to any of the default genuses 
     * for breeds.
     * @param block 
     * @return
     */
    private static boolean isDefaultBreedBlock(Block block){    
        return defaultBreedGenuses.contains(block.getGenusName());
    }
    
    /**
     * Returns the collider name associated with this collision block
     * @param colBlock a collision block instance
     * @return the collider name associated with this collision block
     */
    private static String getColliderName(Block colBlock){
        assert isCollisionBlock(colBlock) : "block not collision block "+colBlock;
        return colBlock.getSocketAt(0).getLabel();
    }
    
    /**
     * Returns the collidee name associated with this collision block
     * @param colBlock a collision block instance
     * @return the collidee name associated with this collision block
     */
    private static String getCollideeName(Block colBlock){
        assert isCollisionBlock(colBlock) : "block not collision block "+colBlock;
        return colBlock.getSocketAt(1).getLabel();
    }
    
    /**
     * Sets the Collidee name of the specified colBlock with the given String name
     * @param colBlock the Collision block to update
     * @param name the new collidee name
     */
    private static void setCollideeName(Block colBlock, String name){
        assert isCollisionBlock(colBlock) : "Block not collision block "+colBlock;
        colBlock.getSocketAt(1).setLabel(name);
    }
    
    /**
     * Sets the Collider name of the specified colBlock with the given String name
     * @param colBlock the Collision block to update
     * @param name the new collider name
     */
    private static void setColliderName(Block colBlock, String name){
        assert isCollisionBlock(colBlock) : "Block not collision block "+colBlock;
        colBlock.getSocketAt(0).setLabel(name);
    }
    
    /**
     * Updates the collision blocks in the specified drawer and the drawers of other breeds.
     * This method assumes that the specified drawer was just added and so it will add
     * a collision block to specify a collision with itself and collision blocks with other species.
     * This method will also add a new collision block in the other drawers, that specify a collision
     * with this new breed
     * @param drawer the drawer of the specified breedName
     * @param breedName the String breedName of a breed just added
     */
    private static void addNewCollisionBlocks(String drawer, String breedName){
        //add a collision blocks to specified drawer
        Block col_myself = createCollisionBlock(breedName, breedName);
        getFactory().addDynamicBlock(new RenderableBlock(getFactory(), col_myself.getBlockID()), drawer);
        //add collision blocks with other breeds in the specified drawer
        //then update the collision blocks in other breed drawers to include a 
        //collision with this specified breedName
        for(String otherBreedName : breedNameToShape.keySet()){
        	if (otherBreedName == null) throw new RuntimeException("Why do we have a breed name that's null?");
            if(!otherBreedName.equals(breedName)){ 
                //wait until the other breed drawer is created, it will update
                //the collision blocks of the new drawer accordingly
                Block colblock = createCollisionBlock(breedName, otherBreedName);
                getFactory().addDynamicBlock(new RenderableBlock(getFactory(), colblock.getBlockID()), drawer);
                // add other col blocks with new breed to other drawers unless
                // the other breed is remote
                if (!isRemoteBreedName(otherBreedName)) {
                    colblock = createCollisionBlock(otherBreedName, breedName);
                    getFactory().addDynamicBlock(new RenderableBlock(getFactory(), colblock.getBlockID()), otherBreedName);
                }
            }
        }
    }
    
    /**
     * Adds the collision involving the specified breedName to all other breed drawers
     * @param breedName
     */
    private static void addCollisionBlockOf(String breedName){
        for(String otherBreedName : breedNameToShape.keySet()){
            if(!otherBreedName.equals(breedName) && !isRemoteBreedName(otherBreedName)){
            	Block block = createCollisionBlock(otherBreedName, breedName);
                RenderableBlock renderable = new RenderableBlock(getFactory(), block.getBlockID());
                getFactory().addDynamicBlock(renderable, otherBreedName);
            }
        }
    }
    
    /**
     * Updates Collision blocks of the new breed name.  
     * Assumes that associated block drawer has been updated.
     * @param oldName
     * @param newName
     */
    private static void updateCollisionBlocks(String oldName, String newName){
        //update the collision blocks within the drawers
        //and within the workspace
        
        for(String breedName : breedNameToShape.keySet()){
            if (isRemoteBreedName(breedName)) continue;
            for(RenderableBlock rb : getFactory().getDynamicBlocks(breedName)){
                updateCollisionBlock(rb, oldName, newName);
            }
        }
        
        for(RenderableBlock b : workspace.getRenderableBlocksFromGenus("collision")){
            updateCollisionBlock(b, oldName, newName);
        }
    }
    
    private static void updateCollisionBlock(RenderableBlock colBlock, String oldName, String newName){
        Block block = Block.getBlock(colBlock.getBlockID());
        boolean colChanged = false;
        if(isCollisionBlock(block)){
            if(getCollideeName(block).equals(oldName)){
                setCollideeName(block, newName);
                colChanged = true;
            }
            if(getColliderName(block).equals(oldName)){
                setColliderName(block, newName);
                colChanged = true;
            }
            if(colChanged)
                colBlock.repaintBlock();
        }
    }
    
    /**
     * Updates the default breed blocks, such as create blocks, of the new
     * breed name.
     * Assumes that associated block drawer has been updated.
     * @param oldName
     * @param newName
     */
    private static void updateDefaultBreedBlocks(String oldName, String newName){
        for(String otherBreedName : breedNameToShape.keySet()){
            if (isRemoteBreedName(otherBreedName)) continue;
            for(RenderableBlock rb : getFactory().getDynamicBlocks(otherBreedName)){
                updateDefaultBreedBlock(rb, oldName, newName);
            }
        }
        
        for(RenderableBlock b : workspace.getRenderableBlocks()){
            updateDefaultBreedBlock(b, oldName, newName);
        }
    }
    
    private static void updateDefaultBreedBlock(RenderableBlock block, String oldName, String breedName){
        Block b = Block.getBlock(block.getBlockID());
        
        for(String genus : defaultBreedGenuses){
            if(b.getProperty(SLBlockProperties.BREED_NAME)!= null &&
                    b.getProperty(SLBlockProperties.BREED_NAME).equals(oldName)){
                if(b.getGenusName().equals(genus)){
                    b.setBlockLabel(breedName);
                    //set breed of this block
                    b.setProperty(SLBlockProperties.BREED_NAME, breedName);
                    block.repaintBlock();
                    return;
                }
            }
        }
        
        //otherwise this block is just a breed block but not a default
        //block
        if(isBreedBlock(b) && b.getProperty(SLBlockProperties.BREED_NAME)!= null &&
                b.getProperty(SLBlockProperties.BREED_NAME).equals(oldName)){
            b.setProperty(SLBlockProperties.BREED_NAME, breedName);
        }
        
    }
    
    /**
     * Removes the collision blocks that involve the specified breedName 
     * from all the breed drawers
     * @param breedName the String breed name 
     */
    private static void removeCollisionBlockOf(String breedName){
        //if this method is being called because a page was removed, this 
        //method assumes that the page block drawer associated with the removed page, 
        //has already been removed
        //note: there is room for optimization here
        for(String otherBreedName : breedNameToShape.keySet()){
            if(!otherBreedName.equals(breedName) && !isRemoteBreedName(otherBreedName)){
                for(RenderableBlock rb : getFactory().getDynamicBlocks(otherBreedName)){
                    Block block = Block.getBlock(rb.getBlockID());
                    if(isCollisionBlock(block) && getCollideeName(block).equals(breedName)){
                        getFactory().removeDynamicBlock(rb, otherBreedName);                      
                    }
                }
            }
        }
    }
    
    private static void removeComplementaryCollisionBlock(RenderableBlock colBlock){
        // If the other breed is remote, we don't have a complimentary block
        Block b = Block.getBlock(colBlock.getBlockID());
        if (isRemoteBreedName(getCollideeName(b))) return;
        
        for(RenderableBlock rb : getFactory().getDynamicBlocks(getCollideeName(b))){
            Block block = Block.getBlock(rb.getBlockID());
            if(isCollisionBlock(block) && getCollideeName(block).equals(getColliderName(b))
                    && getColliderName(block).equals(getCollideeName(b))){
                getFactory().removeDynamicBlock(rb, getCollideeName(b));
            }
        }
    }    
    
    
    private static void removeDuplicateCollisionBlock(RenderableBlock colBlock){
        Block b = Block.getBlock(colBlock.getBlockID());
        String drawer = getColliderName(b);
        if (isRemoteBreedName(drawer)) return;
        for(RenderableBlock rb : getFactory().getDynamicBlocks(drawer)){
            Block block = Block.getBlock(rb.getBlockID());
            if(isCollisionBlock(block) && getCollideeName(block).equals(getCollideeName(b))
                    && getColliderName(block).equals(getColliderName(b))){
                getFactory().removeDynamicBlock(rb, drawer);
            }
        }

    }
    
    /**
     * Returns the Page with the specified name from the workspace; null
     * if the page does not exist.
     * @param name the String name of the Page to search for
     * @return 
     */
    private static Page getPageWithName(String name){
        Page page = null;
        for(WorkspaceWidget w : workspace.getWorkspaceWidgets()){
            if(w instanceof Page){
                if(((Page)w).getPageName().equals(name))
                    page = (Page)w;
            }
        }
        
        return page;
    }
    
    public void workspaceEventOccurred(WorkspaceEvent event) {
        Block block = Block.getBlock(event.getSourceBlockID());
        String drawer = null;
        switch(event.getEventType()){
            case WorkspaceEvent.PAGE_ADDED:
                //add appropriate breed blocks
                //get pages from workspace
                //IF NOT IGNORING PAGE EVENTS
                Page page = (Page)event.getSourceWidget();
                //System.out.println("received page added (bm) for page "+page.getPageName()); 
                if(isValidBreedName(page.getPageName()) && isExistingBreed(page.getPageName())){
                    //System.out.println("name is valid and breedNameToShape has it with drawer name "+page.getPageDrawer());
                    drawer = page.getPageDrawer();
                    //System.out.println("drawer widget found: "+drawerName);
                    //if workspace is still loading, hold off in adding these blocks for now
                    //will get added in finishLoad()
                    if(!WorkspaceController.isWorkspaceLoading()){
                        //create default breed blocks for breed drawer
                        addDefaultBlocksToBreedDrawer(drawer, page.getPageName());
                        //update the collision blocks of each breed
                        addNewCollisionBlocks(drawer, page.getPageName());
                    }
                    //update the iconic image of this page
                    StarLogoShape shape = AvailableShapes.getShape(breedNameToShape.get(page.getPageName()));
                    if(shape != null){
                    	page.setIcon(shape.icon);
                    }
                    //TODO update run blocks
                }
                break;
            case WorkspaceEvent.PAGE_REMOVED:
                //remove page and drawer button from page bar and associated drawer
                page = (Page)event.getSourceWidget();
                drawer = page.getPageDrawer();
                if(drawer != null && drawer.length() > 0){  //drawer may not exist for this page, although it should (the page being removed may be the default starter page) 
                    getFactory().removeDynamicDrawer(drawer);
                    //now that all other references have been removed, remove the collision blocks that involve this 
                    //breed from the other breed drawers
                    removeCollisionBlockOf(page.getPageName());
                }
                break;
            case WorkspaceEvent.PAGE_RENAMED:
                //iterate through the blocks in this page
                page = (Page)event.getSourceWidget();

                // update the blocks within drawers and workspace...
                //like collision blocks, create blocks, etc... 
                updateCollisionBlocks(event.getOldNameOfSourceWidget(), page.getPageName());
                updateDefaultBreedBlocks(event.getOldNameOfSourceWidget(), page.getPageName());
                //update the rest of the blocks
                for(RenderableBlock rb : page.getBlocks()){
                    Block b = Block.getBlock(rb.getBlockID());
                    if(isBreedBlock(b) && b.getProperty(SLBlockProperties.BREED_NAME) != null
                            && b.getProperty(SLBlockProperties.BREED_NAME).equals(event.getOldNameOfSourceWidget())){
                        b.setProperty(SLBlockProperties.BREED_NAME, page.getPageName());
                    }
                }
                break;
            case WorkspaceEvent.BLOCK_ADDED:
                //UPDATE THE BREED NAME OF THE BLOCK ADDED IF IT IS A BREED BLOCK
                if(block instanceof BlockStub){
                    //the breed name of a stub can only change if its parent changes
                    //so just set the breed name of this stub to its page label
                	Block parentBlock = SLBlockProperties.getParent(block);
                	if(parentBlock != null) {
                        String parentProp = parentBlock.getProperty(SLBlockProperties.BREED_NAME);
                        if (parentProp != null) {
                            block.setProperty(SLBlockProperties.BREED_NAME, parentProp);
                        }
                	}
                }else if(isBreedBlock(block)){
                    if(isDefaultBreedBlock(block)){
                        block.setProperty(SLBlockProperties.BREED_NAME, block.getBlockLabel().substring(block.getLabelPrefix().length(), block.getBlockLabel().length()-block.getLabelSuffix().length()));
                    }else{
                        if(event.getSourceWidget() instanceof Page){
                            page = (Page)event.getSourceWidget();
                            block.setProperty(SLBlockProperties.BREED_NAME, page.getPageName());
                            if(block.hasStubs()){
                                //update the breed name of this block's stubs
                                for(Long stubID : BlockStub.getStubsOfParent(block.getBlockID())){
                                    Block.getBlock(stubID).setProperty(SLBlockProperties.BREED_NAME, page.getPageName());
                                }
                            }
                        }
                    }
                }
                //UPDATE COLLISION BLOCKS IF THIS IS A COLLISION BLOCK
                if(event.getSourceWidget() instanceof Page && isCollisionBlock(block)){
                    //test first that this is not a "self" collision block
                    if(!getColliderName(block).equals(getCollideeName(block))){
                        //remove collision blocks from other drawers that have this similar collision
                        //pageremoved would do the same as well
                        removeComplementaryCollisionBlock(RenderableBlock.getRenderableBlock(block.getBlockID()));
                    }
                }                
                // Update dependencies if this block is a procedure declaration. 
                if (block.isProcedureDeclBlock()) {
                    DependencyManager.updateDependencies();
                }
                break;
            case WorkspaceEvent.BLOCK_REMOVED:
            	if(isCollisionBlock(Block.getBlock(event.getSourceBlockID()))){
                    //restore collision block in original block drawer (for now) in the future will need to restore collision block to all drawers
                    drawer = getColliderName(block);
                    if(drawer != null){ //should not be null, if it is, breeds are not being updated correctly in breedNameToShape
                        //RenderableBlock rb = RenderableBlock.getRenderableBlock(event.getSourceBlockID()); 
                    	// this creates a new collision block to replace the deleted one, if the breeds still exist for the deleted block.
                    	// (reusing the old one was bad because it would still have links to blocks that used to be connected to it)
                    	if (breedNameToShape.containsKey(getCollideeName(block))) {
                    		RenderableBlock rb = new RenderableBlock(getFactory(), createCollisionBlock(getColliderName(block),getCollideeName(block)).getBlockID()); 
                    		getFactory().addDynamicBlock(rb, drawer);
                    	}
                    }
                    //before restoring collision block to other drawers, test if it is not a "self" collision block
                    if(!getColliderName(block).equals(getCollideeName(block))){
                        //note: the problem with calling this method is that this method will create new block and renderable block instances of 
                        //collision blocks instead of using the collision blocks that were removed earlier
                    	addCollisionBlockOf(getColliderName(block));
                    }
                }
                break;
        }
    }
    
    ////////////////////////
    // SAVING AND LOADING //
    ////////////////////////
    
    /**
     * Returns an escaped (safe) version of string.
     */
    private static String escape(String s) {
        return s.replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;");
    }
    
    /**
     * Returns the save string represeting the breed name to shape mapping
     * @return the save string represeting the breed name to shape mapping
     */
    public static String getBreedNameToShapeSaveString(){
        StringBuffer saveString = new StringBuffer();
        
        saveString.append("<BreedShapeMappings>");
        for(String breedName : breedNameToShape.keySet()){
            saveString.append("<BtoSMapping>");
            saveString.append("<BreedName>");
            saveString.append(escape(breedName));
            saveString.append("</BreedName>");
            saveString.append("<BreedShape>");
            saveString.append(breedNameToShape.get(breedName));
            saveString.append("</BreedShape>");
            if (breedToHost.containsKey(breedName)) {
                saveString.append("<BreedId>");
                saveString.append(breedToHost.get(breedName));
                saveString.append("</BreedId>");
            }
            saveString.append("</BtoSMapping>");
        }
        
        // If we have a server hash, we append it now.
        long hash = WorkspaceController.getNetworkManager().getServerHash();
        if (hash != 0) {
            saveString.append("<BreedHost id=\"");
            saveString.append(hash);
            saveString.append("\"/>");
        }
        
        saveString.append("</BreedShapeMappings>");
        
        return saveString.toString();
    }
    
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
     * Loads the breed pages and shapes associated with them into the 
     * breedNameToShape map.
     */
    public static void loadPageAndBreedShape(Element root){
        SLBlockObserver obs = WorkspaceController.getObserver();
        NodeList mappingsList = root.getElementsByTagName("BtoSMapping");
        for(int i=0; i<mappingsList.getLength(); i++){
            Node mappingNode = mappingsList.item(i);
            NodeList children = mappingNode.getChildNodes();
            String breedName = null;
            String breedShape = null;
            String breedHost = null;
            Node childNode;
            for(int j=0; j<children.getLength(); j++){
                childNode = children.item(j);
                if(childNode.getNodeName().equals("BreedName")){
                    breedName = childNode.getTextContent();
                }else if(childNode.getNodeName().equals("BreedShape")){
                    breedShape = childNode.getTextContent();
                }else if (childNode.getNodeName().equals("BreedId")) {
                    breedHost = childNode.getTextContent();
                }
            }
            
            if(breedName != null && breedShape != null && isValidBreedName(breedName)){
                if (breedHost != null) {
                    try {
                        // This is a network host. 
                        addRemoteBreed(Long.parseLong(breedHost), breedName, breedShape);
                        obs.addBreed(breedName, breedShape);
                    }
                    catch (NumberFormatException e) {
                        System.err.println("Invalid host: " + breedHost);
                    }
                }
                else {
                    // for local breeds, we just keep track of the shapes.
                    breedNameToShape.put(breedName, breedShape);
                    obs.addBreed(breedName, breedShape);
                }
            }
        }
        
        mappingsList = root.getElementsByTagName("BreedHost");
        if (mappingsList.getLength() > 0) {
            String id = getNodeValue(mappingsList.item(0), "id");
            try {
                WorkspaceController.getNetworkManager().loadServerHash(Long.parseLong(id));
            }
            catch (NumberFormatException e) {
                System.err.println("Invalid host id: " + id);
                WorkspaceController.getNetworkManager().loadServerHash(0);
            }
        }
        else {
            WorkspaceController.getNetworkManager().loadServerHash(0);
        }
    }
    
    /**
     * Performs any final updates and changes to the state of this
     * BreedManager.  Must be called whenever a project save file
     * is loaded to achieve the proper state of this.  
     * 
     * <p>Note that any remote breeds are not loaded in this method, since 
     * they will be loaded when the remote breed information is passed.
     */
    public static void finishLoad(){
        //populate the breed drawers for local breeds only
        for(String breedName : breedNameToShape.keySet()){
            if (isRemoteBreedName(breedName)) continue;
            String drawer = breedName;
            //create default breed blocks for breed drawer
            addDefaultBlocksToBreedDrawer(drawer, breedName);
            //update the collision blocks of each breed
            addNewCollisionBlocks(drawer, breedName);
        }
        
        for(RenderableBlock b : workspace.getRenderableBlocksFromGenus("collision")){
            removeComplementaryCollisionBlock(b);
            removeDuplicateCollisionBlock(b);
        }
    }
    
    /**
     * Resets the state of this manager.
     *
     */
    public static void reset(){
        breedNameToShape.clear();
        hostToBreeds.clear();
        breedToHost.clear();
    }
    
    // Remote Breeds ===========================================
    
    /** Add a remote breed to our mappings. */
    private static void addRemoteBreed(Long id, String breedName, String shape) 
    {
        // Note that we add the name WE have given it, not the original.
        List<String> breeds = hostToBreeds.get(id);
        if (breeds == null) {
            breeds = new ArrayList<String>();
            hostToBreeds.put(id, breeds);
        }
        breeds.add(breedName);
        breedToHost.put(breedName, id);
        breedNameToShape.put(breedName, shape);
    }
    
    /** Generate a new remote breed name with the given prefix. */
    private static String genRemoteBreed(String name) {
        String breedName = name + NETWORK_BREED_CHAR;
        int i;
        for (i = 0; ; i++) {
            if (!breedNameToShape.containsKey(breedName + i))
                return breedName + i;
        }
    }
    
    /** 
     * Find the local name for the remote breed with the given name. For 
     * example, getLocalName(1034518934, "Turtles") might return "Turtles@1".
     * Returns null if not found. 
     */
    private static String getLocalName(Long id, String name) {
        List<String> breeds = hostToBreeds.get(id);
        if (breeds == null) return null;
        name = name + NETWORK_BREED_CHAR;
        for (String breed : breeds) {
            if (breed.startsWith(name)) return breed;
        }
        return null;
    }

    /**
     * Import a breed from the network. Note that the import does NOT grant
     * the user any rights to the new breed (no changes can be made). 
     * 
     * @param id The host id the breed originated from
     * @param name The breed's name on the original host
     * @param shape The shape of the breed
     */
    static void importBreed(Long id, String name, String shape) {
        // Don't import the same breed twice. This shouldn't happen, but
        // who knows. 
        if (getLocalName(id, name) != null) return;
        
        // Generate a new local name for this breed
        String breedName = genRemoteBreed(name);
        
        // Add this breed for the given host.
        addRemoteBreed(id, breedName, shape);
        WorkspaceController.getObserver().addBreed(breedName, shape);
		
		// add a collision block in existing drawers
        addCollisionBlockOf(breedName);
        
		System.out.println("Importing breed " + name + " from host " + id +
	        " as: " + breedName + "(" + shape + ")");
	}
    
    /**
     * Delete a remote breed.
     * 
     * @param id The host id the breed originated from
     * @param name The breed's name on the original host
     */
    static void deleteBreed(Long id, String name) {
        name = getLocalName(id, name);
        if (name == null) return;
        
        hostToBreeds.get(id).remove(name);
        breedToHost.remove(name);
        breedNameToShape.remove(name);
        WorkspaceController.getObserver().deleteBreed(name);
        
        // remove collision blocks from existing drawers
        removeCollisionBlockOf(name);
        
        System.out.println("Deleted remote breed " + name);
    }
    
    /**
     * Update a remote breed.
     * 
     * @param id The host id the breed originated from
     * @param oldName The breed's old name
     * @param newName The breed's new name (or the same as old name)
     * @param shape The breed's new shape
     */
    static void updateBreed(Long id, String oldName, String newName, String shape) {
        String breed = getLocalName(id, oldName);
        if (breed == null) return;
        
        // Update the name, if necessary.
        if (!oldName.equals(newName)) {
            newName = genRemoteBreed(newName);
            hostToBreeds.get(id).remove(breed);
            hostToBreeds.get(id).add(newName);
            breedToHost.remove(breed);
            breedToHost.put(newName, id);
            breedNameToShape.remove(breed);
            WorkspaceController.getObserver().renameBreed(breed, newName, shape);
            
            // Update collision blocks
            updateCollisionBlocks(breed, newName);
            breed = newName;
        }
        
        // Update the shape.
        breedNameToShape.put(breed, shape);
        WorkspaceController.getObserver().updateBreedShape(breed, shape);
        
        System.out.println("Updated remote breed: " + newName);
    }
    
    /**
     * Send the initial breed information to some new neighbors. This method
     * should be called whenever a new direct network connection is made.
     */
    static void sendBreedInfo(Long host) {
        List<String> names = new ArrayList<String>(breedNameToShape.size());
        List<String> shapes = new ArrayList<String>(breedNameToShape.size());
        Set<String> allBreeds = new HashSet<String>(breedNameToShape.keySet());
        SLNetworkManager netMgr = WorkspaceController.getNetworkManager();
        
        // Remote breeds.
        for (Long id : hostToBreeds.keySet()) {
            names.clear();
            shapes.clear();
            for (String breed : hostToBreeds.get(id)) {
                names.add(breed.substring(0, breed.indexOf(NETWORK_BREED_CHAR)));
                shapes.add(breedNameToShape.get(breed));
                allBreeds.remove(breed);
            }
            netMgr.sendEvent(host, breedSummary(id, names, shapes));
        }
        
        // Local breeds = whatever's left over in allBreeds.
        names = new ArrayList<String>(allBreeds);
        shapes.clear();
        for (String breed : names) {
            shapes.add(breedNameToShape.get(breed));
        }
        netMgr.sendEvent(host, breedSummary(netMgr.getServerHash(), names, shapes));
    }
    
    // Importing/Exporting Turtles ===============================
    
    /** Call this method when preparing to export turtles from the VM. */
    public static void turtlesToExport() {
        breedSlnumToBreedInfo.clear();
    }
    
    /** Returns the map of breeds to export. */
    static Map<Long, BreedInfo> getExportBreedMap() {
        return breedSlnumToBreedInfo;
    }
    
    /** Add an export breed mapping. */
    public static void addExportBreed(Long breedslnum, String breedname) {
        int index = breedname.indexOf(NETWORK_BREED_CHAR);
        Long host;
        if (index == -1) {
            // Local breed
            host = WorkspaceController.getNetworkManager().getServerHash();
        }
        else {
            host = breedToHost.get(breedname);
            assert host != null;
            breedname = breedname.substring(0, breedname.indexOf(NETWORK_BREED_CHAR));
        }
        
        breedSlnumToBreedInfo.put(breedslnum, new BreedInfo(host, breedname));
    }
    
    /** Import agents from another host. */
    static void receiveAgents(long[] agentHeap, long[] slnums, Long[] hosts, String[] names) {
        SLBlockObserver obs = WorkspaceController.getObserver();
        int len = slnums.length;
        assert hosts.length == len && names.length == len;
        
        // Create our mapping (remote -> local slnums).
        // Unrecognized breeds will be imported as the default breed in the
        // VM (breed[0]).
        long[] map = new long[len * 2];
        for (int i = 0; i < len; i++) {
            map[2*i] = slnums[i];
            String localName = getLocalName(hosts[i], names[i]);
            map[2*i+1] = localName == null ? -1L : obs.getBreedSlnum(localName);
        }
        
        System.out.println("Importing turtles");
        System.out.println(Arrays.toString(map));
        obs.importTurtles(map, agentHeap);
    }
    
    // ===========================================================
    
    /**
     * @param orig Modifiable set of original local breed names
     */
    private static void processNewBreeds(Set<String> orig, List<BreedTag> breedtags){
    	SLBlockObserver obs = WorkspaceController.getObserver();
    	SLNetworkManager mgr = WorkspaceController.getNetworkManager();
    	
    	// Deleted Breeds ===================================
    	for (BreedTag tag : breedtags) {
    	    orig.remove(tag.getID());
    	}
    	
    	for (String breed : orig) {
    	    breedNameToShape.remove(breed);
    	    Page page = getPageWithName(breed);
    	    for (RenderableBlock block : page.getBlocks()) {
    	        rw.blockRemoved(Block.getBlock(block.getBlockID()));
    	    }
    	    workspace.removePage(getPageWithName(breed));
    	    obs.deleteBreed(breed);
    	    mgr.sendEvent(breedDeleted(breed));
    	    // TODO mark related blocks "unusable" w/red highlight or something
    	}
    	
    	// ==================================================
    	int breedCount = 0;
    	for (BreedTag tag : breedtags) {
    	    String id = tag.getID();
    	    String name = tag.getName();
    	    String shape = tag.getShape();
    	    
    	    // Added Breeds ======================================
    	    if (!breedNameToShape.containsKey(id)) {
    	        breedNameToShape.put(name, shape);
    	        getFactory().addDynamicDrawer(name, breedCount);
    	        workspace.addPage(new Page(name), breedCount);
    	        obs.addBreed(name, shape);
    	        mgr.sendEvent(breedAdded(name, shape));
    	    }
    	    
    	    else {
    	        boolean changed = false;
    	        
    	        // Reshaped Breeds ===============================
    	        if (!breedNameToShape.get(id).equals(shape)) {
    	            breedNameToShape.put(id, shape);
    	            obs.updateBreedShape(id, shape);
    	            workspace.getPageNamed(id).setIcon(tag.getImage());
    	            changed = true;
    	        }
    	        
    	        // Renamed Breeds ================================
    	        if (!id.equals(name)) {
    	            breedNameToShape.put(name, shape);
    	            breedNameToShape.remove(id);
    	            workspace.renamePage(id, name);
    	            obs.renameBreed(id, name, shape);
    	            changed = true;
    	        }
    	        
    	        if (changed) {
    	            mgr.sendEvent(breedUpdated(id, name, shape));
    	        }
    	    }
    	    
    	    breedCount++;
    	}
    }
    
    // BreedInfo ======================================================
    
    /**
     * The BreedInfo class encapsulates a breed host and name.
     */
    public static class BreedInfo {
        public final Long host;
        public final String name;
        public BreedInfo(Long h, String n) {
            host = h;
            name = n;
        }
        public String toString() {
            return name + NETWORK_BREED_CHAR + host;
        }
    }
}

