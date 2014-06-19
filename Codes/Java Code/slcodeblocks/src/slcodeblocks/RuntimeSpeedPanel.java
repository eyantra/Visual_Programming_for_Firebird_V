package slcodeblocks;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import codeblockutil.CIconButton;
import codeblockutil.CSlider;
import codeblockutil.CIconButton.Icon;


/**
 * RuntimeSpeedPanel is found in the Runtime Workspace perspective.
 * 
 * Abstractly, it holds information about the VM's current speed, and
 * also defines the maximum and minimum value that the VM's speed can have.
 * 
 * Graphically, it contains a slider, a pause/run button, and a step button.
 * These  buttons allows users to make changes to the VM's current speed
 * value graphically.
 * 
 * The RuntimeSpeedPanel is also a controller that maintains the abstract
 * VM speed value and the Graphical representation of the speed via a CSlider.
 * One important aspect of this controller is that it must convert between the
 * CSlider's value (which ranges between [5,-5]) and the abstract VM Speed
 * value (which ranges between [1,11]).
 * 
 * The RuntimeSpeedPanel provides two public methods that may be of interest
 * to clients of the RuntimeSpeedPanel: getSpeedSliderPosition() and
 * setSpeedSliderPosition().  These methods return and mutate the ABSTRACT
 * speed value, which the controller then uses to set to set the position of
 * the graphical CSldier GUI.
 * 
 * @specfield VM_VALUE : int //the absract VM speed value
 * @specfield CSLIDER_VALUE : int //the graphical representation of the speed value
 */
public class RuntimeSpeedPanel extends JPanel{
	private static final long serialVersionUID = 328149080415L;
	/** The abstract min VM speed value */
	public static final int MIN_VALUE = 1;
	 /** The abstract max VM speed value */
    public static final int MAX_VALUE = 11;
    /** The abstract default VM speed value */
    public static final int REALTIME_VALUE = 6;
	
    /** The graphical default CSlider value */
	private static final int DEFAULT_VALUE = 0;
	/** The graphical max CSlider value */
	private static final int MAXIMUM_VALUE = 5;
	/** The graphical min CSlider value */
	private static final int MINIMUM_VALUE = -5;
	
	/** Application */
	private final SLBlockObserver observer;
	/** Graphical pause button */
	private final CIconButton pause;
	/** Graphical step button */
	private final CIconButton step;
	/** Graphical stop button */
	private final CIconButton stop;
	/** Graphical CSlider, also acts as a data structure
	 * for holding the current value of the abstract VM's speed
	 * value.  The conversion from abstract to view is a translation of -6
	 */
	private final CSlider slider;
	/** Run/Pause falg */
	private boolean playMode;
	/** The buffered value of the graohicak slider */
	private int oldSliderValue;
	
