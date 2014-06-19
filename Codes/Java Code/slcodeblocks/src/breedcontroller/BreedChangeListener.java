package breedcontroller;

/**
 * Listens for changes to breed tags
 */
interface BreedChangeListener {
	/**
	 * Handle change in breed's name
	 * @param tag
	 * 
	 * @requires tag != null
	 */
	void changeBreedName(BreedTag tag);
	/**
	 * Handle a shape change
	 * @param shape
	 * 
	 * @requires tag != null
	 */
	void changeBreedShape(String shape);
	/**
	 * Handle a change in the active breed (new breed selected)
	 * @param tag
	 * 
	 * @requires tag != null
	 */
	void changeBreedSelection(BreedTag tag);
}
