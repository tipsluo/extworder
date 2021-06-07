package extworder;

import java.util.ArrayList;

public class Char {
	String str;
	float x,y;
	float height,width;
	int left,right,top,bottom;
	
	public Char(String str,float x, float y, float height,float width) {
		this.str=str;
		this.x=x;
		this.y=y;
		this.height=height;
		this.width=width;
		left=Math.round(x);
		right=(int)(Math.round(x+width-0.001));
		top=Math.round(y);
		bottom=(int)(Math.round(y+height-0.001));
	}
	
	public boolean coverPoint(float xPoint,float yPoint) {
		return xPoint>=x && xPoint<x+width && yPoint>=y && yPoint<y+height;
	}
	
	public ArrayList<Char> getAboveConnected(Content content) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (top<1) return chars;
		
		Char ch=content.bitmap.points[left][top-1].ch;
		chars.add(ch);
		
		for (int i=left,j=top-1; i<=right; i++) {
			if (ch==content.bitmap.points[i][j].ch) continue;
			
			ch=content.bitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}

	public ArrayList<Char> getBelowConnected(Content content) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (bottom >= content.bottom) return chars;
		
		Char ch=content.bitmap.points[left][bottom+1].ch;
		chars.add(ch);
		
		for (int i=left,j=bottom+1; i<=right; i++) {
			if (ch==content.bitmap.points[i][j].ch) continue;
			
			ch=content.bitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}

	public ArrayList<Char> getLeftConnected(Content content) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (left<1) return chars;
		
		Char ch=content.bitmap.points[left-1][top].ch;
		chars.add(ch);
		
		for (int i=left-1,j=top; j<=bottom; j++) {
			if (ch==content.bitmap.points[i][j].ch) continue;
			
			ch=content.bitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}

	public ArrayList<Char> getRightConnected(Content content) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (right >= content.right) return chars;
		
		Char ch=content.bitmap.points[right+1][top].ch;
		chars.add(ch);
		
		for (int i=right+1,j=top; j<=bottom; j++) {
			if (ch==content.bitmap.points[i][j].ch) continue;
			
			ch=content.bitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}
}
