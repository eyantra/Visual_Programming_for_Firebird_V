package slcodeblocks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import renderable.BlockUtilities;
import renderable.RenderableBlock;
import workspace.Page;
import workspace.Workspace;
import codeblocks.Block;
import codeblockutil.CButton;
import codeblockutil.CFileHandler;
import codeblockutil.CGraphiteButton;


/**
 * The DataImportManager handles importing of data from
 * a csv file.  It imports the first 300 variables it finds
 * and either creates a new variable or sets the value of
 * that variable.
 * 
 * THe DataImportManager takes in a file of varible
 * declarations of the format:
 * <name>, <value>, <breed owner>
 * The name is a String indicating the name of the variable.
 * The value is a floating point number.  The breed owner is
 * the name of the agent/breed that this variable belongs to.
 * The breed owner is optional, and if none is specified then
 * "Global" is chosen as the default.  Each variable declaration
 * is seperated by a newline character.
 * 
 * The DIM starts by importing from a csv file, then converts
 * the text into data.  the DIM can imports ONLY the first 300
 * variable declarations it sees.  It then checks to see
 * if each variable already exists on teh workspace.  If none
 * exists, then it adds wither an agent variable or a global
 * variable with the same name.  If the user had specified that
 * the breed owner of the variable is "Global", then it will
 * add a global variable.  Otherwise, it adds an agent variable.
 * Finally, each variable is set with the specified value
 */
public final class DataImportManager implements ActionListener{
	private static final String GLOBAL_PAGE = "Everyone";
	private static final String GLOBAL = "Shared";
	private static final String GLOBAL_GENUS = "global-var-number";
	private static final String AGENT_GENUS = "agent-var-number";
	/** The Button of this */
	private CButton button;
	
	/**
	 * Constructor
	 */
	public DataImportManager(){
		button = new CGraphiteButton("import");
		button.addActionListener(this);
	}
	
	/**
	 * Reponse of the clicking on the button
	 */
	public void actionPerformed(ActionEvent e) {
		importData();
	}
	
	/**
	 * @return the UI button to interface with importer
	 */
	public CButton getJButton(){
		return button;
	}
	
