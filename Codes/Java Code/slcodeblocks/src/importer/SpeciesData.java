package importer;

import java.util.ArrayList;
import java.util.HashMap;

public class SpeciesData {

    private static boolean varDeclGenusMapInit = false;
    private static boolean procParamGenusMapInit = false;
    //maps old genus name for var decls to new genus name for var decls
    private static HashMap<String, String> varDeclGenusMap = new HashMap<String, String>();
    //maps old proc-param names to new param  names
    private static HashMap<String, String> procParamGenusMap = new HashMap<String, String>();
    
    //species name assigned from old system
    private String origSpeciesName;
    
    //genus name that the new version understands
    private String genusName;
    
    //should only be non-null if isStub is true
//    private String parentGenusName = null;    // XXX not used
    
    private boolean isStub = false;
    
    private ConnectorData plug = null;
    private ArrayList<ConnectorData> sockets=null;
    
    
    public SpeciesData(String genusName, String plugKind, String[] socketKinds, 
            String[] socketLabels){
        
        assert socketKinds.length == socketLabels.length : "socketkinds do not match socketlabels";
        
        initVarDeclGenusMapping();
        initParamMapping();
        
        this.origSpeciesName = genusName;
        translateGenusName(genusName);
        
        boolean isCollision = (genusName.equals("collision"));
        
        if(plugKind != null)
            plug = new ConnectorData(plugKind, "", isCollision);
        
        if(socketKinds != null){
            sockets = new ArrayList<ConnectorData>();
            for(int i=0; i<socketKinds.length; i++){
//                ConnectorData cd = new ConnectorData(socketKinds[i], socketLabels[i], isCollision);
                
                //TODO let the BLOCKDATA CLASS TO THIS
                //if(cd.positionType.equals("mirror"))
                  //  plug = cd;
                //else
                    sockets.add(new ConnectorData(socketKinds[i], socketLabels[i], isCollision));
            }
        }
        
    }
    
    private static void initVarDeclGenusMapping(){
        if(!varDeclGenusMapInit){
            varDeclGenusMapInit = true;
            varDeclGenusMap.put("agent-var-decl-num", "agent-var-number");
            varDeclGenusMap.put("agent-var-decl-string", "agent-var-string");
            varDeclGenusMap.put("agent-var-decl-bool", "agent-var-boolean");
            varDeclGenusMap.put("global-var-decl-num", "global-var-number");
            varDeclGenusMap.put("global-var-decl-string", "global-var-string");
            varDeclGenusMap.put("global-var-decl-bool", "global-var-boolean");
            //TODO when patches done, add them here
        }
    }
    
    private static void initParamMapping(){
    	if(!procParamGenusMapInit){
    		procParamGenusMapInit = true;
    		procParamGenusMap.put("proc-param-number", "proc-param-number");
    		procParamGenusMap.put("proc-param-string", "proc-param-string");
    		procParamGenusMap.put("proc-param-boolean", "proc-param-boolean");
    		procParamGenusMap.put("proc-param-list", "proc-param-list");
    		procParamGenusMap.put("proc-param-getter-number", "getterproc-param-number");
    		procParamGenusMap.put("proc-param-getter-string", "getterproc-param-string");
    		procParamGenusMap.put("proc-param-getter-boolean", "getterproc-param-boolean");
    		procParamGenusMap.put("proc-param-getter-list", "getterproc-param-list");
    		procParamGenusMap.put("proc-param-setter-number", "setterproc-param-number");
    		procParamGenusMap.put("proc-param-setter-string", "setterproc-param-string");
    		procParamGenusMap.put("proc-param-setter-boolean", "setterproc-param-boolean");
    		procParamGenusMap.put("proc-param-setter-list", "setterproc-param-list");
    		procParamGenusMap.put("inc-param", "incproc-param-number");
    	}
    }
    
    /**
     * Returns a mapping of var decl species names to their corresponding 
     * genus name equivalent in the new version.  
     * @param speciesName
     * @return String genus name corresponding to the specified speciesName; null
     * if the given speciesName is not a var decl
     */
    public static String translateVarDeclSpeciesNameToGenus(String speciesName){
    	return varDeclGenusMap.get(speciesName);
    }
    
    /**
     * Translates the specified genus name g into a genus name that the new version
     * will understand.  Most genuses translate directly to the genus name understood
     * in the new system, but some genuses such as variable declaration blocks esp. 
     * need to be translated.
     * @param g String genus name from the old version
     */
     private void translateGenusName(String g){
        if(varDeclGenusMap.containsKey(g)){
            genusName = varDeclGenusMap.get(g);
        }else if(g.startsWith("variable-get-value-")){
            //this block is a getter or an agent-who var
            isStub=true;
            if(g.endsWith("of"))
                genusName = "agent";  //need to append the genus of parent - will be done later
            else
                genusName = "getter";  //need to append the genus of the parent - done later
            
        }else if(g.startsWith("set-") && g.endsWith("-variable")){
            isStub = true;
            genusName = "setter";
        }else if(g.equals("inc-variable")){
            isStub = true;
            genusName = "inc"; //need to append the genus of the parent - done later
        }else if(g.equals("procedure-call")){
            isStub = true;
            genusName = "callerprocedure";  //this is the actual genusname of this stub
        }else if(g.startsWith("proc-param")){
        	//we've got a parameter!
        	genusName = procParamGenusMap.get(g);
        	if(genusName.contains("getter") || genusName.contains("setter") ||
        			genusName.contains("inc")){
                isStub=true;
        	}
        }
        else{
            genusName = g;
        }
    }
    
    /**
     * Returns true if the block associated with this species is a stub
     * @return true if the block associated with this species is a stub
     */
    public boolean isBlockStub(){
        return this.isStub;
    }
    
    /**
     * Returns the genusName determined by the species data.
     * The returned genus may not be the final genus name to send to codeblocks, 
     * especially stubs and variable decls.
     * @return the genusName determined by the species data.
     */
    public String getGenusName(){
        return this.genusName;
    }
    
    /**
     * Returns the original species name associated with this data
     * @return the original species name associated with this data
     */
    public String origSpeciesName(){
        return this.origSpeciesName;
    }
    
    /**
     * Returns connector data extracted from the species.
     * Does not necessarily contain connection information like connID
     */
    public ConnectorData getPlug(){
        return plug;
    }
    
    /**
     * Sets the plug to the specified cd
     * @param cd
     */
    public void setPlug(ConnectorData cd){
        plug = cd;
    }
    
    /**
     * Sets the sockets of this to the specified connector data
     * @param cd
     */
    public void setSockets(ArrayList<ConnectorData> cd){
        sockets = cd;
    }
    
    /**
     * Returns connector data extracted from the species.
     * Does not necessarily contain connection information like connID
     */
    public ArrayList<ConnectorData> getSockets(){
        return sockets;
    }
    
    public String toString(){
        StringBuffer str = new StringBuffer();
        str.append("Genus: "+genusName+" orig name: "+this.origSpeciesName);
        if(plug != null)
            str.append(" plugKind: "+plug);
        if(sockets != null){
            str.append(" with sockets: ");
            for(ConnectorData data : sockets){
                str.append(data);
            }
        }
        
        return str.toString();
    }

    
}
