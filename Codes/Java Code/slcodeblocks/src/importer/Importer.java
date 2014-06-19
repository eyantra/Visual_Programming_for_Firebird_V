package importer;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import codeblockutil.XMLStringWriter;

import slcodeblocks.BreedManager;

/**
 * Translates the old save format to the new save format of codeblocks.
 * 
 * Warning: this class and the other classes within this package are very delicate.
 * Many names and details have been harded coded here based on information in 
 * the lang_def.xml and save_file.dtd.  Be careful when changing this class and 
 * changing the save format and specifications within the lang_def.xml.  
 * 
 * @author Ricarose Roque (ria@alum.mit.edu)
 */
public class Importer {
	
	//set to true to get output as importer loads old project file
	private static final boolean DEBUG = false;
    
    //final contents to return that will be in the new save format
    private static  XMLStringWriter contents;
    
    //contains XML mappings between breed/page names to their shape names
    private static XMLStringWriter breedToShapeMapping = null;
    
    //page data extracted from old file format
    private static ArrayList<PageData> pages = new ArrayList<PageData>();
    
    //maps the long ids and names of block species extracted from the old save format
    private static HashMap<String, SpeciesData>  speciesMap = new HashMap<String, SpeciesData>();
    
    //maps the global-variable-number block label with its runtime bounding constraints.
    //For example, say we have a global-variable-number called MyNumber attached to
    //a slider called MySlider.  Say that in teh runtime workspace, we set teh slider
    //to have a minimum of 10, a maximum of 30, and a value of 25.  THe min,max, and value
    //are what we call the bounding constraints applied onto MyNumber.
    private static HashMap<String, VariableConstraints> constraintsMap = new HashMap<String, VariableConstraints>();
    
    //an ordered set of block data extracted from string in old version
    //set is ordered by the xcor first and uses block id as a tie breaker
    private static TreeSet<BlockData> blocks = new TreeSet<BlockData>(
            new Comparator<BlockData>(){
                public int compare(BlockData bd1, BlockData bd2){
                    
                    if(bd1.getXCor() < bd2.getXCor())
                        return -1;
                    else if(bd1.getXCor() > bd2.getXCor())
                        return 1;
                    else if(bd1.getXCor() == bd2.getXCor()){
                        if(bd1.getYCor() < bd2.getYCor())
                            return -1;
                        else if(bd1.getYCor() > bd2.getYCor())
                            return 1;
                        else{
                            if(bd1.getBlockID() < bd2.getBlockID())
                                return -1;
                            else    
                                return 1;    
                        }
                    }
                    
                    System.out.println("WARNING COMPARATOR RETURN ZERO! for bd1: "+bd1+" and \n bd2: "+bd2);
                    
                    return 0;
                }
            });
    
    /**
     * Clears all the internal data from this importer.
     */
    public static void reset(){
        breedToShapeMapping = null;
        contents = null;
        
        pages.clear();
        speciesMap.clear();
        blocks.clear();
        constraintsMap.clear();
        BlockData.reset();
    }
    
    //load pages and breeds data
    /**
     * Extracts page, breed, and block data from the old save format and 
     * translates it to the new save format of codeblocks.  
     * @param str String to load the old workspace from
     * @return String formatted in the new save format for slcodeblocks to load
     */
    public static String loadOldWorkspaceFromString(String str) {
    	contents = new XMLStringWriter(0);
        contents.beginXMLString("SLCODEBLOCKS");
        
        //load pages
        loadPagesFromString(str);
        
        //load block species
        loadSpeciesFromString(str);
        
        //load constraints
        loadConstraintsFromString(str);
        
        //load blocks
        loadBlocksFromString(str);
        //oraganize blocks according to page and translate their 
        //coordinates to the right page
        organizeBlocksAndPages();
        
        //get xml by iterating through pages
        constructXML();
        
        contents.endXMLString();
        
        if(DEBUG)
        	System.out.println(contents);
        
        return contents.toString();
    }
    
