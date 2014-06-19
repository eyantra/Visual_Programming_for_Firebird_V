package slcodeblocks;

import java.io.File;
import java.util.Iterator;

/**
 * Generates the XML specification for the set of Shape blocks.
 * 
 * NOTE: Only one instance of the ShapeBlocksGenerator should exist.  We only need to load 
 * this information once during start up.
 */
public class ShapeBlocksGenerator implements DynamicBlockSet{

    private static StringBuffer genusString = null;
    private static StringBuffer drawerString = null;
    
    private static ShapeBlocksGenerator sbg = new ShapeBlocksGenerator();
    
    private ShapeBlocksGenerator(){}
    
    /**
     * Returns the single instance of this.
     * @return the single instance of this.
     */
    public static ShapeBlocksGenerator getInstance(){
        return sbg;
    }
    
    public String getGenuses() {
        
        if(genusString == null){
            genusString = new StringBuffer();
            
            Iterator<StarLogoShape> shapes = AvailableShapes.iterator();
            while(shapes.hasNext()){
                StarLogoShape shape = shapes.next();
                appendGenus(genusString, shape.fullName(), shape.directory()+File.separator+shape.skinName()+"_icon.png");
            }
        }
        
        return genusString.toString();
    }
    
    private void appendGenus(StringBuffer contents, String shapeName, String shapeLocation){
        contents.append("<BlockGenus " +
                "name=\"" + shapeName + "\" " +
                "kind=\"data\" initlabel=\"\" " +  
                "color=\"255 252 138\">");
        contents.append("<description><text> Reports the agent shape shown. </text></description>");
        contents.append("<BlockConnectors><BlockConnector connector-kind=\"plug\" " + 
                "connector-type=\"string\" >" + 
                "</BlockConnector></BlockConnectors>");
        contents.append("<Images><Image width=\"64\" height=\"64\"><FileLocation>");
        contents.append(shapeLocation);
        contents.append("</FileLocation></Image></Images>");  
        contents.append("<LangSpecProperties><LangSpecProperty key=\"vm-cmd-name\" value=\"eval-num\"></LangSpecProperty>" + 
                "<LangSpecProperty key=\"special-value\" value=\""+shapeName+"\"></LangSpecProperty>" + 
                "</LangSpecProperties></BlockGenus>\n");
    }

    public String getFamilies() {
        return "";  //Shapes are not grouped into families
    }

    public String getBlockDrawerMembership() {
        if(drawerString == null){
            drawerString = new StringBuffer();
            
            drawerString.append("<BlockDrawer name=\"Shapes\" type=\"factory\" button-color=\"255 252 138\">");
            
            // let the first block in the drawer be set shape.
            drawerString.append("<BlockGenusMember>setshape</BlockGenusMember>");
            
            // now list all of the available shapes
            Iterator<StarLogoShape> shapes = AvailableShapes.iterator();
            while(shapes.hasNext()){
                StarLogoShape shape = shapes.next();
                drawerString.append("<BlockGenusMember>");
                drawerString.append(shape.fullName());
                drawerString.append("</BlockGenusMember>");
            }
            
            drawerString.append("</BlockDrawer>");
        }
        
        return drawerString.toString();
    }
    
    public static void main(String[] args)
    {
        System.out.println("Genuses returned:");
        System.out.println(ShapeBlocksGenerator.getInstance().getGenuses());
        System.out.println("Drawer string returned: ");
        System.out.println(ShapeBlocksGenerator.getInstance().getBlockDrawerMembership());
    }

    
}
