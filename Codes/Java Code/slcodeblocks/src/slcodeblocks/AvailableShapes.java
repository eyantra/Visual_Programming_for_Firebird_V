package slcodeblocks;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


/** Stores and manages all available shapes */
public class AvailableShapes
{
    private static final boolean DEBUG = false;
    private static AvailableShapes availableShapes;

    private HashMap<String, StarLogoShape> nameToShape = new HashMap<String, StarLogoShape>();
    private HashMap<String, ShapeCategory> nameToCategory = new HashMap<String, ShapeCategory>();
    private SortedSet<ShapeCategory> categories = new TreeSet<ShapeCategory>();
    // public HashMap<String, StarLogoShape> newShapes;
    private String modelDirectoryPath;
    private StarLogoShape defaultShape;

    private class ShapeCategory implements Comparable<Object>
    {
        private String name;
        private SortedSet<StarLogoShape> shapes = new TreeSet<StarLogoShape>();

        public ShapeCategory(String newName)
        {
            name = newName;
        }

        public boolean addShape(StarLogoShape shape)
        {
            return shapes.add(shape);
        }

        /**
         * The iterator iterates through the category's shapes in ascending
         * order
         */
        public Iterator<StarLogoShape> shapeIterator()
        {
            return shapes.iterator();
        }

        public String name()
        {
            return name;
        }

        public boolean equals(Object o)
        {
            return name.equals(((ShapeCategory) o).name);
        }

        public int compareTo(Object o)
        {
            return name.compareTo(((ShapeCategory) o).name);
        }

        public int hashCode()
        {
            return name.hashCode();
        }
    }

    private AvailableShapes()
    {
        this((System.getProperty("application.home") != null) ? (System
                .getProperty("application.home") + "/models/") : (System
                .getProperty("user.dir") + "/models/"));
    }

    public AvailableShapes(String modelDirectoryPath)
    {
        this.modelDirectoryPath = modelDirectoryPath;
        // nameToShape = new HashMap<String, StarLogoShape>();
        findAvailableShapes();
    }

    private static AvailableShapes getInstance()
    {
        if (availableShapes == null)
            availableShapes = new AvailableShapes();
        return availableShapes;
    }

