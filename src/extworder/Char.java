package extworder;

import java.util.ArrayList;
import java.util.Comparator;

public class Char extends Rectangle {
	String str;
	float x,y;
	String fontname;
	Row row;
	float width,height;
	
	static Comparator<Char> compareChars = (Char ch1, Char ch2) ->
		ch1.y != ch2.y ? (int)(ch1.y-ch2.y) : (int) (ch1.x-ch2.x);
	
	public Char(String str,float x, float y, float width, float height, String fontname) {
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
		String name;
		float height;
		boolean bold;
		
		public CharFont(String name,float height) {
			this.name=name;
			this.height=height;
			this.bold=name.contains(".B");
		}
		
	    @Override
	    public int hashCode() {
	        return (int)( (height*100000 + 
	        				(bold ? 1 : 0) ) * 100 +
	        					(short)name.hashCode());
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
	        if (name != other.name || height !=other.height)
	            return false;
	        return true;
		}
		
		@Override
	    public int compareTo(CharFont charfont) {
	        return (int)(hashCode()-charfont.hashCode());
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
