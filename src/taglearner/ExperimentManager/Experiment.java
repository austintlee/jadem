package taglearner.ExperimentManager;

import java.io.Serializable;

public class Experiment implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 26006386202240290L;
	private int start;
	private int duration;
	private int increment;
	private int iteration;
	
	public void setExperiment(int s, int i, int j, int k) {
		this.start = s;
		this.duration = i;
		this.increment = j;
		this.iteration = k;
	}
	
	public int getStart() { return this.start; }
	public int getDuration() { return this.duration; }
	public int getIncrement() { return this.increment; }
	public int getIteration() { return this.iteration; }
}
