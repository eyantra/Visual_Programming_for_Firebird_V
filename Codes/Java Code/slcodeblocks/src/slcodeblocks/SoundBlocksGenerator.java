package slcodeblocks;

import java.io.*;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Class used for the creation of sound blocks. These are a family of simple string blocks. 
 * They are generated from the list of wave files in the
 * sounds/ directory.
 */
public class SoundBlocksGenerator implements DynamicBlockSet
{
    private static StringBuffer genusString = null;
    private static StringBuffer familyString = null;
    private static StringBuffer drawerString = null;
    
    //list of all the sound names
    private static ArrayList<String> names = null;
   
    /**
     * Default sound name.
     * Note: getBlockName(defaultSoundName) is the block referenced by the lang_def.xml file
     * (in the drawer section).
     */
    public static final String defaultSoundName = "laugh"; 
    
    
    private static SoundBlocksGenerator sbg = new SoundBlocksGenerator();
    
    private SoundBlocksGenerator(){}
    
    /**
     * Returns the single instance of this.
     * @return the single instance of this.
     */
    public static SoundBlocksGenerator getInstance(){
        return sbg;
    }
    
    /**
     * Given a name of a sound, returns the name of the corresponding block (BlockGenus).
     * 
     *  @requires sound_name != null
     *  @returns the block name
     */
    public static String getBlockName(String sound_name)
    {
        return "sound-" + sound_name;
    }
    
    /**
     * Given a name of a sound, returns the string value of the corresponding block.
     * This value is interpreted by the SLAudio system in starlogoc.torusworld.
     * 
     *  @requires sound_name != null
     *  @returns the string value of the sound block
     */
    public static String getBlockValue(String sound_name)
    {
        return sound_name;
    }
    

    public String getGenuses() {
        if(names == null)
            getSoundNamesList();
        
        if(genusString == null){
            genusString = new StringBuffer();
            
            genusString.append("<BlockGenuses>");
            for (String name : names)
                appendBlockGenus(name);
            genusString.append("</BlockGenuses>");
        }
        
        return genusString.toString();
    }

    public String getFamilies() {
        if(names == null)
            getSoundNamesList();
        
        if(familyString == null){
            familyString = new StringBuffer();

            familyString.append("<BlockFamilies><BlockFamily>");
            for (String name: names)
                familyString.append("<FamilyMember>" + getBlockName(name) + "</FamilyMember>");
            familyString.append("</BlockFamily></BlockFamilies>");
        }
        
        return familyString.toString();
    }

    public String getBlockDrawerMembership() {
        if(names == null)
            getSoundNamesList();
        
        if(drawerString == null){
            drawerString = new StringBuffer();
            
            drawerString.append("<BlockDrawer name=\"Sounds\" type=\"factory\" button-color=\"150 255 107\">");
            
            // add play sound block at the top of the sounds drawer
            drawerString.append("<BlockGenusMember>play</BlockGenusMember>");
            
            // add all available sounds to the drawer
            for (String name: names) {
                drawerString.append("<BlockGenusMember>");
	            drawerString.append(getBlockName(name));
	            drawerString.append("</BlockGenusMember>");
            }
            drawerString.append("</BlockDrawer>");
        }
        
        return drawerString.toString();
    }

    /**
     * Returns a list of sound names, defaultSoundName always being first in 
     * the list (regardless of whether the default sound file actually exists)
     */
    private static void getSoundNamesList()
    {
        names = new ArrayList<String>();
        // the default sound is always in there, otherwise there is no block..
        names.add(defaultSoundName);
        
        File soundDir = new File("sounds");
        if (soundDir == null || !soundDir.isDirectory())
            return;
        
        for (File f : soundDir.listFiles())
        {
            String name = f.getName();
            if (!name.endsWith(".wav")) continue;
            // remove .wav"
            name = name.substring(0, name.length() - 4);
            if (!name.equalsIgnoreCase(defaultSoundName))
                names.add(name);
        }
        Collections.sort(names.subList(1, names.size()));
    }

    /**
     * Appends a single block genus section to the XML contents.
     */
    private static void appendBlockGenus(String name)
    {
        genusString.append("<BlockGenus " +
                        "name=\"" + getBlockName(name) + "\" " +
                        "kind=\"data\" initlabel=\"" + getBlockValue(name) + "\" " +  
                        "editable-label=\"no\" color=\"150 255 107\">");
        genusString.append("<description><text> Sound </text></description>");
        genusString.append("<BlockConnectors><BlockConnector connector-kind=\"plug\" " + 
                        "connector-type=\"string\" position-type=\"mirror\">" + 
                        "</BlockConnector></BlockConnectors>");
        genusString.append("<LangSpecProperties>" + 
                        "<LangSpecProperty key=\"vm-cmd-name\" value=\"eval-num\"></LangSpecProperty>" + 
                        "<LangSpecProperty key=\"is-monitorable\" value=\"yes\"></LangSpecProperty>" + 
                        "</LangSpecProperties></BlockGenus>");
    }

}
