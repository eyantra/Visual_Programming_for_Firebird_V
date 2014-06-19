package importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import codeblocks.BlockConnector;
import codeblocks.BlockGenus;
import codeblockutil.XMLStringWriter;
import slcodeblocks.BreedManager;

public class BlockData {

    //we need to remove this string from create-agents and create-and-do 
    //count and scatter breed bocks
    private final static String createString = "Create ";
    private final static String countTurtlesString = "count ";
    private final static String scatterBreedsString = "scatter ";
    private final static String withString = " with";
    private final static String productString = "x";
    protected final static String commentGenusString = "comment";
    private final static String polyString = "poly";
    
    //mapping of all block data from block id to block data instance.
    //needed for other block data to access information of blocks it is connected to
    private static HashMap<Long, BlockData> allBlockData = new HashMap<Long, BlockData>();
    
    private long blockID;
    private SpeciesData species = null;
    private String speciesName;
    //genusName will be the new and true genus name of this block
    private String genusName;
    
    private String label;
    
    private boolean hasBreed;
    private String breed;
    
    private int xcor;
    private int ycor;
    
    private long plugId = -1;
    private long afterId = -1;
    private long beforeId = -1;
    private int[] socketBlockIds;
    private boolean hasPolyConnector = false;
    
    //these will only be used if species == null
    private ConnectorData plug;
    private ArrayList<ConnectorData> sockets;
    
//    private boolean isBad;        // not used atm
    private boolean isMinimized;
    
    //for stubs only
    private String parentName;
    private String parentGenus;
    
    private String shapeName;
    
    //infix genus set
    private static HashSet<String> infixSet = null;
    
    //the page of this block data
    private PageData page;
    
    //the constraint (min, max, value) of this block if it has any
    VariableConstraints constraint = null;
    
    /**
     * Constructs a new BlockData object.  This object assumes that the information 
     * being passed to it is from the old save file.  The specified species SpeciesData 
     * parameter was extracted from the file but contains translated information
     * @param id
     * @param speciesName
     * @param species
     * @param label
     * @param xcor
     * @param ycor
     * @param socketBlockIds
     * @param hasBreed
     * @param breed
     * @param plugId
     * @param afterId
     * @param beforeId
     * @param isBad
     * @param isMinimized
     * @param shapeName
     * @parem constraint
     */
    public BlockData(long id, String speciesName, SpeciesData species, String label, double xcor, double ycor, 
            int[] socketBlockIds, boolean hasBreed, String breed, long plugId, long afterId, 
            long beforeId, boolean isBad, boolean isMinimized, String shapeName, VariableConstraints constraint){
        
        if(infixSet == null)
            initInfixGenusesSet();
        
        this.blockID = id;
        
        if(species != null){
            this.species = species;
        }
        
        this.speciesName = speciesName;
        
        this.label = label;
        this.xcor = (int)Math.round(xcor);
        this.ycor = (int)Math.round(ycor);
        
        this.socketBlockIds = socketBlockIds;
        this.plugId = plugId;
        this.afterId = afterId;
        this.beforeId = beforeId;
        
        this.hasBreed = hasBreed;
        this.breed = breed;
        
//        this.isBad = isBad;   // not used atm
        this.isMinimized = !isMinimized;
        
        this.shapeName = shapeName;
        
        this.constraint = constraint;

        translateGenus();
        //just stop, dont make anymore changes to this data since its a comment
        //we're going to ignore it
        if(!genusName.equals(commentGenusString)){
            organizePlugsAndSockets();
            translateLabel();//make sure that it will be valid in the new system
            //new system recognizes int locations only
        }
        //add to the mapping of all block data
        allBlockData.put(Long.valueOf(this.blockID), this);
    }    
    /**
     * Clears all internal static information
     */
    public static void reset(){
        allBlockData.clear();
    }
    
    public static BlockData getBlockData(long id){
        return allBlockData.get(Long.valueOf(id));
    }
    
    
    /**
     * Adds all the infix genuses to the set.
     */
    private static void initInfixGenusesSet(){
        infixSet = new HashSet<String>();
        
        infixSet.add("sum");
        infixSet.add("difference");
        infixSet.add("product");
        infixSet.add("quotient");
        infixSet.add("lessthan");
        infixSet.add("equals");
        infixSet.add("not-equals");
        infixSet.add("greaterthan");
        infixSet.add("lessthanorequalto");
        infixSet.add("greaterthanorequalto");
        infixSet.add("min");
        infixSet.add("max");
        infixSet.add("power");
        infixSet.add("remainder");
        infixSet.add("and");
        infixSet.add("or");
        infixSet.add("string-append");
    }
    /**
     * Determines if the specified genus is an infix genus.
     * @param genus
     * @return true if the specified genus is an infix genus.
     */
    private boolean isInfix(String genus){
        return infixSet.contains(genus);
    }
    
