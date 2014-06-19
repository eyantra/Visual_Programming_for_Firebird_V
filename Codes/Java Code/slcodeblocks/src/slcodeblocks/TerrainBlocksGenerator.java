package slcodeblocks;

/** 
 * This class is meant to generate the level blocks dynamically.
 * It currently doesn't work. It does, however, generate the 
 * Terrains drawer and place the three most useful blocks in it
 * (load level, reload level and save level). The level blocks are
 * correctly generated on startup (i.e. Level 0 gets generated), but
 * nothing is causing blocks to be added/deleted when terrains are
 * created/destroyed nor do the blocks update their labels when a
 * terrain is renamed. That is still to do. See starlogoc/PatchManagerListener
 * for an interface to receiving notification of these events. 
 */
public class TerrainBlocksGenerator implements DynamicBlockSet {
	
    private static StringBuffer genusString = null;
    private static StringBuffer drawerString = null;
    
    private static TerrainBlocksGenerator tbg = new TerrainBlocksGenerator();
    
    private TerrainBlocksGenerator() { }
    
    /**
     * Returns the single instance of this.
     * @return the single instance of this.
     */
    public static TerrainBlocksGenerator getInstance(){
        return tbg;
    }
    
    public String getGenuses() {
        
        if(genusString == null) {
            genusString = new StringBuffer();
            
            // comment this section out to remove the level blocks
            /*Collection<String> names = Application.app.getLevelNames();
            for(String name : names)
                appendGenus(genusString, name );*/
        }
        
        return genusString.toString();
    }
    
    // generate the level block
    private void appendGenus(StringBuffer contents, String levelName){
        contents.append("<BlockGenus " +
                "name=\"" + levelName + "\" " +
                "kind=\"data\" initlabel=\""+ levelName +"\" " +                  
                "editable-label=\"no\" color=\"255 252 138\">");
        contents.append("<description><text> Name of the level. </text></description>");
        contents.append("<BlockConnectors><BlockConnector connector-kind=\"plug\" " + 
                "connector-type=\"string\" >" + 
                "</BlockConnector></BlockConnectors>"); 
        contents.append("<LangSpecProperties><LangSpecProperty key=\"vm-cmd-name\" value=\"eval-num\"></LangSpecProperty>" + 
                "<LangSpecProperty key=\"special-value\" value=\""+levelName+"\"></LangSpecProperty>" + 
                "</LangSpecProperties></BlockGenus>\n");
    }

    public String getFamilies() {
        return ""; 
    }

    public String getBlockDrawerMembership() {
        if(drawerString == null){
            drawerString = new StringBuffer();
            
            drawerString.append("<BlockDrawer name=\"Terrains\" type=\"factory\" button-color=\"255 252 138\">");
            
            // let the first block in the drawer be set shape.
            // these three blocks are defined in the lang_def file
            drawerString.append("<BlockGenusMember>loadlevel</BlockGenusMember>");
            drawerString.append("<BlockGenusMember>savesnapshot</BlockGenusMember>");
            drawerString.append("<BlockGenusMember>reloadlevel</BlockGenusMember>");
            
            // now list all of the level names -- comment this section out
            // to remove the level blocks as well
            /*Collection<String> names = Application.app.getLevelNames();
            for(String name : names) {
                drawerString.append("<BlockGenusMember>");
                drawerString.append(name);
                drawerString.append("</BlockGenusMember>");
            }*/
            
            drawerString.append("</BlockDrawer>");
        }
        
        return drawerString.toString();
    }   
}