    private static void constructXML(){
        contents.beginElement("Pages", false);
        
        for(PageData pd : pages){
            contents.beginElement("Page", true);
            contents.addAttribute("page-name", pd.getPageName());
            contents.addAttribute("page-width", String.valueOf(pd.getPageWidth()));
            contents.addAttribute("page-drawer", pd.getPageName());
            contents.endAttributes();
            
            //add blocks
            if(pd.getBlockData().size() > 0){
                contents.beginElement("PageBlocks", false);
                for(BlockData bd : pd.getBlockData()){
                   bd.appendSaveString(contents,  pd.getPageName());
                }
                contents.endElement("PageBlocks");
            }
            
            contents.endElement("Page");
        }
        
        contents.endElement("Pages");
        
        appendBreedToShapeMapping(contents);
    }
    
    private static void handleCommentBlock(BlockData data){
    	//TODO old comment block loading:
    	//right now comment blocks are being ignored
    	//however in the future they should be incorporated into a comment post it
    	BlockData before = BlockData.getBlockData(data.getBeforeId());
        BlockData after = BlockData.getBlockData(data.getAfterId());
        if(before != null){
        	if(after != null)
        		before.setAfterId(after.getBlockID());
        	else 
        		before.setAfterId(-1l);
        }
        if(after != null ){
        	if(before != null)
        		after.setBeforeId(before.getBlockID());
        	else 
        		after.setBeforeId(-1l);
        }
    	
    }
    
    private static void organizeBlocksAndPages(){
        int pageIndex = 0;
        int pageOffset = 0;
        
        PageData pd = pages.get(pageIndex);
        
        boolean pagedone = false;
        
        Iterator<BlockData> blocksIter = blocks.iterator();
        //going to save all the blocks that begin stacks for second
        //iteration of organization where we make sure all the blocks 
        //are really in their correct pages
        ArrayList<BlockData> topLevelBlocks = new ArrayList<BlockData>();
        
 
        BlockData data = null;
        while(blocksIter.hasNext()){
            if(data == null){  //added the data to a page, now get the next block
                data = blocksIter.next();
                //check if comment block, we want to ignore those
                if(data.getGenusName().equals(BlockData.commentGenusString)){
                    handleCommentBlock(data);
                    data = null;
                    continue;
                }
                
                //tell block data to properly connect its poly connectors if it has any
                if(data.hasPolyConnector())
                    data.updatePolyConnectors();
                    
                if(!data.hasPlugEquivalent())
                    topLevelBlocks.add(data);
                
                
                
                if(DEBUG)
                	System.out.println("Trying to add block to a page: "+data);
            }
            assert !pagedone : "Done iterating pages but some blocks without page: "+data;
            if(data.getXCor() >= pageOffset && data.getXCor() < (pageOffset + pd.getPageWidth())){

                    data.translateCoorsToPage(pageOffset, pd.getPageWidth());
                    pd.addBlockData(data);
                if(DEBUG)
                	System.out.println("adding to page: "+pd.getPageName()+": "+data);
                data = null; //reset data object
            }else{
                pageIndex++;
                if(pageIndex >= pages.size())
                    pagedone = true;
                else{
                    pd = pages.get(pageIndex);
                    pageOffset += pages.get(pageIndex-1).getPageWidth();
                }
            }
        }
        
        //we iterate over again to make sure that blocks really are in the 
        //right pages.  some blocks may have extended into the next page 
        //maybe because their inside infix blocks that extended out.  or 
        //their stack may have started on one page and some of the blocks
        //because of center alignment may be in another page while other blocks
        //in the stack land in the page before
        //another note: dont need to update the locations of blocks from other
        //pages because all their locations are relative to their respective pages
        blocksIter = topLevelBlocks.iterator();
        data = null;
        while(blocksIter.hasNext()){
            data = blocksIter.next();
            consolidateStackPage(data, data.getPageOf());
        }
        
    }
    
    private static void consolidateStackPage(BlockData bd, PageData pd){ 
        
        //make sure that bd has pd as its page, otherwise update
        if(!bd.getPageOf().equals(pd)){
            pd.setPageWidth(pd.getInitPageWidth()+bd.getXCor()+10);
            bd.setXCor(pd.getInitPageWidth() + bd.getXCor());
            bd.getPageOf().removeBlockData(bd);
            pd.addBlockData(bd);
        }
        
        //make sure socket blocks are the same page as this
        if(bd.getSocketData() != null && bd.getSocketData().size() > 0){
            for(ConnectorData cd : bd.getSocketData()){
                if(cd.getConnId() > -1){
                    consolidateStackPage(BlockData.getBlockData(cd.getConnId()), pd);
                }
            }
        }
        
        //make sure after block is the same page as this
        if(bd.getAfterBlock() != null){
            consolidateStackPage(bd.getAfterBlock(), pd);
        }
        
    }
    
