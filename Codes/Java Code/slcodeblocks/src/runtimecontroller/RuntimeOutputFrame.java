package runtimecontroller;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.JComponent;

import codeblockutil.CGraphiteButton;
import codeblockutil.CLabel;
import codeblockutil.CTextField;

/**
 * Utilities class for creating output frame components
 */
public class RuntimeOutputFrame {
	/**
	 * @param l
	 * @return a button for saving the graph data
	 */
	public static JComponent getSaveDataButton(ActionListener l){
		CGraphiteButton button = new CGraphiteButton("Save Data");
		button.setPreferredSize(new Dimension(90,35));
		if (l != null){
			button.addActionListener(l);
		}
		return button;
	}
	/**
	 * @param l
	 * @return a button for saving the graph image
	 */
	public static JComponent getSaveImageButton(ActionListener l){
		CGraphiteButton button = new CGraphiteButton("Save Image");
		button.setPreferredSize(new Dimension(90,35));
		if (l != null){
			button.addActionListener(l);
		}
		return button;
	}
	/**
	 * @param l
	 * @return a textfield for setting the period
	 */
	public static CTextField getPeriodField(ActionListener l){
		CTextField field = new CTextField();
		field.setPreferredSize(new Dimension(90,20));
		if (l != null){
			field.addActionListener(l);
		}
		return field;
	}
	/**
	 * @param l
	 * @return a button for clearing the graph.
	 */
	public static JComponent getClearButton(ActionListener l){
		CGraphiteButton button = new CGraphiteButton("Clear Graph");
		button.setPreferredSize(new Dimension(90,35));
		if (l != null){
			button.addActionListener(l);
		}
		return button;
	}
	/**
	 * @return a label telling the user to set the time inteval
	 */
	public static JComponent getIntevalLabel(){
		CLabel label = new CLabel("Set Time Interval (in seconds)");
		label.setPreferredSize(new Dimension(160,12));
		return label;
	}
}
