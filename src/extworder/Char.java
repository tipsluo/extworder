package extworder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Char extends Rectangle {
	String str;
	float x,y;
	String fontname;
	Row row;
	float width,height;
	
	static Comparator<Char> compareChars = (Char ch1, Char ch2) ->
		ch1.y != ch2.y ? Common.compareValue(ch1.y,ch2.y) : Common.compareValue(ch1.x,ch2.x);
		//ch1.y != ch2.y ? (int)(ch1.y-ch2.y) : (int) (ch1.x-ch2.x);
		
	public Char(String str,float x, float y, float width, float height, String fontname) {
		this.str=str;
		this.x=x;
		this.y=y;
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
		
		if (right >= page.width) return chars;
		
		Char ch=null;
		
		for (int j=upper; j<=lower; j++) {
			int i=right;
			for(int i1=1; i1 < height * Common._CharHGapRatio ; i1++) {
				i=right+i1;
				if (i>page.width) break;
				if (page.pageBitmap.points[i][j]!=null) break;
			}
			
			if (i>=page.width || page.pageBitmap.points[i][j]==null) continue;
			if (ch==page.pageBitmap.points[i][j].ch) continue;
			
			ch=page.pageBitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}
	
	static public class CharFont implements Comparable<CharFont>{
		String name;
		float height;
		int bold;
		//int rgb;
		
		public CharFont(String name,float height) {
			this.name=name;
			this.height=height;
			/*try {
				rgb=color.toRGB();
			} catch (IOException e) {
				e.printStackTrace();
			}*/
					
			bold=CheckBold.check(name);
		}
		
		/*private int colorDiff() {
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
		}*/
		
		private int value() {
			int i=(int) (height * 1000);
			
			if(bold>0)
				i=i + 400 * bold;	
					
	        return i;
		}
		
	    @Override
	    public int hashCode() {
	    	return value();
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

		final static class CheckBold {
			final static int _BOLD=2;
			final static int _SEMIBOLD=1;
			final static int _NOBOLD=0;
			
			static final Pattern LastPart;
			static final Pattern Bold;
			static final Pattern Semibold;
			
			static {
				LastPart=Pattern.compile("[\\.-](.*)$");
				Bold=Pattern.compile("Bold");
				Semibold=Pattern.compile("Semibold");
			}
			
			public static int check(String s) {
				Matcher m=LastPart.matcher(s);
				
				if(m.find()) {
					String lastPart=m.group(1);
					
					if(lastPart.equals("B"))
						return _BOLD;
					else {
						m=Bold.matcher(lastPart);
						if(m.find())
							return _BOLD;
						else {
							m=Semibold.matcher(lastPart);
							if(m.find())
								return _SEMIBOLD;
							else
								return _NOBOLD;
						}
					}
				}
				
				return _NOBOLD;
			}
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
