package extworder;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.graphics.color.PDColor;

import extworder.Common.CheckBold;

public class Char extends Rectangle {
	String str;
	float x,y;
	PDColor color;
	String fontname;
	Row row;
	float width,height;
	
	static Comparator<Char> compareChars = (Char ch1, Char ch2) ->
		ch1.y != ch2.y ? (int)(ch1.y-ch2.y) : (int) (ch1.x-ch2.x);
	
	public Char(String str,float x, float y, float width, float height, String fontname, PDColor color) {
		this.str=str;
		this.x=x;
		this.y=y;
		this.height=height;
		this.width=width;
		this.fontname=fontname;
		this.color=color;
		left=Math.round(x);
		right=(int)(Math.round(x+width-0.001));
		top=Math.round(y);
		bottom=(int)(Math.round(y+height-0.001));
	}

	public ArrayList<Char> getLeftConnected(Page page) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (left<1) return chars;
		
		Char ch=null;
		
		for (int j=top; j<=bottom; j++) {
			int i=left;
			for(int i1=1; i1 < height * Common._CharHGapRatio;i1++) {
				i=left-i1;
				if (i<0) break;
				if (page.bitmap.points[i][j]!=null) break;
			}
					
			if (i<0 || page.bitmap.points[i][j]==null) continue;
			if (ch==page.bitmap.points[i][j].ch) continue;
			
			ch=page.bitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}

	public ArrayList<Char> getRightConnected(Page page) {		
		ArrayList<Char> chars=new ArrayList<Char>();
		
		if (right >= page.width) return chars;
		
		Char ch=null;
		
		for (int j=top; j<=bottom; j++) {
			int i=right;
			for(int i1=1; i1 < height * Common._CharHGapRatio ; i1++) {
				i=right+i1;
				if (i>page.width) break;
				if (page.bitmap.points[i][j]!=null) break;
			}
			
			if (i>=page.width || page.bitmap.points[i][j]==null) continue;
			if (ch==page.bitmap.points[i][j].ch) continue;
			
			ch=page.bitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}
	
	static public class CharFont implements Comparable<CharFont>{
		private String name;
		private float height;
		private int bold;
		private int rgb;
		
		public CharFont(String name,float height,PDColor color) {
			this.name=name;
			this.height=height;
			try {
				rgb=color.toRGB();
			} catch (IOException e) {
				e.printStackTrace();
			}
					
			bold=Common.checkBold.check(name);
		}
		
		private int colorDiff() {
			int r = Math.abs((rgb >> 16) & 0x000000FF);
			int g = Math.abs((rgb >>8 ) & 0x000000FF);
			int b = Math.abs((rgb) & 0x000000FF);
			
			int max,min;
			
			if(r>=g) {
				max=r;
				min=g;
			} else {
				max=g;
				min=r;
			}
			
			if(max<b)
				max=b;
			if(min>b)
				min=b;
			
			return max-min;
		}
		
		private int value() {
			int i=(int) (height * 1000);
			
			if(bold>0)
				i=i + 400 * bold;	
					
	        // i=(i<<8) + colorDiff();
	         
	        return i;
		}
		
	    @Override
	    public int hashCode() {
	    	return (value() << 8) + (byte)name.hashCode();
	    	//return value();
	    }
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
	            return true;
	        if (obj == null)
	            return false;
	        if (getClass() != obj.getClass())
	            return false;
	        
	        CharFont other = (CharFont) obj;
	        
	        return hashCode()==other.hashCode();
		}
		
		@Override
	    public int compareTo(CharFont charfont) {
	        return hashCode()-charfont.hashCode();
	    }
		
		float getHeight() {
			return height;
		}
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
