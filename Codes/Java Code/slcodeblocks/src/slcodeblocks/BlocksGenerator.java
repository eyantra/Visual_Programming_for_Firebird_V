package slcodeblocks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * BlocksGenerator dynamically produces an XML Element object containing the BlockGenus, BlockFamily 
 * and BlockDrawer definitions of dynamic block sets such as Block shapes and Block sounds.  
 * 
 * In order to include a dynamic block set, it must be added to this generator using 
 * BlocksGenerator.addDynamicBlockSet().  
 * 
 */
public class BlocksGenerator {

    private static ArrayList<DynamicBlockSet> sets = new ArrayList<DynamicBlockSet>();
    
    private static StringBuffer contents = null;
    private static Element newRoot = null;
    
    
    private BlocksGenerator(){}
    
    /**
     * Adds the specificed DynamicBlockSet object into this.  
     * The returned Element in BlocksGenerator.getDynamicBlockSetDefns will include
     * the definitions from the added DynamicBlockSet.
     * @param dbs
     */
    public static void addDynamicBlockSet(DynamicBlockSet dbs){
        //System.out.println("adding dbs "+dbs);
        sets.add(dbs);
    }
    
    /**
     * Returns an Element object containing the BlockGenus, Family, and BlockDrawer specifications
     * of dynamic blocks such as shapes and sounds.  
     * @return an Element object containing the BlockGenus, Family, and BlockDrawer specifications
     * of dynamic blocks such as shapes and sounds.  
     */
    public static Element getDynamicBlockSetDefns(){
        
        //System.out.println("getting dynamic block set dfns with : "+sets.size());
        
        if(contents == null || newRoot == null){
            contents = new StringBuffer();
            
            contents.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
            contents.append("<!DOCTYPE BlockLangDef SYSTEM \"support/lang_def.dtd\">\n");
            
            contents.append("<BlockLangDef>");
            
            contents.append("<BlockGenuses>");
            
            for(DynamicBlockSet dbs : sets){
                contents.append(dbs.getGenuses());
            }
            
            contents.append("</BlockGenuses>");
            
            contents.append("<BlockFamilies>");
            
            for(DynamicBlockSet dbs : sets){
                contents.append(dbs.getFamilies());
            }
            
            contents.append("</BlockFamilies>");
            
            //NOTE: MUST KEEP THE FACTORY NAME "factory" IN SYNC WITH THE FACTORY NAME
            //IN LANGUAGE DEFINITION FILE
            contents.append("<BlockDrawerSets><BlockDrawerSet name=\"factory\">"); 
            
            for(DynamicBlockSet dbs : sets){
                contents.append(dbs.getBlockDrawerMembership());
            }
            
            contents.append("</BlockDrawerSet></BlockDrawerSets></BlockLangDef>");
            
            //System.out.println("returned xml: ");
            //System.out.println(contents);
            
            newRoot = parseXML();
        }
        
        return newRoot;
    }
    
    /**
     * Parses the contents XML buffer
     */
    private static Element parseXML()
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document doc;
        Element res = null;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(new ByteArrayInputStream(contents.toString().getBytes()));
            res = doc.getDocumentElement();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }
    
    public static void main(String[] args)
    {
        System.out.println("Genuses returned:");
        BlocksGenerator.getDynamicBlockSetDefns();
        System.out.println("contents: "+contents);
    }
    
}
