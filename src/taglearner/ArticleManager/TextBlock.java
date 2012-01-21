package taglearner.ArticleManager;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;

public class TextBlock implements Serializable {

	private String id;
	private int    lineCnt;
	private String text;
	private ArrayList<String> terms = new ArrayList<String>();
	
	private Point  a = new Point();
	private Point  b = new Point();
	
	public TextBlock(String ID) { this.id = ID; }
	public TextBlock(TextBlock tb) {
		setID(tb.getID());
		setLineCnt(tb.getLineCnt());
		setText(tb.getText());
		setCoordinates(tb.getA(),tb.getB());
		setTerms(tb.getTerms());
	}
	
	public void setID(String newID) { this.id = newID; }
	public void setLineCnt(int c) { this.lineCnt = c; }
	public void setText(String t) { this.text = t; }
	public String getID() { return this.id; }
	public int getLineCnt() { return this.lineCnt; }
	public String getText() { return this.text; }
	public void setTerms(ArrayList<String> newTerms) {
		this.terms.clear();
		this.terms.addAll(newTerms);
	}
	public ArrayList<String> getTerms() { return this.terms; }

	/**
	 * Given a string of the format "x1,y1,x2,y2"
	 * Sets a = (x1,y1) and b = (x2,y2)
	 * 
	 * @param coord
	 */
	public void setCoordinates(String coord) {
	
		String[] coords = coord.split(",");
		if (coords.length != 4) {
			System.out.println("Invalid coordinates for Text block: "+this.id);
			a = new Point(0,0);
			b = new Point(0,0);
		}
		else {
			a = new Point(Integer.parseInt(coords[0]),Integer.parseInt(coords[1]));
			b = new Point(Integer.parseInt(coords[2]),Integer.parseInt(coords[3]));
		}
	}
	
	public void setCoordinates(Point a, Point b) {
		this.a = new Point(a);
		this.b = new Point(b);
	}
	
	// A couple different ways to get the coordinates
	public Point getA() { return this.a; }
	public Point getB() { return this.b; }
	
	public int getX1() { return this.a.x; }
	public int getY1() { return this.a.y; }
	public int getX2() { return this.b.x; }
	public int getY2() { return this.b.y; }
	
	public boolean equals(Object o) {
		return (this.id.equals(((TextBlock) o).getID()));
	}
	public void clear() {
		this.id = "";
		this.lineCnt = 0;
		this.text = "";
	}
}
