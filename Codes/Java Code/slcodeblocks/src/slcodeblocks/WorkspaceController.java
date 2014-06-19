package slcodeblocks;

import importer.Importer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import workspace.SearchBar;
import workspace.SearchableContainer;
import workspace.TrashCan;
import workspace.Workspace;
import workspace.WorkspaceEvent;
import workspace.WorkspaceListener;
import workspace.ZoomSlider;
import workspace.typeblocking.TypeBlockManager;
import codeblocks.BlockConnectorShape;
import codeblocks.BlockGenus;
import codeblocks.BlockLinkChecker;
import codeblocks.BlockShape;
import codeblocks.CommandRule;
import codeblocks.InfixRule;
import codeblocks.SocketRule;
import codeblockutil.CBorderlessButton;
import codeblockutil.CButton;
import codeblockutil.CHeader;


/**
 * <code>WorkspaceController</code> is a StarlogoTNG dependent class.  It is responsible for building 
 * and initializing the <code>Workspace</code> at startup and loading of new and old projects.  
 * It is also responsible for passing Application level requests and actions to the Workspace.
 * 
 * There is only one instance of the <code>WorkspaceController</code>
 */
public class WorkspaceController implements WorkspaceListener{

    /** This marks the version of old projects */
    private static final double OLD_LOAD_VERSION = 10400;
    
    private static final String LANG_DEF_FILEPATH = "support/lang_def.xml";
//    private static final String SAVE_FORMAT_DTD_FILEPATH = "support/save_format.dtd";
    
    /** Empty variable list. */
    private static final List<Variable> EMPTY_VARS_LIST = 
        new ArrayList<Variable>();
    
    private static Element langDefRoot;
    
    /** saveTitle denotes the section the slcodeblocks save String will 
     * have in the overall Starlogo TNG save file */
    private static final String saveTitle = "`slcodeblocks`";
    
    //flags 
    private boolean isWorkspacePanelInitialized = false;
    
    /** The single instance of the Workspace Controller for Starlogo TNG*/
    private static WorkspaceController wc = new WorkspaceController();
    
    private static BreedManager bm;
    private static ZoomSlider zoomSlider;
    private static RunBlockManager rbm;
    private static ProcedureOutputManager pom;
    
    private JPanel workspacePanel;
    private static Workspace workspace;
    private static boolean isWorkspaceLoading = false;
    private static SearchBar searchBar;
    
    
    //Keep a pointer to Application (SLBlockObserver)
    private static SLBlockObserver observer;
    
    private static SLNetworkManager networkManager;
    