    /**
     * Returns true iff this block is a stub.
     */
    public boolean isBlockStub(){
        if(species != null)
            return species.isBlockStub();
        else 
            return false;
    }
    
    /**
     * Labels that have a "<" will make xml unhappy.  Other characters like ">" and "&" also.
     * Must translate them to valid xml characters.
     * 
     * Also translate labels that have set prefixes like breed blocks
     */
    private void translateLabel(){
        if(label.contains("<")){
            label = label.replace("<", "&lt;");
        }else if(label.contains(">")){
            label = label.replace(">", "&gt;");
        }else if(label.contains("&")){
            label = label.replace("&", "&amp;");
        }else if(label.contains("'")){
            label = label.replace("'", "&apos;");
        }else if(label.contains("\"")){
            label = label.replace("\"", "&quot;");
        }
        
        if(genusName.equals("create-agents") || genusName.equals("create-and-do")){
            label = label.substring(createString.length());
        }else if(genusName.equals("count-breeds")){
            label = label.substring(countTurtlesString.length());
        }else if(genusName.equals("count-breeds-with")){
            label = label.substring(countTurtlesString.length(), label.length() - withString.length());
        }else if(genusName.equals("scatter-breeds")){
            label = label.substring(scatterBreedsString.length());
        }else if(genusName.equals("product")){
            label = productString;
        }else if(this.speciesName.equals("ashape")){
            label = "";
        }
    }
    
    /**
     * Finalizes the genus name using both block and species data.
     * Will also finalize parent genus and name info for stubs.
     */
    private void translateGenus(){
        if(species == null){
            //number1, number10 --> number, and their label is value
            
            if(this.speciesName.contains("number") && !this.speciesName.contains("param"))
                this.genusName = "number";
            else if(this.speciesName.equals("less-than"))
                this.genusName = "lessthan";
            else if(this.speciesName.equals("greater-than"))
                this.genusName = "greaterthan";
            else if(this.speciesName.equals("less-than-or-equal-to"))
                this.genusName = "lessthanorequalto";
            else if(this.speciesName.equals("greater-than-or-equal-to"))
                this.genusName = "greaterthanorequalto";
            else if(this.speciesName.equals("crt"))
                this.genusName = "create-agents";
            else if(this.speciesName.equals("make-slider"))
                this.genusName = "slider";
            else if(this.speciesName.equals("ashape")){
                this.genusName = shapeName;
            }else if(SpeciesData.translateVarDeclSpeciesNameToGenus(this.speciesName) != null){
            	//for some reason there are projects that contain var decls blocks
            	//but do not have a corresponding species in the project file 
            	//for it, and so this method gets the translated genus that corresponds 
            	//to this species name in the new version
            	this.genusName = SpeciesData.translateVarDeclSpeciesNameToGenus(this.speciesName);
            }
            else{ 
                this.genusName = this.speciesName;
            }
        }else{
            if(species.isBlockStub()){
                if(!species.getGenusName().startsWith("caller")){
                    //use breed to determine the scope of this block
                    assert this.hasBreed : "Block is a stub but does not have breed: "+label+" species: "+species;
	                if(species.origSpeciesName().contains("param")){    
	                	if(species.origSpeciesName().contains("number") || 
	                			species.origSpeciesName().contains("inc")){
	                        this.parentGenus = "proc-param-number";
	                    }else if(species.origSpeciesName().contains("boolean")){
	                        this.parentGenus = "proc-param-boolean";
	                    }else if(species.origSpeciesName().contains("string")){
	                        this.parentGenus = "proc-param-string";
	                    }else if(species.origSpeciesName().contains("list")){
	                        this.parentGenus = "proc-param-list";
	                    }
	                	
	                	this.genusName = species.getGenusName();
	                }else if(BreedManager.isValidBreedName(breed) && !breed.equals("Globals")){
	                        //agent var
	                        if(species.origSpeciesName().contains("number") ||
	                                species.origSpeciesName().contains("inc")){
	                            this.parentGenus = "agent-var-number";
	                            this.genusName = species.getGenusName() + parentGenus;
	                        }else if(species.origSpeciesName().contains("boolean")){
	                            this.parentGenus = "agent-var-boolean";
	                            this.genusName = species.getGenusName() + parentGenus;
	                        }else if(species.origSpeciesName().contains("string")){
	                            this.parentGenus = "agent-var-string";
	                            this.genusName = species.getGenusName() + parentGenus;
	                        }
	                    }else if(breed.equals("Globals")){
	                        //global var
	                        if(species.origSpeciesName().contains("number") ||
	                                species.origSpeciesName().contains("inc")){
	                            this.parentGenus = "global-var-number";
	                            this.genusName = species.getGenusName() + parentGenus;
	                        }else if(species.origSpeciesName().contains("boolean")){
	                            this.parentGenus = "global-var-boolean";
	                            this.genusName = species.getGenusName() + parentGenus;
	                        }else if(species.origSpeciesName().contains("string")){
	                            this.parentGenus = "global-var-string";
	                            this.genusName = species.getGenusName() + parentGenus;
	                        }
	                    }//TODO patch var
	                
                }else{ //this is a caller block
                    this.parentGenus = "procedure";
                    this.genusName = species.getGenusName();
                }
                
                this.parentName = label;  //don't need to worry about prefixes and suffixes
            }else{
                this.genusName = species.getGenusName();
            }
        }
        
        assert genusName != null : "Genus name null with species: "+species;
        
        //comments do not exist in new system
        if(!genusName.equals(commentGenusString))
            assert BlockGenus.getGenusWithName(genusName) != null : "No Genus exists of this: "+this;
    }
    
