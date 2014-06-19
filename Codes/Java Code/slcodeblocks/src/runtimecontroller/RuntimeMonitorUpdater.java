package runtimecontroller;

/**
 * This class is used to update RuntimeMonitors on the Swing thread. It is
 * needed because VM "tick" notifications come from the VM thread, which should
 * not edit any GUI state.
 */
public class RuntimeMonitorUpdater implements Runnable {

	private RuntimeRecorder recorder;
	private double time;

	public RuntimeMonitorUpdater(RuntimeRecorder recorder, double time) {
		this.recorder = recorder;
		this.time = time;
	}

	public void run() {
		recorder.updateImage(time);
	}
}