    /**
     * Only one instance of the Workspace controller will exist to manipulate and 
     * manage the only instance of the workspace.
     */
    private WorkspaceController(){
        workspace = Workspace.getInstance();
        bm = new BreedManager(workspace);
        //init runblock manager which manages all run blocks
        rbm = new RunBlockManager(workspace);
        pom = new ProcedureOutputManager(workspace);
        
        workspace.addWorkspaceListener(rbm);
        workspace.addWorkspaceListener(this);
        workspace.addWorkspaceListener(bm);
        //workspace = loadFreshWorkspace();
        networkManager = new SLNetworkManager();
        
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document doc;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(new File(LANG_DEF_FILEPATH));
            langDefRoot = doc.getDocumentElement();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Returns the single instance of this
     * @return the single instance of this
     */
    public static WorkspaceController getInstance(){
        return wc;
    }
    
    public static SLNetworkManager getNetworkManager() {
		return networkManager;
    }
    
    public static BreedManager getBreedManager() {
		return bm;
    }
    
//  TODO old starlogoblocks has agent monitor control too, should we have this in here as well?
    
    /**
     * Sets the workspace zoom to the specified zoom level
     * @param zoom the desired zoom level
     */
    public void setWorkspaceZoom(double zoom){
        
    }
    
    /**
     * Returns the workspace zoom level
     * @return the workspace zoom level
     */
    public double getWorkspaceZoom(){
        return 0;
    }
    
    /**
     * Automatically organizes all the blocks within the block canvas.
     */
    public void cleanUpAllBlocks(){
        workspace.cleanUpAllBlocks();
    }
    
    /**
     * Copies the highlighted blocks
     */
    public void copyWorkspaceAction() {
    	workspace.copyBlocks();
    }
    
    /**
     * Pastes the highlighted blocks
     */
    public void pasteWorkspaceAction() {
    	workspace.pasteBlocks();
    }
    
    /**
     * Automatically sends focus to the search bar, ready for the user
     * to input a search command.
     */
    public void activateSearchBar(){
        searchBar.getComponent().requestFocus();
    }
    
    /**
     * Enables or disables typeblocking depending on the specified enable parameter
     * @param enable Enables typeblocking if true; Disables typeblocking otherwise
     */
    public void setTypeBlockingEnabled(boolean enable){
        
    }
    
    
    /**
     * Enables or disables sound on the workspace
     * @param enable Enables sound if true; Disables sound otherwise
     */
    public void setSoundEnabled(boolean enable){
        
    }
    
    /**
     * Adds the specified WorkspaceListener to the Workspace
     * @param listener
     */
    public void addWorkspaceListener(WorkspaceListener listener){
        workspace.addWorkspaceListener(listener);
    }
    
    /**
     * Removes the specified WorkspaceListener from the Workspace
     * @param listener
     */
    public void removeWorkspaceListener(WorkspaceListener listener){
        workspace.removeWorkspaceListener(listener);
    }
    
    
    /**
     * Undos the most recent workspace action
     *
     */
    public void undoWorkspaceAction()
    {
    	workspace.undo();
    }
    
    /**
     * Redos the most recent workspace action
     */
    public void redoWorkspaceAction()
    {
    	workspace.redo();
    }
    
    /**
     * Notifies listeners that the given events have occurred.
     */
    public void notifyListeners(Iterable<WorkspaceEvent> events) {
        for (WorkspaceEvent e : events) {
            workspace.notifyListeners(e);
        }
    }
    
    /**
     * Resets the entire workspace.  This includes all blocks, pages, drawers, and trashed blocks.  
     * Also resets the undo/redo stack.  The language (i.e. genuses and shapes) is not reset.
     */
    public static void resetWorkspace(){
        //clear all pages and their drawers
        //clear all drawers and their content
        //clear all block and renderable block instances
        workspace.reset();
        //reset zoom
        if (zoomSlider != null) zoomSlider.reset();
        //clear runblock manager data
        rbm.reset();
        //clear the breed manager
        BreedManager.reset();
        //clear procedure output information
        ProcedureOutputManager.reset();
        //clear search bar
        //TODO make this more integrated in workspace
        searchBar.reset();
        observer.reallocateGlobalVariables(EMPTY_VARS_LIST);
        observer.reallocatePatchVariables(EMPTY_VARS_LIST);
        observer.reallocateTurtleVariables(EMPTY_VARS_LIST);
        
        // clear compiler variables
        SLBlockCompiler.getCompiler().clearAllVariables();
    }
    

    
    /**
     * This method creates and lays out the entire workspace panel with its 
     * different components.  Workspace and language data not loaded in 
     * this function.
     * Should be call only once at application startup.
     */
    private void initWorkspacePanel(){
        //workspace = loadFreshWorkspace();
        
        //create search bar
        searchBar = new SearchBar("Search blocks", "Search for blocks in the drawers and workspace", workspace);

        
        //add trashcan and prepare trashcan images
        ImageIcon tc = new ImageIcon("support/images/trash0000.png");
        ImageIcon openedtc = new ImageIcon("support/images/trash0023.png");
        
        TrashCan trash = new TrashCan(tc.getImage(), openedtc.getImage());
        workspace.addWidget(trash, true, true);
        
        //create header pane
        CButton breedButton = bm.getEditBreedsButton();
        breedButton.setPreferredSize(new Dimension (100,35));
        
        zoomSlider = new ZoomSlider();
        
        // importer taken out for version 1 release
        //DataImportManager dim = new DataImportManager();
        //CButton importer = dim.getJButton();
        //importer.setPreferredSize(new Dimension (100,35));
        
        JComponent headerPane = CHeader.makeBasicHeader(
        		workspace.getFactoryManager().getFactorySwitcher(),
        		new JComponent[] {breedButton, zoomSlider},
        		searchBar.getComponent());
        
        workspacePanel = new JPanel();
        workspacePanel.setLayout(new BorderLayout());
        workspacePanel.add(headerPane, BorderLayout.NORTH);
        workspacePanel.add(workspace, BorderLayout.CENTER);
        
        isWorkspacePanelInitialized = true;
        
        TypeBlockManager.enableTypeBlockManager(workspace.getBlockCanvas());
    }
    
    /**
     * Returns the JComponent of the entire workspace. 
     * @return the JComponent of the entire workspace. 
     */
    public JComponent getWorkspacePanel(){
        if(!isWorkspacePanelInitialized)
            initWorkspacePanel();
        //repaint the zoomslider so that the thumb is in the right place
        zoomSlider.repaint();
        return workspacePanel;
    }
    
    /**
     * This is the final step in the initialization of the Workspace and the 
     * block language in Starlogo TNG.  The initial workspace and entire
     * block language is loaded here and displayed within the workspace
     * panel.  (Should only call this after observer set and torusworld has
     * been initialized - if not, you will encourter lots of problems.)
     *
     */
    public void finishWorkspaceControllerInit(){
        workspace = loadFreshWorkspace();
        
        //add the the loaded searchable containers into the search bar
        //if it has already been created
        if(searchBar != null){
            for(SearchableContainer con : getAllSearchableContainers()){
                searchBar.addSearchableContainer(con);
            }
        }
    }
    
    /**
     * Reloads the workspace.  Assumes that the workspace has been reset and language is set.  
     *
     */
    public void reloadWorkspace(){
        isWorkspaceLoading = true;
        //System.out.println("preparing to load from slcodeblocks");
        //load the breed names and shape information 
        BreedManager.loadPageAndBreedShape(langDefRoot);
        //load the canvas blocks (if canvas is primary space) or
        //pages, pagedrawers, and their blocks (if any) from the save file 
        workspace.loadWorkspaceFrom(null, langDefRoot);
        //load dynamically generated blocks into workspace
        workspace.loadWorkspaceFrom(null, BlocksGenerator.getDynamicBlockSetDefns());
        BreedManager.finishLoad();
        refreshSearchBar();

        // show the factory
        Workspace.getInstance().getFactoryManager().viewStaticDrawers();
        
        isWorkspaceLoading = false;
    }
    
    private static void refreshSearchBar(){
        for(SearchableContainer con : getAllSearchableContainers()){
            searchBar.addSearchableContainer(con);
        }
    }
    
    /**
     * Returns an unmodifiable Iterable of SearchableContainers
     * @return an unmodifiable Iterable of SearchableContainers
     */
    public static Iterable<SearchableContainer> getAllSearchableContainers(){
        return workspace.getAllSearchableContainers();
    }
    
    /**
     * TODO MAY MOVE THIS TO BREED MANAGER
     */
    public void workspaceEventOccurred(WorkspaceEvent event) {
        //something happened in the workspace, mark changes to the observer
        if(observer !=null && !isWorkspaceLoading) {
            observer.markChanged();
        }
    }
    
    ///////////////////////////
    // SLCODEBLOCKS OBSERVER //
    ///////////////////////////
    public static void setObserver(SLBlockObserver slbo)
    {
        observer = slbo;
        SLBlockCompiler.setObserver(slbo);
        SLNetworkManager.setObserver(slbo);
        workspace.addWorkspaceListener(SLBlockCompiler.getCompiler());
    }

    public static void unsetObserver()
    {
        observer = null;
        SLBlockCompiler.setObserver(null);
        SLNetworkManager.setObserver(null);
    }

    public static SLBlockObserver getObserver()
    {
        return observer;
    }

    public static boolean hasObserver()
    {
        return observer != null;
    }
    
    ////////////////////////
    // SAVING AND LOADING //
    ////////////////////////
    
    /**
     * Returns true iff the workspace is in the process of loading information
     */
    public static boolean isWorkspaceLoading(){
        return isWorkspaceLoading;
    }
    
    /**
     * Returns the save string for the entire workspace.  This includes the block workspace, any 
     * custom factories, canvas view state and position, pages
     * @return the save string for the entire workspace.
     */
    public String getSaveString(){
        StringBuffer saveString = new StringBuffer();
        //append the title of this section of save string
        saveString.append(saveTitle);
        saveString.append("\r\n");
        //append the save data
        saveString.append("<?xml version=\"1.0\" encoding=\"UTF-16\"?>");
        saveString.append("\r\n");
        //dtd file path may not be correct...
        //saveString.append("<!DOCTYPE StarLogo-TNG SYSTEM \""+SAVE_FORMAT_DTD_FILEPATH+"\">");
        //append root node
        saveString.append("<SLCODEBLOCKS>");
        saveString.append(workspace.getSaveString());
        //save subsets
        saveString.append(SubsetManager.getSaveString());
        //save the breed name to shape mapping
        saveString.append(BreedManager.getBreedNameToShapeSaveString());
        saveString.append("</SLCODEBLOCKS>");
        return saveString.toString();
    }
    
    /**
     * Loads the entire workspace from the specified contents.  This includes: 
     * - all the live RenderableBlocks and their associated Blocks within the Canvas
     * - the pages organizing the canvas
     * - the block drawers and any custom drawers created by the user
     * - if this is the first time loading Starlogo, loads the necessary 
     *   language content too.
     * @param contents the String contents of the workspace to load
     * @param firstTime boolean flag to determine if this is the first time 
     * @param version the version of TNG that the file belongs to
     * Starlogo TNG is starting up
     */
    public void loadSaveString(String contents, boolean firstTime, double version, double currentVersion){
        
        //load language genuses and block shapes info if this is the first 
        //time starlogo tng is being loaded
        if(firstTime){
            loadSLTNGLanguage();
        }//need to have language ready for the old workspace importer to use info from
        
        //check the version number
        if(version <= OLD_LOAD_VERSION){
            contents = Importer.loadOldWorkspaceFromString(contents);
            //now clear the importer
            Importer.reset();
        }else{
            //if we support multiple workspaces need to create new workspace....
            //grab the slcodeblocks save section from contents
            int start = contents.indexOf(saveTitle);
            if (start < 0)
                return;
            int end = contents.indexOf("`", start + saveTitle.length());
            if (end < 0)
                end = contents.length();
            contents = contents.substring(start + saveTitle.length(), end);
        }
            //System.out.println("loading contents: "+contents);
            contents = contents.substring(contents.indexOf("<"));
        //}
        //extract the root element from contents
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document doc;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(new InputSource(new StringReader(contents)));
            Element root = doc.getDocumentElement();
            if (root.getNodeName().equals("SLCODEBLOCKS")){
                isWorkspaceLoading = true;
                //System.out.println("preparing to load from slcodeblocks");
                //load the breed names and shape information 
                BreedManager.loadPageAndBreedShape(root);
                //load the canvas (or pages and page blocks if any) blocks from the save file
                //also load drawers, or any custom drawers from file.  if no custom drawers
                //are present in root, then the default set of drawers is loaded from 
                //langDefRoot
                workspace.loadWorkspaceFrom(root, langDefRoot);
                //load dynamically generated blocks into workspace
                workspace.loadWorkspaceFrom(null, BlocksGenerator.getDynamicBlockSetDefns());
                //load subsets
                SubsetManager.loadBlockDrawerSubsetSets(root);
                //now update the BreedManager, RunBlockManager, and RuntimeWorkspace to match
                //the correct state of the workspace and the blocks
                BreedManager.finishLoad(); 
                //reset procedure output information
                ProcedureOutputManager.finishLoad();
                
                refreshSearchBar();

                isWorkspaceLoading = false;
                workspace.notifyListeners(new WorkspaceEvent(null, WorkspaceEvent.WORKSPACE_FINISHED_LOADING, true));
                
                // show the factory
                Workspace.getInstance().getFactoryManager().viewStaticDrawers();
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
        
        if(version < currentVersion) {
        	JOptionPane.showMessageDialog(null, "You are loading a project from an older version of StarLogo TNG." +
        			"\nThus, you may notice some changes in your block appearance and overall layout.\n" +
        			"If you run into problems, please contact: starlogo-request@mit.edu.", "Success!", JOptionPane.PLAIN_MESSAGE);
        }
    }
    
    /**
     * Loads all the properties, custom shapes, custom link rules of 
     * Starlogo TNG.
     * 
     * NOTE: Should only call this once and only more than once if a new langauge
     * is being loaded.
     */
    private void loadSLTNGLanguage(){
    	    //shapes must be loaded first
        BlockConnectorShape.loadBlockConnectorShapes(langDefRoot);
        //load genuses
        BlockGenus.loadBlockGenera(langDefRoot);
        //generate dynamic genuses       
        BlocksGenerator.addDynamicBlockSet(ShapeBlocksGenerator.getInstance());
        BlocksGenerator.addDynamicBlockSet(SoundBlocksGenerator.getInstance());
        //BlocksGenerator.addDynamicBlockSet(TerrainBlocksGenerator.getInstance());
        BlockGenus.loadBlockGenera(BlocksGenerator.getDynamicBlockSetDefns());

        BlockShape.addCustomShapes(new SLBlockShapeSet());
        
        //load custom rules
        BlockLinkChecker.addRule(new MonitorRule());
        BlockLinkChecker.addRule(new CommandRule());
        BlockLinkChecker.addRule(new SocketRule());
        BlockLinkChecker.addRule(new PolyRule());
        BlockLinkChecker.addRule(new StackRule());
        BlockLinkChecker.addRule(new ParamRule());
        BlockLinkChecker.addRule(new InfixRule());
        BlockLinkChecker.addRule(new WaitRule());
        BlockLinkChecker.addRule(new RunForSomeTimeRule());
    }
    
    /**
     * Loads the entire workspace by the lang_def file of this
     * language.  Only called once at application startup.
     */
    private Workspace loadFreshWorkspace(){
        
        //extract the root from lang def file
        if (langDefRoot.getNodeName().equals("BlockLangDef")){
            isWorkspaceLoading = true;
            loadSLTNGLanguage();
            //load fresh workspace
            BreedManager.loadPageAndBreedShape(langDefRoot); //needs workspace
            workspace.loadWorkspaceFrom(null, langDefRoot);
            //load dynamically generated blocks into workspace
            workspace.loadWorkspaceFrom(null, BlocksGenerator.getDynamicBlockSetDefns());
            workspace = Workspace.getInstance();
            BreedManager.finishLoad();
            
            // show the factory
            Workspace.getInstance().getFactoryManager().viewStaticDrawers();

            isWorkspaceLoading = false;
            return workspace;
        }
        return null;
    }
    
    /////////////////////////////////////
    // TESTING SLCODEBLOCKS SEPARATELY //
    /////////////////////////////////////
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("WorkspaceDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        int inset = 50;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setBounds(inset, inset,
                  screenSize.width  - inset*2,
                  screenSize.height - inset*2);
        
        WorkspaceController.getInstance().finishWorkspaceControllerInit();
        final Workspace ws = workspace;
        SearchBar searchBar = new SearchBar("Search blocks", "Search for blocks in the drawers and workspace", ws);
        for(SearchableContainer con : WorkspaceController.getAllSearchableContainers()){
            searchBar.addSearchableContainer(con);
        }
        
        //get trashcan images
        String trashlocation = ((System.getProperty("application.home") != null) ?
                System.getProperty("application.home") :
                    System.getProperty("user.dir")) + "/support/images/";
        
        System.out.println(trashlocation+"trash.png");
        ImageIcon tc = new ImageIcon(trashlocation+"trash0000.png");
        ImageIcon openedtc = new ImageIcon(trashlocation+"trash0023.png");
        
        TrashCan trash = new TrashCan(tc.getImage(), openedtc.getImage());
        ws.addWidget(trash, true, true);
        
        //set up header
        CButton saveButton = new CBorderlessButton("Save");
        saveButton.setPreferredSize(new Dimension (100,35));
        saveButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		System.out.println(WorkspaceController.getInstance().getSaveString());
        	}
        });
        CButton undoButton = new CBorderlessButton("Undo");
        undoButton.setPreferredSize(new Dimension (100,35));
        undoButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
                workspace.undo();
        	}
        });
        
        CButton debugger = new CBorderlessButton("Debugger");
        debugger.setPreferredSize(new Dimension (100,35));
        
        JComponent headerPane = CHeader.makeBasicHeader(
        		workspace.getFactoryManager().getFactorySwitcher(),
        		new JComponent[] {new ZoomSlider(), debugger, saveButton},
        		searchBar.getComponent());
        
        frame.add(headerPane, BorderLayout.NORTH);
        frame.add(ws, BorderLayout.CENTER);
        frame.setVisible(true);
        
        ws.notifyListeners(new WorkspaceEvent(null, WorkspaceEvent.WORKSPACE_FINISHED_LOADING, true));
        
        TypeBlockManager.enableTypeBlockManager(ws.getBlockCanvas());
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

}
