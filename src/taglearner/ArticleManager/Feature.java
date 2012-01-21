package taglearner.ArticleManager;

import java.io.Serializable;

public class Feature implements Serializable, Comparable<Feature> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4515934533453767766L;
	
	private String m_Term;
	private double m_Weight;
	
	public Feature(String t, double w) {
		m_Term = t;
		m_Weight = w;
	}

	public void setTerm(String term) { this.m_Term = term; }
	public void setWeight(double w) { this.m_Weight = w; }
	
	public String getTerm() { return this.m_Term; }
	public double getWeight() { return this.m_Weight; }
	
	@Override
	public String toString() {
		return "("+m_Term+","+Double.toString(m_Weight)+")";
	}

	public int compareTo(Feature f) {
		double w   = f.getWeight();		
		return (int) (this.m_Weight - w);
	}
	
	@Override
	public boolean equals(Object o) {
		return (this.m_Term.equals(((Feature) o).getTerm()));
	}
	
	@Override
	public int hashCode() {
		return (this.m_Term.hashCode() + (int) this.m_Weight);
	}
}
