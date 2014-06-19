package importer;

import codeblockutil.XMLStringWriter;

public class ConnectorData {
    private String kind, label, initKind;
    private String positionType = "single";
    private long connId = -1;
    
    /**
     * This constructor assumes that the information passed to it is information
     * that the old system understands.  It will translate this information for the new system.
     * @param kind
     * @param label
     * @param isCollision
     */
    public ConnectorData(String kind, String label, boolean isCollision){
        translateKind(kind);
        
        
        //if part of a collision block must extract the "number:" string in label
        if(isCollision)
            this.label = label.substring(2);
        else
            this.label = label;
        
    }
    
    /**
     * This constructor assumes that the information passed to it is information 
     * that the new system will understand.  it will not translate any information
     * @param kind
     * @param initKind
     * @param label
     * @param posType
     * @param connId
     */
    public ConnectorData(String kind, String initKind, String label, String posType, long connId){
        this.kind = kind;
        this.initKind = initKind;
        this.label = label;
        this.positionType = posType;
        this.connId = connId;
    }
    
    public String getKind(){
        return kind;
    }
    
    public String getInitKind(){
        return this.initKind;
    }
    
    public void setKind(String k){
        this.kind = k;
    }
    
    
    
    public String getLabel(){
        return label;
    }
    
    public String getPosType(){
        return positionType;
    }
    
    public void setConnId(long id){
        connId = id;
    }
    
    public long getConnId(){
        return connId;
    }
    
    public void setLabel(String l){
        this.label = l;
    }
    
    /**
     * Translates the specified connector kind k to connector kinds and position 
     * types that the new format understands
     * @param k the String kind k from the old version
     */
    private void translateKind(String k){
        if(k.equals("polymorphic")){
            kind =  "poly";
        }else if(k.equals("command")){
            kind = "cmd";
        }else if(k.equals("slider")){
            kind = "number-inv";
        }else if(k.endsWith("Right")){
            kind = k.substring(0, k.indexOf("Right"));
            positionType = "mirror";
        }else if(k.endsWith("Below")){
            kind = k.substring(0, k.indexOf("Below"));
            positionType = "bottom";
        }else{
            kind = k;
        }
        
        //double check if kind == polymorphic, because right and below could
        //have been polymorphicBelow
        if(kind.equals("polymorphic"))
            kind = "poly";
        
        //now set the init kind
        if(!kind.equals("poly"))
            this.initKind = this.kind;
        else
            this.initKind = "poly";
            
    }
    
    /**
     * Appends the save String of this to the specified writer.  The connType is 
     * equal to either "plug" or "socket" depending the connector.
     * @param writer
     * @param connType
     */
    public void appendSaveString(XMLStringWriter writer, String connType){
        writer.beginElement("BlockConnector", true);
        if(label != null)
            writer.addAttribute("label", label);
        writer.addAttribute("connector-kind", connType);
        writer.addAttribute("connector-type", this.kind);
        //procedures, lists should be poly, anything else?
        
        writer.addAttribute("init-type", this.initKind);
        if(connId > -1)
            writer.addAttribute("con-block-id", ""+connId);
        writer.addAttribute("position-type", this.positionType);
        writer.endAttributes();
        writer.endElement("BlockConnector");
    }
    
    public String toString(){
        return "label: "+label+", kind: "+kind+", initKind: "+initKind+", posType: "+positionType+", connId: "+connId;
    }
}