    private static void loadPagesFromString(String str){
        String title = "`workspace`";
        int start = str.indexOf(title);
        if (start < 0)
            return ;
        int end = str.indexOf("`", start + title.length());
        if (end < 0)
            end = str.length();
        str = str.substring(start + title.length(), end);
        
        Tokenizer tokenizer = new Tokenizer(str);
        ArrayList<String> items = tokenizer.tokenize();
        removeQuotes(items);
        
        //load pages
        loadPagesFromList(items);
    }
    
    private static void loadPagesFromList(ArrayList<String> items){
        int i = 0;
        while (i<items.size()) {
            String item = items.get(i);

            if (item.equals("number-of-pages")) {
                //String value = items.get(i+1);
                //numPages = Integer.parseInt(value);
                i += 2;
                continue;
            }
        
            if (item.equals("page")) {
                String value = items.get(i+1); //value is the page name
                i += 2;

                int pageWidth = -1;
                String shapeName = "";
                //however the page name may have more than one word
                while (!items.get(i).equals("page-width")) {
                    value += " " + items.get(i);
                    i++;
                }

                item = items.get(i);
                if (item.equals("page-width")) {
                    pageWidth = Integer.parseInt(items.get(i+1));
                    i += 2;
                }
 
                if (i < items.size())
                    item = items.get(i);
                if(item.equals("shape"))    {
                    shapeName = items.get(i + 1);
                    
                    i += 2;
                }
                
                if(DEBUG)
                	System.out.println("loaded page: "+value+" of: "+shapeName+" width: "+pageWidth);
                //update the breed to shape mapping if page name is a valid breed name
                if(BreedManager.isValidBreedName(value) && shapeName != "")
                    addBreedToShapeMapping(value, shapeName);
                pages.add(new PageData(value, pageWidth));
                
                continue;
                
            }
            // skip the next if we haven't found anything we know
            i+=2;
        }
    }
        
    /**
     * Adds the specified breedName and breedShape to the breed to shape mapping. 
     * 
     * The breedName should be the same as the page name.
     * @param breedName
     * @param breedShape
     */
    private static void addBreedToShapeMapping(String breedName, String breedShape){
        if(breedToShapeMapping == null){
            breedToShapeMapping = new XMLStringWriter(4);
            breedToShapeMapping.beginXMLString("BreedShapeMappings");
        }
        
        breedToShapeMapping.beginElement("BtoSMapping", false);
        breedToShapeMapping.addDataElement("BreedName", breedName);
        breedToShapeMapping.addDataElement("BreedShape", breedShape);
        breedToShapeMapping.endElement("BtoSMapping");
    }
    
    /**
     * Appends the XML String of the BreedToShapeMapping to the specified contents
     * @param contents
     */
    private static void appendBreedToShapeMapping(XMLStringWriter writer){
        if(breedToShapeMapping != null){
            breedToShapeMapping.endXMLString();
            writer.appendXMLWriterString(breedToShapeMapping);
        }
    }

