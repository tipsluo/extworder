package extworder;

import java.util.ArrayList;
import java.util.Comparator;

import org.apache.pdfbox.pdmodel.font.PDFont;

public class Char extends Rectangle {
	public String str;
	//String fontname;
	public PDFont font;
	Row row;
	
	static Comparator<Char> compareChars = (Char ch1, Char ch2) ->
		ch1.left!=ch2.left ? (int)(ch1.left-ch2.left) : (int)(ch1.upper-ch2.upper);
		
	public Char(String str,float x, float y, float width, float height, PDFont font) {
		this.str=str;
		this.height=Math.round(height);
		this.width=Math.round(width);
		this.font=font;
		left=Math.round(x);
		right=(int)(Math.round(x+width-0.001));
		upper=Math.round(y);
		lower=(int)(Math.round(y+height-0.001));
	}
	
	public Char(String str) {
		this.str=str;
	}

	public ArrayList<Char> getLeftConnected(Page page, int hInterval, int vAdj) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (left<1) return chars;
		
		Char ch=null;
		
		int u=upper-vAdj;
		if(u<1)
			u=1;
		
		int l=lower+vAdj;
		if(l>page.lower)
			l=page.lower;
		
		for (int j=u; j<=l; j++) {
			int i=left;
			for(int i1=1; i1 <= hInterval;i1++) {
				i=left-i1;
				if (i<0) break;
				if (page.pageBitmap.points[i][j]!=null) 
					break;
			}
					
			if (i<0 || 
				 page.pageBitmap.points[i][j]==null) 
				continue;
			if (ch==page.pageBitmap.points[i][j].ch) continue;
			
			ch=page.pageBitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}

	public ArrayList<Char> getRightConnected(Page page, int hInterval, int vAdj) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (right >= page.right) return chars;
		
		Char ch=null;
		
		int u=upper-vAdj;
		if(u<1)
			u=1;
		
		int l=lower+vAdj;
		if(l>page.lower)
			l=page.lower;
		
		for (int j=u; j<=l; j++) {
			int i=-1;
			for(int i1=1; i1 <= hInterval ; i1++) {
				i=right+i1;
				if (i>page.right) break;
				if (page.pageBitmap.points[i][j]!=null) break;
			}
			
			if(i==-1)
				continue;
			
			if (i>=page.right || 
					page.pageBitmap.points[i][j]==null) 
				continue;
			if (ch==page.pageBitmap.points[i][j].ch) continue;
			
			ch=page.pageBitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}
	
	public ArrayList<Char> getLowerConnected(Page page, int vGap) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (lower >= page.lower) return chars;
		
		Char ch=null;
		
		for (int i=left; i<=right; i++) {
			int j=-1;
			for(int j1=1; j1<=vGap; j1++) {
				j=lower+j1;
				if (j>page.lower) break;
				if (page.pageBitmap.points[i][j]!=null) break;
			}
			
			if(j==-1)
				continue;
			
			if (j>=page.lower || 
					page.pageBitmap.points[i][j]==null) 
				continue;
			if (ch==page.pageBitmap.points[i][j].ch) continue;
			
			ch=page.pageBitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}
	
	boolean isTerminator() {
		return (str.contains(".") || str.contains("?") || str.contains("!"));
	}
	
	public void updateRectangle(Point[][] points) {
		int le=left;
		int ri=right;
		int up=upper;
		int lo=lower;
		
		resetRectangle();
		
		for(int x=le; x<=ri; x++)
			for(int y=up; y<=lo; y++) 
				if(points[x][y]!=null && points[x][y].ch==this) {
					updateRectangle(x,y);
				}
		
		width=right-left;
		height=lower-upper;
	}
	
	static public class Point {
		int x,y;
		Char ch;
		
		public Point(int x, int y, Char ch) {
			this.x=x;
			this.y=y;
			this.ch=ch;
		}
	}
}