    /**
     * Finalizes the plug and socket data by using information from both the 
     * block and the species.  
     * Assumes that the genus has already been finalized as it will use genus 
     * information to fill up the connector data of blocks without species.
     */
    private void organizePlugsAndSockets(){
        if(species != null){
            //extract plug and socket info from species
            
            
            if(species.getSockets().size() > 0){
                sockets = new ArrayList<ConnectorData>();
                for(int i=0; i<species.getSockets().size(); i++){
                   /* species.getSockets().get(i).setConnId(socketBlockIds[i]);
                    if(species.getSockets().get(i).getPosType().equals("mirror")){
                        species.setPlug(species.getSockets().get(i));
                    }*/

                    ConnectorData cd = species.getSockets().get(i);
                    if(!cd.getPosType().equals("mirror")){
                        sockets.add(new ConnectorData(cd.getKind(), cd.getInitKind(), cd.getLabel(), cd.getPosType(), socketBlockIds[i]));
                    }else{
                        if(plugId > -1)
                            plug = new ConnectorData(cd.getKind(), cd.getInitKind(), cd.getLabel(), cd.getPosType(), plugId);
                        else
                            plug = new ConnectorData(cd.getKind(), cd.getInitKind(), cd.getLabel(), cd.getPosType(), socketBlockIds[i]);
                    }
                    if(cd.getKind().equals(polyString))
                        this.hasPolyConnector=true;
                }
                
                //species.getSockets().remove(species.getPlug());
            }
            //for mirror sockets move them to plug to become the 
            //mirrored plug that codeblocks understands
            
            //in the old format, numberRight is not given the connId, but the plug
            if(species.getPlug() != null && plug == null){
                //plug.setConnId(plugId);
                plug = new ConnectorData(species.getPlug().getKind(), species.getPlug().getInitKind(), species.getPlug().getLabel(), 
                        species.getPlug().getPosType(), plugId);
                if(species.getPlug().getKind().equals(polyString))
                    this.hasPolyConnector = true;
            }
            
            //special case for this particular species in the old system
            //should not have a "list" plug like it does.
            //all var decl blocks had a "list" plug.  
            if(!species.origSpeciesName().equals("global-var-decl-num") && 
                    species.origSpeciesName().contains("var-decl")){
                plug = null;
            }
        }else{  //species is null
            //must get info from BlockGenus
            BlockGenus genus = BlockGenus.getGenusWithName(genusName);
            
            //TODO infix blocks!
            //if infix block, ignore first socket, use plug id for conn
            //plug id
            assert genus != null : "No block genus exists for "+this;
            if(genus.getInitPlug() != null){
                BlockConnector p = genus.getInitPlug();
                this.plug = new ConnectorData(p.getKind(), p.initKind(), p.getLabel(), 
                        toStringPosType(p.getPositionType()), plugId);
                if(this.plug.getKind().equals(polyString))
                    this.hasPolyConnector = true;
            }
            
            if(genus.getInitSockets() != null){
                //must test if infix, if so ignore the first socket in 
                //socketBlockIds
                
                int index;
                if(isInfix(genusName)){
                    index = 1;
                    
                }else{
                    index = 0;
                }
                
                sockets = new ArrayList<ConnectorData>();
                for(BlockConnector socket : genus.getInitSockets()){
                    sockets.add(new ConnectorData(socket.getKind(), socket.initKind(), socket.getLabel(), 
                            toStringPosType(socket.getPositionType()), socketBlockIds[index]));
                    if(socket.getKind().equals(polyString))
                        this.hasPolyConnector = true;
                    index++;
                }
                
                //however in the old save format, forever, runonce, runforsometime
                //blocks may have more sockets than there are in its genus because of breeds
                if(genusName.equals("forever") || genusName.equals("runonce") || genusName.equals("runforsometime")){
                    index = sockets.size();
                    while(sockets.size() < socketBlockIds.length){
                        sockets.add(new ConnectorData("cmd", "cmd", "", "single", socketBlockIds[index]));
                        index++;
                    }
                }
            }
            
        }
    }
    