	/**
	 * Constructs RuntimeSpeedPanel
	 * 
	 * @param app
	 * 
	 * @requires app != null
	 * @effects constructs a new RuntimeSpeedPanel with the value
	 * 			set to the default abstract value
	 */
	public RuntimeSpeedPanel(SLBlockObserver app) {
		super(new FlowLayout());
		super.setBackground(Color.black);
		this.observer = app;
		this.playMode = true;
		this.oldSliderValue = DEFAULT_VALUE;
		
		pause = new CIconButton(Icon.PAUSE);
		pause.setToolTipText("Pause at the current frame");
		
		step = new CIconButton(Icon.STEP);
		step.setToolTipText("Run for one frame");

		stop = new CIconButton(Color.BLACK, Color.RED, Icon.STOP);
		stop.setToolTipText("Emergency stop");

		slider = new CSlider(MINIMUM_VALUE,MAXIMUM_VALUE,DEFAULT_VALUE);
		slider.setBounds(0,0,225,20);
		slider.setToolTipText("Change the execution speed");
		
		JLabel t0 = new TickMarkLabel("0");
		t0.setBounds(0,20,20,20);
		JLabel thalf = new TickMarkLabel("0.5");
		thalf.setBounds(65,20,20,20);
		JLabel t1 = new TickMarkLabel("1");
		t1.setBounds(110,20,20,20);
		JLabel t2 = new TickMarkLabel("2");
		t2.setBounds(152,20,20,20);
		JLabel t5 = new TickMarkLabel("5");
		t5.setBounds(172,20,20,20);
		JLabel tmax = new TickMarkLabel("max");
		tmax.setBounds(200,20,40,20);
		
		JPanel sliderPane = new JPanel(null);
		sliderPane.setOpaque(false);
		sliderPane.add(slider);
		sliderPane.add(t0);
		sliderPane.add(thalf);
		sliderPane.add(t1);
		sliderPane.add(t2);
		sliderPane.add(t5);
		sliderPane.add(tmax);
		sliderPane.setPreferredSize(new Dimension(225,40));

		add(step);
		add(pause);
		add(stop);
		add(sliderPane);
		
		this.revalidate();
		
		pause.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(playMode == true){
					oldSliderValue = slider.getValue();
					playMode = false;
					slider.setValue(MINIMUM_VALUE);
					pause.setToolTipText("Play at the current frame");
					pause.setIcon(Icon.PLAY);
				}else{
					slider.setValue(oldSliderValue);
					playMode = true;
					pause.setToolTipText("Pause at the current frame");
					pause.setIcon(Icon.PAUSE);
				}
				observer.speedChanged(getSpeedSliderPosition());
				
			}
		});
		
		step.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(playMode != true){
					observer.stepVM();
				}
			}
		});
		
		stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e){
				observer.stopNow();
			}			
		});
		
		slider.addMouseListener(new MouseAdapter(){
			public void mouseReleased(MouseEvent e){
				//property changed event thrown ONLY when user user releases mouse
				if(slider.getValue() == MINIMUM_VALUE){
					//if slider has been moved to 0%, select the pause button
					playMode = false;
					oldSliderValue = DEFAULT_VALUE;
				}else{
					//otherwise, you're in play mode
					playMode = true;
				}
				observer.speedChanged(getSpeedSliderPosition());
			}
		});
    }
    
    /**
     * Sets the abstract VM's speed value to the value of "position".
     * The values of "position" should range between
     * [RuntimeSpeedPanel.MIN_VALUE, RuntimeSpeedPanel.MAX_VALUE],
     * which happens to currently be [1,11].
     * 
     * If the new "position" is some value outside the range, it will
     * be set to some either the MIN or MAX value, whichever is closest.
     * 
     * If the new "position" is set to RuntimeSpeedPanel.MIN_VALUE,
     * then the this goes into pause mode.
     */
    public void setSpeedSliderPosition(int position){
        //we need to convert the position value, which is based on
    	//a range of [1 to 11] inclusive, to the slider raw value,
    	//which is based on a range of [-5 to 5] inclusive.
    	int graphicalPosition = position-6;
    	slider.setValue(graphicalPosition);
		if(slider.getValue() == MINIMUM_VALUE){
			//if slider has been moved to 0%, select the pause button
			playMode = false;
			oldSliderValue = DEFAULT_VALUE;
		}else{
			//otherwise, you're in play mode
			playMode = true;
		}
		observer.speedChanged(getSpeedSliderPosition());
    }
    
    /**
     * @returns the abstract VM's speed value
     */
    public int getSpeedSliderPosition(){
        //we need to convert the position value, which is based on
    	//a range of [1 to 11] inclusive, to the slider raw value,
    	//which is based on a range of [-5 to 5] inclusive.
    	return slider.getValue()+6;
    }
}

/**
 * Label for major tick marks
 */
class TickMarkLabel extends JLabel{
	private static final long serialVersionUID = 328149080416L;
	public TickMarkLabel(String text){
		super(text);
		this.setForeground(Color.white);
		this.setFont(new Font("Ariel", Font.BOLD, 10));
	}
}