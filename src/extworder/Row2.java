package extworder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import extworder.Common.SortedList;
import extworder.Common.StatGroup;

public class Row2 extends Rectangle {
	CharFont charfont;
	ArrayList<Char2> chars;
	Block2 block;
	SortedList<Block2> blockCandidates;
	Page2 page;
	float interval=-1;
	float medium=-1;
	private long value=-1;
	private String str="";
	int spaceWidth=-1;
	
	static Comparator<Row2> compareRows = (Row2 r1, Row2 r2) ->
		r1.upper != r2.upper ? Common.compareValue(r1.upper,r2.upper) : Common.compareValue(r1.left,r2.left);
	static Comparator<Row2> compareRowHeights = (Row2 r1, Row2 r2) ->
		(int)(r1.height-r2.height) ;
	static Comparator<Row2> compareRowLefts = (Row2 r1, Row2 r2) ->
		r1.left != r2.left ? Common.compareValue(r1.left,r2.left) : Common.compareValue(r1.upper,r2.upper);
	
	public Row2() {
		super();

		chars=new ArrayList<Char2>();
	}
	
	public Row2(Char2 ch, Page2 page, float interval) {
		chars=new ArrayList<Char2>();
		blockCandidates=new SortedList<Block2>();
		this.page=page;
		this.interval=interval;
		
		setCharFont(ch);
		
		SortedList<Char2> sortChars=new SortedList<Char2>();
		sortChars.list.add(ch);
		searchLeftestChar(ch,interval,sortChars);
		searchRightestChar(ch,interval,sortChars);
		
		for(Char2 ch1:sortChars.list)
			registerChar(ch1);
		
		Collections.sort(chars,Char2.compareChars);
		
		value=getValue();
		
		if(page.rowCandidates.addSortUniq(this))
			ch.registerRow(this);
	}
	
	void registerChar(Char2 ch) {
		chars.add(ch);
		updateRectangle(ch);
	}
	
	void registerBlock(Block2 block) {
		if(blockCandidates.addSortUniq(block))
			for(Char2 ch:chars)
				ch.registerBlock(block);
	}
	
	public String render() {
		Collections.sort(chars,Char2.compareChars);
		
		getSpaceWidth();
			
		str="";
		string();
		return str;
	}
	
	private void getSpaceWidth() {
		if(chars.size()<2) {
			spaceWidth=999;
			return;
		}
			
		StatGroup<Integer> intervals=new StatGroup<Integer>();
		Char2 ch0=chars.get(0);
		for(int i=1; i<chars.size(); i++) {
			Char2 ch1=chars.get(i);
			if(ch1.str.contains(" ")) {
				spaceWidth=(int) ch1.width;
				return;
			}

			intervals.add(ch1.left-ch0.right);
			
			ch0=ch1;
		}
		
		if(intervals.records.size()<2) {
			spaceWidth=999;
			return;
		}
		
		spaceWidth=intervals.topsByValue(2).get(1);
	}
	
	long getValue() {
		if(value>=0)
			return value;
			
		if(chars.size()<=1) {
			value=999999999;
			return value;
		}
		
		int aveInterval=0;
		int sumInterval=0;
		ArrayList<Integer> intervals=new ArrayList<Integer>();
		
		Char2 ch0=chars.get(0);
		for(int i=1; i<chars.size(); i++) {
			Char2 ch1=chars.get(i);
			int i1=ch1.left-ch0.right;
			sumInterval+=i1;
			intervals.add(i1);
			ch0=ch1;
		}
		aveInterval=Math.round(sumInterval/chars.size());
		
		int sumDiffSqrt=0;
		for(Integer i: intervals) {
			int diff=i-aveInterval;
			sumDiffSqrt+=diff*diff;
		}
		
		value=Math.abs(sumDiffSqrt / intervals.size());
		return value;
	}
	
	/*void setActive() {
		for(Char2 ch:chars) {
			ch.row=this;
		}
	}*/
	
	public String string() {
		if(str!=null && str!="")
			return str;
		
		Char2 ch0=chars.get(0);
	
		for(Char2 ch:chars) {
			if(spaceWidth <= ch.left-ch0.right) {
				str+=" ";
			}
			str+=ch.str;
			ch0=ch;
		}
		
		return str;
	}
	
	private void searchLeftestChar(Char2 ch, float interval, SortedList<Char2> sortedChars) {
		List<Char2> ls=ch.getLeftConnected(page, interval, 0);

		for(Char2 ch1:ls) {
			if(ch1.height>charfont.height) {
				setCharFont(ch1);
			}
			
			if(! differentRow(ch1)) {
				sortedChars.addSortUniq(ch1);
				searchLeftestChar(ch1,interval,sortedChars);
			}
		}
	}
	