    //load species
    private static void loadSpeciesFromString(String str){
        LineNumberReader lnr = new LineNumberReader(new StringReader(str));
        
        try {
            String thing = lnr.readLine();
            while (thing != null) {
                //System.out.println("read line: " + thing);
                String thingtrim = thing.trim();
                if (!thingtrim.startsWith("BlockSpecies")) {
                    thing = lnr.readLine();
                    continue;
                }
                
                StringBuffer buf = new StringBuffer();
                do {
                    buf.append(thingtrim + "\r\n");
                    thing = lnr.readLine();
                    if (thing == null)
                        break;
                    thingtrim = thing.trim();
                } while (!thingtrim.startsWith("BlockSpecies") && !thingtrim.startsWith("`"));
                
                Tokenizer tokenizer = new Tokenizer(buf.toString());
                ArrayList<String> items = tokenizer.tokenize();
                removeQuotes(items);
                
                loadSpeciesFromArray(items);
                
                if(thingtrim.startsWith("`")) //we've reached the end of the species list
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception f) {
            f.printStackTrace();
        }
    }
    
    /**
     * Extract socket labels, socket/plug kind. 
     * Translate some genuses that are different from the codeblocks version, like variables.
     * Ignoring default arguments since that was specified in the lang def file.
     * @param items
     */
    private static void loadSpeciesFromArray(ArrayList<String> items) {
        String genus = null;
        Long id = 0l;
        String name = null;
        //String breed = null;
        String plugKind = null;
        String[] socketLabels = new String[0];
        String[] socketKinds = new String[0];
        //boolean[] isSocketInlineProc = new boolean[0];
        //boolean areSocketsInlineProcs = false;
        
        //int plugType = -1;
        //ArgumentDescriptor[] defaultArgs = new ArgumentDescriptor[0];
        
        int i = 0;
        while (i < items.size()) {
            String item = items.get(i);
            
            if (item.equals("BlockSpecies")) {
                name = items.get(i + 1);
                //System.out.println("loading species " + name);
                i += 2;
                continue;
            }
            
            if (item.equals("genus")) {
                genus = items.get(i + 1);
                i += 2;
                continue;
            }
            
            if (item.equals("breed")) {
                //breed = items.get(i + 1);
                i += 2;
                continue;
            }
            
            if (item.equals("id")) {
                String value = items.get(i + 1);
                id = Long.valueOf(value);
                i += 2;
                continue;
            }
            
            if (item.equals("plug-kind")) {
                plugKind = items.get(i + 1);
                //TODO plugType = BlockGenus.getKindForKindName(value);
                i += 2;
                continue;
            }
            
            if (item.equals("sockets")) {
                String value = items.get(i + 1);
                int numSockets = Integer.parseInt(value);
                i += 2;
                
                socketLabels = new String[numSockets];
                socketKinds = new String[numSockets];
                //isSocketInlineProc = new boolean[numSockets];
                //defaultArgs = new ArgumentDescriptor[numSockets];
                
                for (int currentSocket = 0;
                currentSocket < numSockets;
                currentSocket++) {
                    
                    String label = items.get(i);
                    assert(label.equals("socket-label"));
                    socketLabels[currentSocket] = items.get(i + 1);
                    
                    label = items.get(i + 2);
                    assert(label.equals("socket-kind"));
                    socketKinds[currentSocket] =
                                items.get(i + 3);
                    i += 4;
                    
                    if(i < items.size())
                    {
                        String nextToken = items.get(i);
                        
                        if(nextToken.equals("inline-proc"))
                        {
                            //isSocketInlineProc[currentSocket] = true;
                            //areSocketsInlineProcs = true;
                            
                            i++;
                        }
                    }
                }
                continue;
            }
            
            if (item.equals("default-arguments")) {
                String value = items.get(i + 1);
                int numDefaultArgs = Integer.parseInt(value);
                i += 2;
                
                int currentSocket;
                for(currentSocket = 0; currentSocket < numDefaultArgs; currentSocket++)
                {
                    String label = items.get(i);
                    assert(label.equals("species-name"));
                    String argSpeciesName = items.get(i + 1);
                    
                    if (!argSpeciesName.equals("null")) {

                        label = items.get(i + 2);
                        assert(label.equals("kind"));
                      /*TODO   int argKind =
                            BlockGenus.getKindForKindName(
                                    items.get(i + 3));*/
                        
                        label = items.get(i + 4);
                        assert(label.equals("initial-text"));
//                        String argInitText = items.get(i + 5);    // XXX not read
                        i += 6;
                        
//                        String argInitCommand = argInitText;
                        
                        if((i < items.size()) && (items.get(i).equals("initial-command")))
                        {
//                            argInitCommand = items.get(i + 1);
                            i += 2;
                        }
                        
                        /* TODO defaultArgs[currentSocket] =
                            new ArgumentDescriptor(
                                    argSpeciesName,
                                    argKind,
                                    argInitText,
                                    argInitCommand);*/
                       // if(name.equals("runforsometime"))
                            //System.out.println(name+" def arg at "+currentSocket+" out of "+numDefaultArgs+" " +defaultArgs[currentSocket].getSaveString());
                    } else {
                        i += 2;
                        
                        //TODO defaultArgs[currentSocket] = null;
                        
                    }
                }
                /*TODO for( ; currentSocket < socketNames.length; currentSocket++)
                    defaultArgs[currentSocket] = null;*/
                
                continue;
            }
            
            if(item.equals("initial-label"))
            {
                //don't care about initialLabel
                //stored in BlockGenus
                //initialLabel = items.get(i + 1);
                
                i += 2;
                
                continue;
            }
            
            if(item.equals("initial-command"))
            {
                //don't care about initial command
                //stored in block genus
                //initialCommand = items.get(i + 1);
                
                i += 2;
                
                continue;
            }
            
            // skip the next if we haven't found anything we know
            i += 2;
        }
        
        assert(name != null);
        assert(genus != null);
        assert(id.longValue() >= 0l);
        
        
        speciesMap.put(id+name, new SpeciesData(name, plugKind, socketKinds, socketLabels));
        
        if(DEBUG)
        	System.out.println("Loaded species: "+id+":  "+speciesMap.get(id+name));
    }
    
    
    //load blocks
    /**
     * Loads the workspace blocks from the specified string
     * @param str String to load the workspace blocks from
     */
    private static void loadBlocksFromString(String str) {
        String title = "`blocks`";
        int start = str.indexOf(title);
        if (start < 0)
            return;
        int end = str.indexOf("`", start + title.length());
        if (end < 0)
            end = str.length();
        str = str.substring(start + title.length(), end);
        
        Tokenizer tokenizer = new Tokenizer(str);
        ArrayList<String> items = tokenizer.tokenize();
        removeQuotes(items);
        
        //HashMap<Long, Block> blockMap = new HashMap<Long, Block>(100);
        // maps id to block for later connections
        
        LineNumberReader lnr = new LineNumberReader(new StringReader(str));
        
        try {
            String thing = lnr.readLine();
            while (thing != null) {
                //System.out.println("read line: " + thing);
                String thingtrim = thing.trim();
                if (!thingtrim.startsWith("Block")) {
                    thing = lnr.readLine();
                    continue;
                }
                
                StringBuffer buf = new StringBuffer();
                thingtrim = "";
                do {
                    buf.append(thingtrim + "\r\n");
                    thing = lnr.readLine();
                    if (thing == null)
                        break;
                    thingtrim = thing.trim();
                } while (!thingtrim.startsWith("Block") && !thingtrim.startsWith("`"));
                
                loadBlockFromString(buf.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception f) {
            f.printStackTrace();
        }
    }
    
    private static void loadBlockFromString(String str) {
        Tokenizer tokenizer1 = new Tokenizer(str);
        ArrayList<String> items1 = tokenizer1.tokenize();
        removeQuotes(items1);
        
        loadBlockFromArray(items1, 0, items1.size());
        
    }

    private static void loadBlockFromArray(ArrayList<String> items, int start, int end) {
        //System.out.println("Load block " + start + " " + end);
        int id = -1;
        String speciesName = null;
        Long speciesID = new Long(0);
        String label = null;
        //String command = null;
        boolean hasBreed = false;
        String breed = null;
        double xcor = 0.0, ycor = 0.0;
        int numSockets = 0;
        int[] socketBlockIDs = new int[0];
        int plugID = -1;
        int controlNextID = -1, controlPrevID = -1;
        String shapeName = null;
        boolean procedureShown = true;
        /*int correspondingParameterID = -1;*/
        boolean isBad = false;
//        boolean isPermanentlyBad = false;

        int i = start;
        while (i<end) {
            String item = items.get(i).toString();

            if (item.equals("species-name")) {
                speciesName = items.get(i+1).toString();
                i += 2;
                //System.out.println("Species name " + speciesName);
                continue;
            }

            if (item.equals("id")) {
                String value = items.get(i+1).toString();
                id = Integer.parseInt(value);
                i += 2;
                //System.out.println("block id " + id);
                continue;
            }

            if (item.equals("species-id")) {
                String value = items.get(i+1).toString();
                speciesID = Long.valueOf(value);
                i += 2;
                //System.out.println("Species id " + speciesID);
                continue;
            }

            if (item.equals("label")) {
                label = items.get(i+1).toString();
                i += 2;
                //System.out.println("label " + label);
                continue;
            }

            if (item.equals("command")) {
                //command = items.get(i+1).toString();
                i += 2;
                //System.out.println("command " + command);
                continue;
            }

            if (item.equals("has-breed")) {
                String value = items.get(i+1).toString();
                hasBreed = (value.equals("true"));
                i += 2;
                //System.out.println("has breed " + hasBreed);
                continue;
            }

            if (item.equals("breed")) {
                breed = items.get(i+1).toString();
                i += 2;
                //System.out.println("breed " + breed);
                continue;
            }

            if (item.equals("xcor")) {
                String value = items.get(i+1).toString();
                xcor = Double.parseDouble(value);
                i += 2;
                //System.out.println("xcor " + xcor);
                continue;
            }

            if (item.equals("ycor")) {
                String value = items.get(i+1).toString();
                ycor = Double.parseDouble(value);
                i += 2;
                //System.out.println("ycor " + ycor);
                continue;
            }
            
            if (item.equals("is-bad")) {
                String value = items.get(i+1).toString();
                isBad = value.equals("true");
                i += 2;
                //System.out.println("has breed " + hasBreed);
                continue;
            }
            
            if (item.equals("is-bad")) {
//                String value = items.get(i+1).toString();
//                isPermanentlyBad = value.equals("true");
                i += 2;
                //System.out.println("has breed " + hasBreed);
                continue;
            }

            if (item.equals("plug")) {
                String value = items.get(i+1).toString();
                plugID = Integer.parseInt(value);
                i += 2;
                //System.out.println("plug " + plugID);
                continue;
            }

            if (item.equals("control-prev")) {
                String value = items.get(i+1).toString();
                controlPrevID = Integer.parseInt(value);
                i += 2;
                //System.out.println("control prev " + controlPrevID);
                continue;
            }

            if (item.equals("control-next")) {
                String value = items.get(i+1).toString();
                controlNextID = Integer.parseInt(value);
                i += 2;
                //System.out.println("control next " + controlNextID);
                continue;
            }

            if (item.equals("sockets")) {
                String value = items.get(i+1).toString();
                //System.out.println("found sockets " + value);
                numSockets = Integer.parseInt(value);
                i += 2;

                socketBlockIDs = new int[numSockets];

                for(int currentSocket = 0; currentSocket < numSockets; currentSocket++) {
                    String sockLabel = items.get(i).toString();
                    assert(sockLabel.equals("socket"));
                    socketBlockIDs[currentSocket] = Integer.parseInt(items.get(i+1).toString());
                    //System.out.println("socket " + currentSocket + " block id " + socketBlockIDs[currentSocket]);
                    i += 2;
                }
                continue;
            }

            if(item.equals("shape-icon")) {
                shapeName = items.get(i + 1).toString();
                i += 2;
                //System.out.println("shape-icon = " + value);
                //shape = AvailableShapes.getShape(value);
                continue;
            }

            if (item.equals("procedure-shown")) {
                String value = items.get(i+1).toString();
                i += 2;
                //System.out.println("procedure-shown " + value);
                procedureShown = value.equalsIgnoreCase("true");
                //this is the isMinimized value for procedures
                continue;
            }
        
            if (item.equals("parent")) {
                //String value = items.get(i+1).toString();
                i += 2;
                //correspondingParameterID = Integer.parseInt(value);
                //System.out.println("parentID " + value);
                continue;
            }

            // skip the next if we haven't found anything we know
            //System.out.println("unknown tag " + items.get(i).toString());
            i+=2;
        }
    
        assert(id >= 0);
        //assert(speciesName != null);
        assert(speciesID >= 0);
        assert( (hasBreed && breed != null) || !hasBreed);
        
        //apply constraints if any
        VariableConstraints constraint = null;
        if(speciesName.equals("global-var-decl-num") && (constraintsMap.containsKey(label))){
        	constraint = constraintsMap.get(label);
        }
        
        //instantiate block data
        BlockData bd = new BlockData(id, speciesName, speciesMap.get(speciesID+speciesName), 
                label, xcor, ycor, socketBlockIDs, hasBreed, 
                breed, plugID, controlNextID, controlPrevID, isBad, procedureShown, shapeName, constraint);
        
        //for run blocks: forever, runonce, runforsometime, set their labels to be
        //the breeds
        if(bd.getGenusName().equals("forever") || bd.getGenusName().equals("runonce") ||
                bd.getGenusName().equals("runforsometime")){
            bd.setBreedLabels(pages);
        }
        
        //check if block has negative coors, if so translate the coors
        if(bd.getXCor() < 0)
        	bd.setXCor(-bd.getXCor());
        if(bd.getYCor() < 0)
        	bd.setYCor(-bd.getYCor());
        
       blocks.add(bd);
       
       if(DEBUG) 
    	   System.out.println("Loaded: "+bd);
       
       
        
    }
    
    /**
     * Set up the constraint mapping by going into the section of
     * the save string under 'runtime'.
     * @param str
     */
    private static void loadConstraintsFromString(String str){
    	String title = "`runtime`";
        int start = str.indexOf(title);
        if (start < 0)
            return;
        int end = str.indexOf("`", start + title.length());
        if (end < 0)
            end = str.length();
        str = str.substring(start + title.length(), end);
        
        Tokenizer tokenizer = new Tokenizer(str);
        ArrayList<String> items = tokenizer.tokenize();
        removeQuotes(items);
               
        LineNumberReader lnr = new LineNumberReader(new StringReader(str));
        
        try {
            String thing = lnr.readLine();
            while (thing != null) {
                String thingtrim = thing.trim();
                if (!thingtrim.startsWith("Runtime-Block")) {
                    thing = lnr.readLine();
                    continue;
                }
                
                StringBuffer buf = new StringBuffer();
                thingtrim = "";
                do {
                    buf.append(thingtrim + "\r\n");
                    thing = lnr.readLine();
                    if (thing == null)
                        break;
                    thingtrim = thing.trim();
                } while (!thingtrim.startsWith("Runtime-Block") && !thingtrim.startsWith("`"));
                
                loadConstraintFromString(buf.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception f) {
            f.printStackTrace();
        }
    }
    
    /**
     * for each "Runtime-Block", create a VariableConstraint
     * if the BLOCK SAVE-STRING DEFINES ONE.
     * @param str
     */
    private static void loadConstraintFromString(String str) {
        Tokenizer tokenizer1 = new Tokenizer(str);
        ArrayList<String> items1 = tokenizer1.tokenize();
        removeQuotes(items1);
        loadConstraintFromArray(items1, 0, items1.size());
    }
    private static void loadConstraintFromArray(ArrayList<String> items, int start, int end) {
    	String label=null, min=null, max=null, value=null;
        int i = start;
        while (i<end) {
            String item = items.get(i).toString();
            if (item.equals("label")) {
            	label = items.get(i+1).toString();
            }else if (item.equals("min")) {
                min = items.get(i+1).toString();
            }else if (item.equals("max")) {
            	max = items.get(i+1).toString();
            }else if (item.equals("current")) {
                value = items.get(i+1).toString();
            }
            i+=2; //skip next two and continue
        }
        
        if(label != null && min != null && max != null && value != null){
	        VariableConstraints vc = new VariableConstraints(label, min, max, value);
	        constraintsMap.put(label, vc);
        }
    }
    
    private static void removeQuotes(ArrayList<String> v) {
        for (int i=0; i<v.size(); i++) {
            if (v.get(i) != null) {
            String str = removeQuotes(v.get(i));
            v.set(i, str);
            }
        }
    }
    
    private static String removeQuotes(String str) {
        if (str.startsWith("\"")&&str.endsWith("\"")) {
            return str.substring(1, str.length()-1);
        } else return str;
        }
}
/**maps the global-variable-number block label 
 * with its runtime bounding constraints. For 
 * example, say we have a global-variable-number 
 * called MyNumber attached to a slider called 
 * MySlider.  Say that in teh runtime workspace, 
 * we set teh sliderto have a minimum of 10, a 
 * maximum of 30, and a value of 25.  THe min,max, 
 * and value are what we call the bounding 
 * constraints applied onto MyNumber.*/
class VariableConstraints {
	String variableLabel = null;
	String min = null;
	String max = null;
	String value = null;
	VariableConstraints(String l, String min, String max, String value){
		this.variableLabel=l;
		this.min=min;
		this.max=max;
		this.value=value;
	}
}
