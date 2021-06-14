package extworder;

import java.util.ArrayList;

public class Char {
	String str;
	float x,y;
	float height,width;
	String fontname;
	int left,right,top,bottom;
	Block block;
	final static float _CharWidthRatio=1.5f;
	final static float _CharHeightRatio=1.5f;
	
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
		
		Char ch=null;
		
		for (int i=left; i<=right; i++) {
			int j=1;
			for (int j1=1;j1<height*_CharHeightRatio;j1++) {
				j=top-j1;
				if (j<0) break;
				if (content.bitmap.points[i][j]!=null) break;
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
		
		Char ch=null;
		
		for (int i=left; i<=right; i++) {
			int j=1;
			for (int j1=1;j1<height*_CharHeightRatio;j1++) {
				j=bottom+j1;
				if (j>=content.bottom) break;
				if (content.bitmap.points[i][j]!=null) break;
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
		
		Char ch=null;
		
		for (int j=top; j<=bottom; j++) {
			int i=left;
			for(int i1=1;i1<width*_CharWidthRatio;i1++) {
				i=left-i1;
				if (i<0) break;
				if (content.bitmap.points[i][j]!=null) break;
			}
					
			if (i<0 || content.bitmap.points[i][j]==null) continue;
			if (ch==content.bitmap.points[i][j].ch) continue;
			
			ch=content.bitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}

	public ArrayList<Char> getRightConnected(Content content) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (right >= content.right) return chars;
		
		Char ch=null;
		
		for (int j=top; j<=bottom; j++) {
			int i=right;
			for(int i1=1;i1<width*_CharWidthRatio;i1++) {
				i=right+i1;
				if (i>content.width) break;
				if (content.bitmap.points[i][j]!=null) break;
			}
			
			if (i>=content.width || content.bitmap.points[i][j]==null) continue;
			if (ch==content.bitmap.points[i][j].ch) continue;
			
			ch=content.bitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}
}
