package taglearner.ResultAnalyzer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.stat.StatUtils;

public class TopicModeler {

	private List<TopicSet> m_TopicList = new ArrayList<TopicSet>();
	private List<Integer> m_Old      = new ArrayList<Integer>();
	private List<Integer> m_Repeated = new ArrayList<Integer>();
	private List<Integer> m_New      = new ArrayList<Integer>();
	private TopicSet m_FirstSet = null;
	private double m_VarOld, m_VarRepeated, m_VarNew;
	
	public void addTopicSet(TopicSet ts) {
		
		if (null == m_FirstSet) {
			m_FirstSet = ts;
			return;
		}
		else if (this.m_TopicList.size() == 0) {
			ts.compareAndSet(m_FirstSet);
		}
		else {
			ts.compareAndSet(m_TopicList.get(m_TopicList.size()-1));
		}
		
		this.m_TopicList.add(ts);
	}
	
	public void runModeler() {
		for (TopicSet ts : m_TopicList) {
			m_Old.add(ts.getSizeOfOld());
			m_Repeated.add(ts.getSizeOfRepeated());
			m_New.add(ts.getSizeOfNew());
		}
		
		int size = m_Old.size();
		double [] oldVals    = new double[size];
		double [] repeatVals = new double[size];
		double [] newVals    = new double[size]; 
		
		int i=0;
		for (Integer d : m_Old) {
			oldVals[i] = (double) d;
			i++;
		}
		
		i=0;
		for (Integer d : m_Repeated) {
			repeatVals[i] = (double) d;
			i++;
		}
		
		i=0;
		for (Integer d : m_New) {
			newVals[i] = (double) d;
			i++;
		}
		
		m_VarOld = StatUtils.variance(oldVals);
		m_VarRepeated = StatUtils.variance(repeatVals);
		m_VarNew = StatUtils.variance(newVals);
	}
	
	public double getSumOfVariances() {
		return (m_VarOld + m_VarRepeated + m_VarNew);
	}
	
	@Override
	public String toString() {
		String toPrint = "";
		
		toPrint = toPrint + m_Old.toString() + "\n";
		toPrint = toPrint + m_Repeated.toString() + "\n";
		toPrint = toPrint + m_New.toString() + "\n";
		toPrint = toPrint + "\n";
		toPrint = toPrint + "Variances:";
		toPrint = toPrint + m_VarOld + "," + m_VarRepeated + "," + m_VarNew;
		
		return toPrint;
	}
}
