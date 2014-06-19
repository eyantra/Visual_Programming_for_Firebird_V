package importer;

import java.util.ArrayList;

public class PageData {

    private String pageName;
    private int pageWidth;
    private int initPageWidth;
    
    private ArrayList<BlockData> blocks = new ArrayList<BlockData>();
    
    public PageData(String pageName, int pageWidth){
        this.pageName = pageName;
        this.pageWidth = pageWidth;
        this.initPageWidth = pageWidth;
    }
    
    public String getPageName(){
        return this.pageName;
    }
    
    public int getInitPageWidth(){
        return this.initPageWidth;
    }
    
    public int getPageWidth(){
        return this.pageWidth;
    }
    
    public void addBlockData(BlockData bd){
        bd.setPageTo(this);
        blocks.add(bd);
    }
    
    public void removeBlockData(BlockData bd){
        blocks.remove(bd);
    }
    
    /**
     * Sets the page width of this page. 
     * Note: We may want to change the pageWidth to a width that 
     * differs from the page width specified in the old save file 
     * because in the old system, page did not necessarily enclose all the
     * blocks that belonged to it.  Some stacks may extend over into the 
     * next page
     * @param pageWidth
     */
    public void setPageWidth(int pageWidth){
        this.pageWidth = pageWidth;
    }
    
    public ArrayList<BlockData> getBlockData(){
        return blocks;
    }
    
}
