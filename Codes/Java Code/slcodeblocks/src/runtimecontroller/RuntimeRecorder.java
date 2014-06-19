package runtimecontroller;

/**
 * A recorder records and monitors the values of
 * a set of monitorables.
 */
public interface RuntimeRecorder {
	/** time steps are never smaller than EPSILON in magnitude,  used for comparing doubles of time */
	public static final double EPSILON = -0.0001;

	/**
	 * Updates the domain of this lines graph (or the set of monitorables)
	 */
	public void updateDomain();
	/**
	 * Updates the the data by storing and processing
	 * Only do this if the the data is not being locked and one period cycle has passed
	 * @parem currentTime the current StarLogo time to update the data by
	 */
	public void updateChart(double currentTime);
	
	/**
	 *  renders the graph,  should be scheduled with the AWT thread after an updateChart is called
	 */
	public void updateImage(double currentTime);
	
	/**
	 * Updates all the series names of the chart within this recorder
	 */
	public void updateChartSeriesNames();
}