    private String toStringPosType(BlockConnector.PositionType posType){
       if(posType == BlockConnector.PositionType.SINGLE)
           return "single";
       else if(posType == BlockConnector.PositionType.MIRROR)
           return "mirror";
       else
           return "bottom";
    }
    
    public long getBlockID(){
        return blockID;
    }
    
    /**
     * Returns the plug block of this  
     * Or returns null if this block has none. 
     */
    public BlockData getPlugEquivalent(){
        if(beforeId > -1){
            return BlockData.allBlockData.get(Long.valueOf(this.beforeId));
        }else if (plug != null && plug.getConnId() > -1){
            return BlockData.allBlockData.get(Long.valueOf(this.plug.getConnId()));
        }
        
        return  null;
    }
    
    /**
     * Returns true if this block has either a before block or plug 
     * block attached to this
     */
    public boolean hasPlugEquivalent(){
        return (beforeId > -1 || (plug != null &&  plug.getConnId() > -1));
    }
    
    public boolean hasPolyConnector(){
        return this.hasPolyConnector;
    }
    
    public void updatePolyConnectors(){
        //there are no poly plugs in the old system
        //so just check sockets
        if(sockets != null){
            for(ConnectorData cd : sockets){
                if(cd.getKind().equals(polyString) && cd.getConnId() > -1){
                    BlockData other = BlockData.getBlockData(cd.getConnId());
                    assert other.plug != null : "plug block of "+other+" connected to "+this+" is null"; 
                    cd.setKind(other.plug.getKind());
                }
            }
        }
    }
    
    /**
     * Returns a list of all the socket connector data of this block; 
     * null if this has no sockets
     */
    public ArrayList<ConnectorData> getSocketData(){
        return sockets;
    }
    
    /**
     * Returns the after block of this if it exists and there is one 
     * connected to it; null otherwise.
     */
    public BlockData getAfterBlock(){
        return BlockData.allBlockData.get(Long.valueOf(afterId));
    }
    
    public void setAfterId(Long id){
        afterId = id;
    }
    
    public void setBeforeId(Long id){
        beforeId = id;
    }
    
    public long getAfterId(){
        return afterId;
    }
    
    public long getBeforeId(){
        return beforeId;
    }
    
    
    /**
     * The genus name that the new system understands.
     * @return the genus name that the new system understands.
     */
    public String getGenusName(){
        return genusName;
    }
    
    /**
     * The number of breed pages should equal the number of sockets for this 
     * block.  This block should also be a forever, runonce, or runforsometime block.
     * If none of the above conditions are met, exceptions will be thrown.
     * @param pages
     */
    public void setBreedLabels(ArrayList<PageData> pages){
        int index = 0;
        //runforsometime has the "secs" label
        if(this.genusName.equals("runforsometime"))
            index = 1;
        for(PageData pd : pages){
            if(BreedManager.isValidBreedName(pd.getPageName()) && index < sockets.size())
                sockets.get(index).setLabel(pd.getPageName());
            index++;
        }
    }
    
    public int getXCor(){
        return xcor;
    }
    
    public void setXCor(int xcor){
        this.xcor = xcor;
    }
    
    public int getYCor(){
        return ycor;
    }
    
    public void setYCor(int ycor){
    	this.ycor = ycor;
    }
    
    /**
     * Assumes that this block is being assigned a page.  
     * It will translate the old system coordinates to the new system coordinates.
     * @param pageOffset how far the page is from the 0 x cor of canvas
     * @param pageWidth
     */
    public void translateCoorsToPage(int pageOffset, int pageWidth){
        assert xcor >= pageOffset : "Translating block incorrectly: "+this;
        xcor -= pageOffset;
        assert xcor < pageWidth : "pageoffset: "+pageOffset+"xcor greater than pageWidth: "+pageWidth+" "+this;
    }
    
    /**
     * Sets the page that this block data will reside in the new system
     * @param page
     */
    public void setPageTo(PageData page){
        this.page = page;
    }
    
