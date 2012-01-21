package taglearner.ExperimentManager;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateInterval {

	private Calendar start = new GregorianCalendar();
	private Calendar end   = new GregorianCalendar();
	
	public DateInterval(Calendar d1, Calendar d2) {
		this.start = (Calendar) d1.clone();
		this.end   = (Calendar) d2.clone();
	}
	
	public void setStartDate(Calendar d) { this.start = (Calendar) d.clone(); }
	public void setEndDate(Calendar d)   { this.end   = (Calendar) d.clone(); }
	
	public Calendar getStartDate() { return this.start; }
	public Calendar getEndDate()   { return this.end; }

	public static Calendar convertToDate(String base) {
		Calendar d = new GregorianCalendar();
		Pattern p = Pattern.compile("(\\d\\d\\d\\d)(\\d\\d)(\\d\\d)[0-9]*");
		Matcher m = p.matcher(base);
		
		String year, month, date;
		if (m.find()) {
			year = base.substring(m.start(1), m.end(1));
			month = base.substring(m.start(2), m.end(2));
			date = base.substring(m.start(3), m.end(3));
		
			d.set(Calendar.YEAR,Integer.parseInt(year));
			d.set(Calendar.MONTH,Integer.parseInt(month));
			d.set(Calendar.DAY_OF_MONTH,Integer.parseInt(date));
		}
		return d;
	}
}