    public static void main(String[] args)
    {
        availableShapes = new AvailableShapes(System.getProperty("user.dir")
                + "/../../lib/models/");
        JFrame f = new JFrame();
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(800, 600));
        Iterator<StarLogoShape> i = availableShapes.nameToShape.values().iterator();
        while (i.hasNext())
        {
            BufferedImage bi = (BufferedImage) i.next().icon;
            if (bi != null)
                p.add(new JLabel(new ImageIcon(bi)));
        }
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setContentPane(p);
        f.pack();
        f.setVisible(true);
    }

    private boolean findAvailableShapes()
	{
		if (DEBUG) System.out.println("Finding shapes");
		File modelDirectory = new File(modelDirectoryPath);
		File[] categoryFolders = modelDirectory.listFiles();
        if(categoryFolders != null){
        		for (int i = 0; i < categoryFolders.length; i++)
        		{
        			if (!categoryFolders[i].isDirectory() || categoryFolders[i].getName().startsWith("CVS")) continue;
                    
        			String categoryFolderPath = modelDirectory + "/" + categoryFolders[i].getName();
        			File categoryDirectory = new File(categoryFolderPath);
        			String category = categoryFolders[i].getName();
        			File[] modelFolders = categoryDirectory.listFiles();
        			
        			for (int j = 0; j < modelFolders.length; j++) 
                    {
        				if (!modelFolders[j].isDirectory() || modelFolders[j].getName().startsWith("CVS")) continue;
                        
        				String modelFolderName = modelFolders[j].getName();
        				String modelFolderPath = categoryFolderPath + "/" + modelFolderName;
        				String[] modelFolderFileNames = (new File(modelFolderPath)).list();
        				
        				if (modelFolderFileNames==null) continue;
        				List<String> modelFolderContents = Arrays.asList(modelFolderFileNames);
        				Iterator<String> iter = modelFolderContents.iterator();
        				while(iter.hasNext())
        				{
        					String fileName = iter.next();
        					if(fileName.endsWith("_icon.png"))
        					{
        						String shapeName = fileName.substring(0,fileName.length()-9);
        						if((new File(modelFolderPath+"/head.md3").exists() &&
        							new File(modelFolderPath+"/upper.md3").exists() &&
        							new File(modelFolderPath+"/lower.md3").exists() &&
        							new File(modelFolderPath+"/head_"+shapeName+".skin").exists() &&
        							new File(modelFolderPath+"/upper_"+shapeName+".skin").exists() &&
        							new File(modelFolderPath+"/lower_"+shapeName+".skin").exists()) ||
        							new File(modelFolderPath+"/"+modelFolderName+".obj").exists()) {
        							try
        							{
                                        //System.out.println("loading from : "+modelFolderPath+"/"+fileName);
                                        BufferedImage icon = ImageIO.read(new File(modelFolderPath+"/"+fileName));
                                        
                                        if (icon != null && (icon.getWidth() != 64 || icon.getHeight() != 64))
                                        {
                                            //System.out.println("Rescaling image " + fileName + ".");
                                            Image original = icon.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                                            icon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
                                            icon.createGraphics().drawImage(original, 0, 0, null);
                                            // write the scaled image to the file, so we won't need to scale it next time
                                            ImageIO.write(icon, "png", new File(modelFolderPath+"/"+fileName));
                                        }
        
        					            addShape(new StarLogoShape(modelFolderName, category, shapeName, icon));
        							}
        							catch(IOException ex)
        							{
        								ex.printStackTrace();
        							}
        						}
        					}
        				}
                    }
                }
        }
		if (DEBUG) System.out.println("Number of shapes: " + nameToShape.size());
		
		if(nameToShape.size()==0)
			return false;
		else 
			return true;
	}    private void addShape(StarLogoShape shape)
    {
        assert !nameToShape.containsKey(shape.fullName()) : "Found two shapes with the same name";
        if (DEBUG)
            System.out.println("Found shape: " + shape);
        nameToShape.put(shape.fullName(), shape);
        if (StarLogoShape.getFullName("animals", "turtle", "default")
                .equalsIgnoreCase(shape.fullName()))
            defaultShape = shape;
        ShapeCategory newCategory = nameToCategory.get(shape.category());
        if (newCategory == null)
        {
            newCategory = new ShapeCategory(shape.category());
            nameToCategory.put(shape.category(), newCategory);
            categories.add(newCategory);
        }
        newCategory.addShape(shape);
    }

    public static StarLogoShape getShape(String name)
    {
        StarLogoShape shape = getInstance().nameToShape.get(name);
        if (shape != null)
            return shape;
        return getDefaultShape();
    }

    public static StarLogoShape getShape(String category, String modelName,
            String skinName)
    {
        return getShape(StarLogoShape
                .getFullName(category, modelName, skinName));
    }

    public static int size()
    {
        return getInstance().nameToShape.size();
    }

    public static StarLogoShape getDefaultShape()
    {
        return getInstance().defaultShape;
    }

    public static BufferedImage getIcon(String shapeName)
    {
        StarLogoShape shape = getInstance().nameToShape.get(shapeName);
        if (shape != null)
            return shape.icon;
        // else {
        // assert false : "Could not find shape " + shapeName;
        // return null;
        // }
        return getDefaultShape().icon;
    }

    /**
     * @return An iterator over all shapes sorted in ascending order first by
     *         category, then by full name
     */
    public static Iterator<StarLogoShape> iterator()
    {
        class ShapeIterator implements Iterator<StarLogoShape>
        {
            private Iterator<ShapeCategory> categoryIterator = getInstance().categories
                    .iterator();
            private ShapeCategory curCategory = null;
            private Iterator<StarLogoShape> shapeIterator = new NullIterator();
            private boolean done = false;

            class NullIterator implements Iterator<StarLogoShape>
            {
                public StarLogoShape next()
                {
                    assert false : "Error: no next element";
                    return null;
                }

                public void remove()
                {
                    assert false : "Error: no next element";
                }

                public boolean hasNext()
                {
                    return false;
                }
            }

            public ShapeIterator()
            {
                findNext();
            }

            private void findNext()
            {
                if (!shapeIterator.hasNext())
                {
                    if (!categoryIterator.hasNext())
                        done = true;
                    else
                    {
                        curCategory = categoryIterator.next();
                        shapeIterator = curCategory.shapeIterator();
                    }
                }
            }

            private StarLogoShape getCurrent()
            {
                return shapeIterator.next();
            }

            public StarLogoShape next()
            {
                assert !done;
                StarLogoShape shape = getCurrent();
                findNext();
                return shape;
            }

            public void remove()
            {
                next();
            }

            public boolean hasNext()
            {
                return !done;
            }
        }

        return new ShapeIterator();
    }

    /** @return An iterator over all categories in ascending order */
    public static Iterator<String> categoryIterator()
    {
        class CategoryIterator implements Iterator<String>
        {
            private Iterator<ShapeCategory> it = getInstance().categories
                    .iterator();

            public String next()
            {
                return it.next().name();
            }

            public boolean hasNext()
            {
                return it.hasNext();
            }

            public void remove()
            {
                next();
            }
        }

        return new CategoryIterator();
    }

    /**
     * @return null if the specified category does not exist; otherwise, returns
     *         an iterator over all shapes in the specified category sorted in
     *         ascending order (as described by StarLogoShape.compareTo)
     */
    public static Iterator<StarLogoShape> getShapeIterator(String categoryName)
    {
        ShapeCategory category = getInstance().nameToCategory.get(categoryName);
        if (category != null)
        {
            Iterator<StarLogoShape> iterator = category.shapeIterator();
            assert iterator.hasNext() : "Found empty category";
            return iterator;
        } else
            return null;
    }
}