    /**
     * Returns the page that this block data will reside in the new system
     * May return null if the pageName has not been assigned yet.  All translated
     * blocks must belong to a page.
     * @return the page name that this block data will reside in the new system; may return null
     */
    public PageData getPageOf(){
        return this.page;
    }
    
    public void appendSaveString(XMLStringWriter writer, String pageLabel){
        
        if(this.isBlockStub()){
            writer.beginElement("BlockStub", false);
            writer.addDataElement("StubParentName", this.parentName);
            writer.addDataElement("StubParentGenus", this.parentGenus);
        }
        
        writer.beginElement("Block", true);
        writer.addAttribute("id", String.valueOf(this.blockID));
        writer.addAttribute("genus-name", this.genusName);
        writer.addAttribute("is-minimized", String.valueOf(this.isMinimized));
        writer.endAttributes();
        
        writer.addDataElement("Label", label);
        if(this.isBlockStub())
            writer.addDataElement("PageLabel", breed);
        else if(BlockGenus.getGenusWithName(this.genusName).isPageLabelSetByPage())
            writer.addDataElement("PageLabel", pageLabel);
        writer.beginElement("Location", false);
        writer.addDataElement("X", String.valueOf(this.xcor));
        writer.addDataElement("Y", String.valueOf(this.ycor));
        writer.endElement("Location");
        
        if(afterId > -1){
            writer.addDataElement("AfterBlockId", String.valueOf(afterId));
        }
        if(beforeId > -1){
            writer.addDataElement("BeforeBlockId", String.valueOf(beforeId));
        }
        
        /*if(species != null){
            if(species.getPlug() != null){
                writer.beginElement("Plug", false);
                species.getPlug().appendSaveString(writer, "plug");
                writer.endElement("Plug");
            }
            if(species.getSockets().size() > 0){
                writer.beginElement("Sockets", false);
                for(ConnectorData cd : species.getSockets()){
                    cd.appendSaveString(writer, "socket");
                }
                writer.endElement("Sockets");
            }
        }else{*/
            if(plug != null){
                writer.beginElement("Plug", false);
                plug.appendSaveString(writer, "plug");
                writer.endElement("Plug");
                
            }
            if(sockets != null && sockets.size() > 0){
                writer.beginElement("Sockets", false);
                for(ConnectorData cd : sockets){
                    cd.appendSaveString(writer, "socket");
                }
                writer.endElement("Sockets");
            }
       // }
        
            //Now we add some important language
            //specific properties if an only iff this block
            //"hasBreed" or has a non-null constraint.
            //A constraint is a set of bounding ranges such
            //as <minimum, maximum, value>.
            if(constraint != null || hasBreed){
        		writer.beginElement("LangSpecProperties", false);
            	if(hasBreed){
            		//adding breed-name property
            		writer.beginElement("LangSpecProperty", true);
            		writer.addAttribute("key", "breed-name");
            		writer.addAttribute("value", breed);
            		writer.endAttributes();
            		writer.endElement("LangSpecProperty");
            	}
            	if(constraint != null){
            		//adding bounding-min property
            		writer.beginElement("LangSpecProperty", true);
            		writer.addAttribute("key", "bounding-min");
            		writer.addAttribute("value", constraint.min);
            		writer.endAttributes();
            		writer.endElement("LangSpecProperty");
            		
            		//adding bounding-max property
            		writer.beginElement("LangSpecProperty", true);
            		writer.addAttribute("key", "bounding-max");
            		writer.addAttribute("value", constraint.max);
            		writer.endAttributes();
            		writer.endElement("LangSpecProperty");
            		
            		//adding bounding-value property
            		writer.beginElement("LangSpecProperty", true);
            		writer.addAttribute("key", "bounding-value");
            		writer.addAttribute("value", constraint.value);
            		writer.endAttributes();
            		writer.endElement("LangSpecProperty");
            	}
        		writer.endElement("LangSpecProperties");
            }
        
        writer.endElement("Block");
        
        if(this.isBlockStub()){
            writer.endElement("BlockStub");
        }
    }
    
    public String toString(){
        String out = "Block "+blockID+" "+label+" of "+genusName+": afterId "+afterId+" beforeId "+beforeId+" PLUGID: "+plugId+
        " hasBreed "+hasBreed+" breed: "+breed+" at: "+xcor+", "+ycor+"\n species: "+species;
        
        //if(species == null){
            out += " PLUG: "+plug;
            if(sockets != null){
                for(ConnectorData data : sockets){
                    out+= " SOCKET: "+data;
                }
            }
        //}
        
        return out;
    }
}

    