	/**
	 * Imports data or show error if failed.
	 *
	 */
	private void importData(){
		try{
			String[] data = parseFile(button);
			Map<String, Tuple<Float, String>> variables = parseData(data);
			parseVariable(variables);
			setVariableValues(variables);
		}catch (DataImportingException ex){
			JOptionPane.showMessageDialog(null, ex.getMessage(), "Data Importing Error", JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}
	
	/**
	 * Compile and set each variable's value.
	 * @param vPair
	 * @throws DataImportingException
	 */
	private void setVariableValues(Map<String, Tuple<Float, String>> variables) throws DataImportingException{
        SLBlockCompiler.getCompiler().compile();
		for (String variable : variables.keySet()){
			Float value = variables.get(variable).getFirst();
			String agent = variables.get(variable).getSecond();
			if(agent.equals(GLOBAL)){
				WorkspaceController.getObserver().setGlobalVariable(variable, value);
			}else{
				//WorkspaceController.getObserver().setAgentVariableValue(who, variable, value);
			}
		}
	}
	
	/**
	 * This method's sole resonsibility is to prepare the
	 * workspace by instantiating any variables that have
	 * been been created yet. It does this by going through
	 * each variable and checking if that variable already
	 * exists in the workspace.  Any variable that does not
	 * already exists will be instantiated.  Here, when we
	 * say instantiated, we mean that the variable will be
	 * assigned a correspinding Block and RenderableBlpck,
	 * then the blocks will be added to the workspace page
	 * canvas and an event will be thrown to notify the
	 * workspace of the addition.
	 * 
	 * @param variables
	 * @requires variables != null, and there exists a page with
	 * 			GLOBAL-PAGE name
	 * @throws DataImportingException
	 */
	private void parseVariable(Map<String, Tuple<Float, String>> variables) throws DataImportingException {
		Set<String> missingVariables = new HashSet<String>(variables.keySet());
		for(Block block : Workspace.getInstance().getBlocks()){
			if(block.isVariableDeclBlock() && missingVariables.contains(block.getBlockLabel())){
				missingVariables.remove(block.getBlockLabel());
			}
		}
		for(String name : missingVariables){
			String agent = variables.get(name).getSecond();
			if(agent.equals(GLOBAL)){
				Page page = Workspace.getInstance().getPageNamed(GLOBAL_PAGE);
				RenderableBlock block = BlockUtilities.getBlock(GLOBAL_GENUS, name);
				page.addBlock(block);
			}else{
				Page page = Workspace.getInstance().getPageNamed(agent);
				if(page == null){
					throw new DataImportingException("No agent exists with the breed: "+agent);
				}else{
					RenderableBlock block = BlockUtilities.getBlock(AGENT_GENUS, name);
					page.addBlock(block);
				}
			}
		}
		Workspace.getInstance().cleanUpAllBlocks();
	}
	
	/**
	 * This digs directs the user to teh system file parser
	 * to find the file he/she desires.  If no file is found,
	 * or if no file was selected, then a DataImportException
	 * is thrown.  When the file is loaded, the file is then
	 * parsed and seperated by newline characters such as "\n"
	 * or "\r".
	 * 
	 * If no information is found in the file, this method
	 * does not guaranteee any action, other then that something
	 * wrong will happen.
	 * 
	 * This method may return an array of any POSITIVE or ZERO
	 * lengths.
	 * 
	 * @param parent
	 * @return an array ofStrings, s, such that;
	 * 		s[0] is the string of variables seperated by commas,
	 * 		s[1] is the string of values seperated by commas.
	 * @requires parent != null
	 * @throws DataImportingException
	 */
	private static String[] parseFile(JComponent parent) throws DataImportingException{
		String[] data = CFileHandler.readFromFile(parent, 300);
		if(data == null){
			throw new DataImportingException(
					"Could not find data in file");
		}else if (data.length<1){
			throw new DataImportingException(
					"File has insufficent data");
		}
		return data;
	}
	
	/**
	 * Takes in an array of variable declarations.  Each declaration
	 * is a line of text containing information seperated by commas.
	 * The first element points to information about the variable's
	 * name.  The second element points to information about
	 * the variable's value.  The third element points to information
	 * about the agent of this variable.  If the variable is a global
	 * variable, then the agent should be "Global".  An example
	 * declaration for an agent variable would be:
	 * 		var1, 8.93, turtles
	 * Notice that there is no comma at the end of the line.  An example
	 * of a global variable declaration would be:
	 * 		var2, 6.3, Global
	 * 
	 * If this method encounters any problem with the input information,
	 * it stops executing immediately and throws a DataImportingException.
	 * 
	 * @param data
	 * @return variable mapping: {variables : (value, agent)}
	 * @requires data != null
	 * @throws DataImportingException
	 */
	private static Map<String, Tuple<Float, String>> parseData(String[] data) throws DataImportingException{
		if(data == null){
			throw new DataImportingException(
					"Data is null.");
		}
		Map<String, Tuple<Float, String>> variables = new HashMap<String, Tuple<Float, String>>();
		for(String datum : data){
			if(datum == null){
				throw new DataImportingException("Data contains null elements.");
			}
			String[] vDatum = datum.split(",");
			if(vDatum.length<2){
				throw new DataImportingException("Variables must be declared with more than 2 attributes: name, value, agent.");
			}
			String variable = vDatum[0].trim();
			if(variable.length()<1){
				throw new DataImportingException("Varibale name is malformed.");
			}else if (variables.containsKey(variable)){
				throw new DataImportingException("Duplicate varibale was declared.");
			}
			String value = vDatum[1].trim();
			if(value.length()<1){
				throw new DataImportingException("Varibale value is malformed.");
			}
			String agent = null;
			if(vDatum.length<3){
				agent = GLOBAL;
			}else{
				agent = vDatum[2].trim();
				if(agent.length()<1){
					agent = GLOBAL;
				}
			}
			try{
				Float fValue = Float.parseFloat(vDatum[1]);
				variables.put(variable, new Tuple<Float, String>(fValue, agent));
			}catch (NumberFormatException ex){
				throw new DataImportingException("Varibale value is malformed.");
			}
			
		}
		return variables;
	}
}
class Tuple<N, M>{
	N first;
	M second;
	public Tuple(N first, M second){
		this.first = first;
		this.second = second;
	}
	public N getFirst() {
		return first;
	}
	public M getSecond() {
		return second;
	}
}
