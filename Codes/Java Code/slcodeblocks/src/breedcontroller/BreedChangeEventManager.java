package breedcontroller;

import java.util.HashSet;
import java.util.Set;

/**
 * Notifies all observers of changes to breed tags
 */
class BreedChangeEventManager {
	private static Set<BreedChangeListener> observers = new HashSet<BreedChangeListener>();
	/**
	 * Add a set listener
	 * @param l
	 * 
	 * @requires l != null
	 */
	static void addBreedChangeListener(BreedChangeListener l){
		if(l == null) throw new RuntimeException("May not subsribe a null listener to PageChanged events");
		observers.add(l);
	}
	/**
	 * Clears the collection of listeners
	 */
	static void clearBreedChangeListeners(){
		observers.clear();
	}
	/**
	 * Notify listeners that a breed's name was changed
	 * @param tag
	 * 
	 * @requires tag 1= null
	 */
	static void fireBreedRenamedEvent(BreedTag tag){
		for(BreedChangeListener l : observers){
			l.changeBreedName(tag);
		}
	}
	/**
	 * Notify listeners that the shape has been changed
	 * @param shape
	 * 
	 * @requires shape != null
	 */
	static void fireShapeChangedEvent(String shape){
		for(BreedChangeListener l : observers){
			l.changeBreedShape(shape);
		}
	}
	/**
	 * Notify listeners that a new breed was selected as active
	 * @param tag
	 * 
	 * @requires tag != null
	 */
	static void fireBreedSelectedEvent(BreedTag tag){
		for(BreedChangeListener l : observers){
			l.changeBreedSelection(tag);
		}
	}
}
