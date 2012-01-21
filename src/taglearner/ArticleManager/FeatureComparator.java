package taglearner.ArticleManager;

import java.util.Comparator;

public class FeatureComparator implements Comparator<Feature>{

	public int compare(Feature o1, Feature o2) {
		return o1.compareTo(o2);
	}

}