	private void searchRightestChar(Char2 ch, float interval, SortedList<Char2> sortedChars) {
		List<Char2> rs=ch.getRightConnected(page, interval, 0);
		
		for(Char2 ch1:rs) {
			if(ch1.height>charfont.height) {
				setCharFont(ch1);
			}
			
			if(! differentRow(ch1)) {
				sortedChars.addSortUniq(ch1);
				searchRightestChar(ch1,interval, sortedChars);
			}
		}
	}
	
	private void setCharFont(Char2 ch) {
		charfont=new CharFont(ch.font.getName(),ch.height);
		medium=(ch.lower-ch.upper)/2f + ch.upper;
	}
	
	boolean differentRow(Char2 ch) {
		if(ch.height==charfont.height)
			return (ch.lower-ch.upper)/2f + ch.upper != medium;
		else
			return false;
	}
	
	boolean differentRow(Row2 row) {
		if(charfont.height==row.charfont.height)
			return medium != row.medium;
		else
			return false;
	}
	
	SortedList<Row2> getAllAboveCandidates(float maxDist) {		
		SortedList<Row2> rows=new SortedList<Row2>();
		
		if (upper<1) return rows;
		
		for (int i=left; i<=right; i++) {
			int j=1;
			int j1=1;
			for (;j1<maxDist;j1++) {
				j=upper-j1;
				if (j<0) break;
				if (page.pageBitmap.points[i][j]!=null) break;
			}
			
			if (j<0 || 
					page.pageBitmap.points[i][j]==null) 
				continue;
			
			if (page.pageBitmap.points[i][j].ch==null ) continue;
			
			rows.addSortUniq(page.pageBitmap.points[i][j].ch.rowCandidates);
		}
		
		return rows;
	}
	
	SortedList<Row2> getAllBelowCandidates(float maxDist) {		
		SortedList<Row2> rows=new SortedList<Row2>();
		
		if (lower >= page.lower) return rows;
		
		for (int i=left; i<=right; i++) {
			int j=1;
			int j1=1;
			for (;j1<maxDist;j1++) {
				j=lower+j1;
				if (j>=page.lower) break;
				if (page.pageBitmap.points[i][j]!=null) 
					break;
			}
			
			if(j>page.lower || 
					page.pageBitmap.points[i][j]==null) 
				continue;
			
			if (page.pageBitmap.points[i][j].ch==null) continue;
			
			rows.addSortUniq(page.pageBitmap.points[i][j].ch.rowCandidates);
		}
		
		return rows;
	}
	
    @Override
    public int compareTo(Rectangle rect) {
    	long h1=hashValue();
    	long h2=rect.hashValue();
    	
        if(h1>h2)
        	return 1;
        else if(h1<h2)
        	return -1;
        else
        	return interval > ((Row2) rect).interval ? 
        				1 : 
        				interval < ((Row2) rect).interval ?
        						-1 : 0;
    }
	 
	/*private CharFont getCharFont() {
		if(chars.size()==0)
			return null;
		
		StatGroup<CharFont> charfonts=new StatGroup<CharFont>();
		
		for (Char2 ch: chars) {
			CharFont cf=new CharFont(ch.font.getName(),ch.height);
			
			charfonts.add(cf);
		}
			
		return charfonts.maxByValue();
	}*/
	
	static public class CharFont implements Comparable<CharFont>{
		String name;
		public float height;
		int bold;
		
		public CharFont(String name,float height) {
			this.name=name;
			this.height=height;
					
			bold=CheckBold.check(name);
		}
		
		public CharFont(CharFont cf) {
			this.name=cf.name;
			this.height=cf.height;
			bold=cf.bold;
		}
		
		public boolean similar(CharFont cf) {
			return Math.abs(value()-cf.value())<=Char2._MaxSameCharFontHeightDiff;
		}
		
		public float value() {
			float f=height;
			
			if(bold>0)
				f=f + bold * Char2._BoldCharFontValue;
					
	        return f;
		}
		
	    @Override
	    public int hashCode() {
	    	return (int)(value()*4096) + (name.hashCode()>>20);
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
	        
	        return value()==other.value();
		}
		
		public boolean allEquals(CharFont charfont) {
			return name==charfont.name && height==charfont.height;
		}
		
		@Override
	    public int compareTo(CharFont charfont) {
	        return (int)(value()-charfont.value());
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
	
	static public class VirtualRow extends Row2 {
		public VirtualRow(ArrayList<Char2> vcs) {
			for(Char2 vc:vcs) {
				vc.row=this;
				vc.updateRectangle(vc);
			}
		}
	}
}
