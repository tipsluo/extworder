package extworder;

import java.util.ArrayList;
import java.util.Comparator;

import extworder.Page.PageBitmap;

public class Char extends Rectangle {
	String str;
	String fontname;
	Row row;
	float width,height;
	
	static Comparator<Char> compareChars = (Char ch1, Char ch2) ->
		ch1.left!=ch2.left ? (int)(ch1.left-ch2.left) : (int)(ch1.upper-ch2.upper);
		
	public Char(String str,float x, float y, float width, float height, String fontname) {
		this.str=str;
		this.height=height;
		this.width=width;
		this.fontname=fontname;
		left=Math.round(x);
		right=(int)(Math.round(x+width-0.001));
		upper=Math.round(y);
		lower=(int)(Math.round(y+height-0.001));
	}

	public ArrayList<Char> getLeftConnected(Page page, int maxInterval) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (left<1) return chars;
		
		Char ch=null;
		
		for (int j=upper; j<=lower; j++) {
			int i=left;
			for(int i1=1; i1 < maxInterval;i1++) {
				i=left-i1;
				if (i<0) break;
				if (page.pageBitmap.points[i][j]!=null && 
						page.pageBitmap.points[i][j]!=Common._ConfusingPoint) 
					break;
			}
					
			if (i<0 || 
				 page.pageBitmap.points[i][j]==null ||
				 page.pageBitmap.points[i][j]==Common._ConfusingPoint) 
				continue;
			if (ch==page.pageBitmap.points[i][j].ch) continue;
			
			ch=page.pageBitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}

	public ArrayList<Char> getRightConnected(Page page, int maxInterval) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (right >= page.right) return chars;
		
		Char ch=null;
		
		for (int j=upper; j<=lower; j++) {
			int i=-1;
			for(int i1=1; i1 < maxInterval ; i1++) {
				i=right+i1;
				if (i>page.right) break;
				if (page.pageBitmap.points[i][j]!=null && 
						page.pageBitmap.points[i][j]!=Common._ConfusingPoint) break;
			}
			
			if(i==-1)
				continue;
			
			if (i>=page.right || 
					page.pageBitmap.points[i][j]==null ||
					page.pageBitmap.points[i][j]==Common._ConfusingPoint) 
				continue;
			if (ch==page.pageBitmap.points[i][j].ch) continue;
			
			ch=page.pageBitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}
	
	public void clearCross(Point[][] points, int x,int y) {
		for (int i=left; i<=right; i++)
			points[i][y]=null;
		for (int i=upper; i<=lower; i++)
			points[x][i]=null;
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
