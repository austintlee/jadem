package taglearner.ResultAnalyzer;

import java.util.Comparator;

public class ExperimentResultComparator implements Comparator<ExperimentResult>{

	public int compare(ExperimentResult o1, ExperimentResult o2) {
		int c1, c2;
		c1 = o1.getExpIter();
		c2 = o2.getExpIter();
		return (c1 - c2);
	}
}
