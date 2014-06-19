package slcodeblocks;

import java.awt.image.BufferedImage;

/** Stores information regarding a single shape */
public class StarLogoShape implements Comparable<Object> {
	private String name;
	private String skin;
	private String category;
	public BufferedImage icon;
	
	public StarLogoShape(String newName, String newCategory, String newSkin, BufferedImage newIcon) {
		name = newName;
		category = newCategory;
		skin = newSkin;
		icon = newIcon;
	}
	
	/** This function is duplicated in torusworld.TorusWorld */
	public static String getCategoryName(String name) {
		int pos = name.lastIndexOf("/");
	    return name.substring(0, pos);
	}
	
	/** This function is duplicated in torusworld.TorusWorld */
	public static String getModelName(String name) {
		int pos1 = name.lastIndexOf("/");
		int pos2 = name.lastIndexOf("-");
	    return name.substring(pos1 + 1, pos2);
	}
	
	/** This function is duplicated in torusworld.TorusWorld */
	public static String getSkinName(String name) {
		int pos = name.lastIndexOf("-");
	    return name.substring(pos + 1);
	}
	
	/** This function is duplicated in torusworld.TorusWorld */
	public static boolean isValidFullName(String name) {
		return name != null && name.length() > 0 && name.indexOf("/") >= 0 && name.lastIndexOf("-") > name.lastIndexOf("/");
	}
	
	/** This function is duplicated in torusworld.TorusWorld */
	public static String getFullName(String category, String modelName, String skinName) {
		return category + "/" + modelName + "-" + skinName;
	}
	
	/** This function is duplicated in torusworld.TorusWorld */
	public String directory() {
		if (System.getProperty("application.home")!=null) {
			return System.getProperty("application.home")+"/models/" + category + "/" + name;
		}
		return System.getProperty("user.dir")+"/models/" + category + "/" + name;
	}
	
	public String fullName() {
		return getFullName(category, name, skin);
	}
	
	public String modelName() {
		return name;
	}
	
	public String skinName() {
		return skin;
	}
	
	public String category() {
		return category;
	}
	
	public boolean equals(Object o) {
		return fullName().equals(((StarLogoShape)o).fullName());
	}
	
	public int compareTo(Object o) {
		return fullName().compareTo(((StarLogoShape)o).fullName());
	}
	
	public int hashCode() {
		return fullName().hashCode();
	}
	
	public String toString() {
		return "[StarLogoShape " + fullName() + "]";
	}
}







