package taglearner.ExperimentManager;

import java.util.Calendar;
import java.util.Comparator;

public class DateComparator
	implements Comparator<Calendar> {

	public int compare(Calendar d1, Calendar d2) {
		if (d1.before(d2))
			return -1;
		if (d1.after(d2))
			return 1;
		if (ExperimentManager.equal(d1,d2))
			return 0;
		
		return 1;
	}
	
	public boolean equals(Object o) {
		return false;
	}
}
