package taglearner.ResultAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import taglearner.ArticleManager.Feature;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class TopicSet {

	private Set<Feature> m_Orig     = new HashSet<Feature>();
	private Set<Feature> m_Old      = new HashSet<Feature>();
	private Set<Feature> m_Repeated = new HashSet<Feature>();
	private Set<Feature> m_New      = new HashSet<Feature>();

	public TopicSet(ArrayList<Feature> topics) {
		for (Feature f : topics)
			m_Orig.add(f);
	}
	
	public TopicSet(Instances centers, Map<Integer,String> idxToTerm) {
		int size = centers.numInstances();
		for (int i=0; i<size; i++) {
			Instance center = new SparseInstance(centers.instance(i));
			int num = center.numValues();
			for (int j=0; j<num; j++) {
				int index = center.index(j);
				double val = center.valueSparse(j);
				String term = idxToTerm.get(index);
				m_Orig.add(new Feature(term,val));
			}
		}
	}
	
	public Set<Feature> getTopics() {
		Set<Feature> ret = new HashSet<Feature>();
		ret.addAll(m_Old);
		ret.addAll(m_Repeated);
		ret.addAll(m_New);
		return ret;
	}
	
	public void compareAndSet(TopicSet oldSet) {
		Set<Feature> oldTopics = (HashSet<Feature>) oldSet.getTopics();
		Set<Feature> newTopics = new HashSet<Feature>();
		newTopics.addAll(m_Orig);
		Iterator<Feature> iter = oldTopics.iterator();
		while (iter.hasNext()) {
			Feature f = iter.next();
			if (m_Orig.contains(f))
				m_Repeated.add(f);
			else
				m_Old.add(f);
		}
		newTopics.removeAll(m_Old);
		newTopics.removeAll(m_Repeated);
		m_New.addAll(newTopics);
	}
	
	public int getSizeOfOld() { return m_Old.size(); }
	public int getSizeOfRepeated() { return m_Repeated.size(); }
	public int getSizeOfNew() { return m_New.size(); }
	public int getSizeOfOrig() { return m_Orig.size(); }
}
