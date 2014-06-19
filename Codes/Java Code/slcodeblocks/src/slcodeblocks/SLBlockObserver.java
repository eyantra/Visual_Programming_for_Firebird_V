package slcodeblocks;

import java.awt.Frame;
import java.util.List;
import java.util.Observer;

public interface SLBlockObserver {

    public void downloadCode(CodePair[] instructions);
    public void runCode(boolean hasRunOnce);
    public long commandToSlnum(int command);
    public long numberToSlnum(double number);
    public int slnumToCommand(long commandAsSlnum);
    public double slnumToNumber(long numberAsSlNum);
    
    public long getBreedSlnum(String breed);
    public long getBreedShapeSlnum(String breed);
    
    public void reallocateTurtleVariables(List<Variable> newVars);
    public void reallocatePatchVariables(List<Variable> newVars);
    public void reallocateGlobalVariables(List<Variable> newVars);
    public void renameGlobalVariable(Variable oldVar, Variable newVar);
    public void renameTurtleVariable(Variable oldVar, Variable newVar);
    public void renamePatchVariable(Variable oldVar, Variable newVar);

    public void setGlobalVariable(String globalVarName, double value);
	//public void setGlobalBoolean(String command, boolean value);
    public double getGlobalVariable(int index);
	public double getMonitorValue(int codePos);
	public boolean getMonitorBooleanValue(int codePos);
	public String getMonitorStringValue(int codePos);
    public long addStringToHeap(String string);
    public long allocateList(int size);
    /** Specifies text that should be used in the undo menu item.  It should be disabled iff it begins with "Can't". */
    public void setUndoString(String undoString);
    /** Specifies text that should be used in the redo menu item.  It should be disabled iff it begins with "Can't". */
    public void setRedoString(String redoString);

	public void runForSomeTimeBlockTurnedOff(long id);
    public void toggleRunBlock(Long id, boolean isRunning);

    public static class CodePair {
	public long[] instructions;
	public int arrayposition;
	public CodePair(long[] instructions, int arrayposition) {
	    this.instructions = instructions;
	    this.arrayposition = arrayposition;
	}
    }


    public void clearAll();
    public void clearTurtles();
    public void clearPatches();
    public void scatterPC(double red, double green, double blue, double black);
    public void createTurtles(int num, String breed, int codePos);
    public void scatterTurtles(String breed);
    public void setPatchTerrain(int index);
    public void setBreeds(String[] breeds, String[] breedIcons);

	public void addBreed(String breedName, String breedDefaultShape);
	public void deleteBreed(String breedName);
	public void renameBreed(String oldName, String newName, String newShape);
	public void updateBreedShape(String breedName, String newShape);
    public boolean doesBreedExist(String breedName);
    public void deleteAllBreeds();

    public Frame getActiveFrame();
    
    // Support for Agent Monitor
    public boolean isAgentMonitored(int who);
    public void setAgentMonitored(int who, boolean monitored);
    public boolean isAgentAlive(int who);
    public boolean isAgentInvisible(int who);
    public void setAgentInvisible(int who, boolean invisible);
    public boolean isAgentPendown(int who);
    public void setAgentPendown(int who, boolean pendown);
    public double getAgentXcor(int who);
    public void setAgentXcor(int who, double xcor);
    public double getAgentYcor(int who);
    public void setAgentYcor(int who, double ycor);
    public double getAgentHeading(int who);
    public void setAgentHeading(int who, double heading);
    public double getAgentSize(int who);
    public void setAgentSize(int who, double size);
    public double getAgentHeightAboveTerrain(int who);
    public void setAgentHeightAboveTerrain(int who, double height);
    public double getAgentColorNumber(int who);
    public void setAgentColorNumber(int who, double color);
    public String getAgentShape(int who);
    public String getAgentBreed(int who);

    public int getWhoNumberForCamera();
    public void setWhoNumberForCamera(int who);
    public int getCameraView();
    public void setCameraView(int cameraView);
    public void addCameraViewListener(Observer o);
    public static final int AGENT_EYE = 1;
    public static final int AGENT_SHOULDER = 2;

    public List<Variable> getAgentVariableList();
	public double getAgentNumberVariableValue(int who, Variable variableName);
	public boolean getAgentBooleanVariableValue(int who, Variable variableName);
	public String getAgentStringVariableValue(int who, Variable variableName);
	public void setAgentVariableValue(int who, Variable variableName, double value);
	public void setAgentVariableValue(int who, Variable variableName, boolean value);
	public void setAgentVariableValue(int who, Variable variableName, String value);

	public int getAgentHatchedWho(int who);
	public void speedChanged(int speedSliderPosition);
	public void stepVM();
	public void stopNow();
	public double getStarLogoTime();
        public void setScore(double score);
	public void showScore();
	public void hideScore();
	public void showClock();
	public void hideClock();
	public void resetClock();
    public void showMiniView();
    public void hideMiniView();
	/** Indicates that the project has changed. */
	public void markChanged();
        
    public void clearInterestInCollisions();
    public void interestInCollision(long whoA, long whoB);
    
    // to bring SpaceLand window when we run code
    public void focusObserver();
    
    // Network-related methods
    public void setNetworkStatus(boolean networked, int hostnum, int actions[]);
    public void importTurtles(long[] breeds, long[] turtleHeap);
    public void importKeyDeltas(int hostnum, byte[] deltas);
}
