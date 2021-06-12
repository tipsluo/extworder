package extworder;

import java.util.ArrayList;

public class Char {
	String str;
	float x,y;
	float height,width;
	String fontname;
	int left,right,top,bottom;
	Block block;
	
	public Char(String str,float x, float y, float height,float width, String fontname) {
		this.str=str;
		this.x=x;
		this.y=y;
		this.height=height;
		this.width=width;
		this.fontname=fontname;
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
		
		//if (content.bitmap.points[left][top-1]==null) return chars;
		
		Char ch=null;
		//Char ch=content.bitmap.points[left][top-1].ch;
		//chars.add(ch);
		
		for (int i=left; i<=right; i++) {
			int j=1;
			for (int j1=1;j1<height;j1++) {
				j=top-j1;
				if (j<0) break;
				if (content.bitmap.points[i][j]==null) continue;
			}
			
			if (j<0 || content.bitmap.points[i][j]==null) continue;
			
			if (ch==content.bitmap.points[i][j].ch) continue;
			
			ch=content.bitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}

	public ArrayList<Char> getBelowConnected(Content content) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (bottom >= content.bottom) return chars;

		//if (content.bitmap.points[left][bottom+1]==null) return chars;
		
		Char ch=null;
		//chars.add(ch);
		
		for (int i=left; i<=right; i++) {
			int j=1;
			for (int j1=1;j1<height;j1++) {
				j=bottom+j1;
				if (j>content.bottom) break;
				if (content.bitmap.points[i][j]==null) continue;
			}
			
			if(j>content.bottom || content.bitmap.points[i][j]==null) continue;
			if (ch==content.bitmap.points[i][j].ch) continue;
			
			ch=content.bitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}

	public ArrayList<Char> getLeftConnected(Content content) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (left<1) return chars;
		
		//if(content.bitmap.points[left-1][top]==null) return chars;
		
		//Char ch=content.bitmap.points[left-1][top].ch;
		//chars.add(ch);
		Char ch=null;
		
		for (int i=left-1,j=top; j<=bottom; j++) {
			if (content.bitmap.points[i][j]==null) continue;
			if (ch==content.bitmap.points[i][j].ch) continue;
			
			ch=content.bitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}

	public ArrayList<Char> getRightConnected(Content content) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (right >= content.right) return chars;
		
		//if(content.bitmap.points[right+1][top]==null) return chars;
		
		//Char ch=content.bitmap.points[right+1][top].ch;
		//chars.add(ch);
		Char ch=null;
		
		for (int i=right+1,j=top; j<=bottom; j++) {
			if (content.bitmap.points[i][j]==null) continue;
			if (ch==content.bitmap.points[i][j].ch) continue;
			
			ch=content.bitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}
}
