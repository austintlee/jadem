package taglearner.ResultAnalyzer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TopicModelSelector {

	private Set<TopicModeler> m_Models = new HashSet<TopicModeler>();
	
	public void AddTopicModeler(TopicModeler model) {
		m_Models.add(model);
	}
	
	/**
	 * Select the modeler with the minimum sum of variances
	 * @return modeler with the smallest sum of variances
	 */
	public TopicModeler selectBestModeler() {
		Iterator<TopicModeler> iter = m_Models.iterator();
		double minVar = Double.POSITIVE_INFINITY;
		TopicModeler best = null;
		while (iter.hasNext()) {
			TopicModeler model = iter.next();
			double sum = model.getSumOfVariances();
			if (sum < minVar) {
				best = model;
				minVar = sum;
			}
		}
		
		return best;
	}
}
