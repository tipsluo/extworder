package extworder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Char extends Rectangle {
	String str;
	//float x,y;
	String fontname;
	Row row;
	float width,height;
	
	static Comparator<Char> compareChars = (Char ch1, Char ch2) ->
		ch1.left!=ch2.left ? (int)(ch1.left-ch2.left) : (int)(ch1.upper-ch2.upper);
		
	public Char(String str,float x, float y, float width, float height, String fontname) {
		this.str=str;
		//this.x=x;
		//this.y=y;
		this.height=height;
		this.width=width;
		this.fontname=fontname;
		left=Math.round(x);
		right=(int)(Math.round(x+width-0.001));
		upper=Math.round(y);
		lower=(int)(Math.round(y+height-0.001));
	}

	public ArrayList<Char> getLeftConnected(Page page) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (left<1) return chars;
		
		Char ch=null;
		
		for (int j=upper; j<=lower; j++) {
			int i=left;
			for(int i1=1; i1 < height * Common._CharHGapRatio;i1++) {
				i=left-i1;
				if (i<0) break;
				if (page.pageBitmap.points[i][j]!=null) break;
			}
					
			if (i<0 || page.pageBitmap.points[i][j]==null) continue;
			if (ch==page.pageBitmap.points[i][j].ch) continue;
			
			ch=page.pageBitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}

	public ArrayList<Char> getRightConnected(Page page) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (right >= page.right) return chars;
		
		Char ch=null;
		
		for (int j=upper; j<=lower; j++) {
			int i=right;
			for(int i1=1; i1 < height * Common._CharHGapRatio ; i1++) {
				i=right+i1;
				if (i>page.right) break;
				if (page.pageBitmap.points[i][j]!=null) break;
			}
			
			if (i>=page.right || page.pageBitmap.points[i][j]==null) continue;
			if (ch==page.pageBitmap.points[i][j].ch) continue;
			
			ch=page.pageBitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
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
