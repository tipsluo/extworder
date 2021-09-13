package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import extworder.Common.RangeGroup;
import extworder.Common.RangeGroup.Range;
import extworder.Common.StatGroup;

public class Row extends Rectangle {
	CharFont charfont;
	ArrayList<Char> chars;
	Block block;
	Page page;
	float width,height;
	RangeGroup.Range wordInterval;
	int spaceWidth;
	
	static Comparator<Row> compareRows = (Row r1, Row r2) ->
		r1.upper != r2.upper ? Common.compareValue(r1.upper,r2.upper) : Common.compareValue(r1.left,r2.left);
	
	public Row(Page page) {
		this.page=page;
	}
		
	public Row(Page page, int x, int y) {
		this.page=page;
		spaceWidth=-1;
		build(x,y);
		width=right-left;
		height=lower-upper;
	}
	
	public Row(Page page, Char ch) {
		this(page,ch.left,ch.upper);
		width=right-left;
		height=lower-upper;
	}
	
	public void addChar(Char ch) {
		chars.add(ch);
		ch.row=this;
		updateRectangle(ch);
	}
	
	public void build(int x,int y) {
		chars=new ArrayList<Char>();
		
		if ( page.pageBitmap.points[x][y].ch == null)
			return;
		
		wordInterval=new Range(Common._CharHGap);
		
		expand(page.pageBitmap.points[x][y].ch);
		
		if(spaceWidth>0)
			//wordInterval=new Range((int)((float)Common._CharHGapSpaceTimes * spaceWidth));
			wordInterval=new Range(Common._CharHSpaceAddGap+spaceWidth);
		else
			wordInterval=getWordInterval(chars);
		
		if(wordInterval==null)
			wordInterval=new Range(Common._CharHGap);
		
		clearCharRows();
		
		chars=new ArrayList<Char>();
		
		expand(page.pageBitmap.points[x][y].ch);
		
		Collections.sort(chars,Char.compareChars);

		charfont=getCharFont();
	}

	private void expand(Char ch) {
		if (ch==null) return;
		
		if(ch.str.contains(" "))
			spaceWidth=ch.right-ch.left+1;
		
		if (ch.row==null) {
			chars.add(ch); 
			ch.row=this;
			
			updateRectangle(ch);
			
			ch.getLeftConnected(page,
					Common._CharHSpaceAddGap+wordInterval.max).forEach(this::expand);
					//(int)((float)wordInterval.max*Common._CharHGapSpaceTimes)).forEach(this::expand);
			ch.getRightConnected(page,
					Common._CharHSpaceAddGap+wordInterval.max).forEach(this::expand);
					//(int)((float)wordInterval.max*Common._CharHGapSpaceTimes)).forEach(this::expand);
		}
	}
	
	private RangeGroup.Range getWordInterval(ArrayList<Char> chs) {
		if(chars.size()<2)
			return null;
		
		ArrayList<Integer> intervals=new ArrayList<Integer>();
		
		Collections.sort(chars,Char.compareChars);
		
		int r=chs.get(0).right;
		for(int i=1; i<chs.size(); i++) {
			Char ch=chs.get(i);
			
			int interval=ch.left-r;
			
			if(! intervals.contains(interval))
				intervals.add(interval);
			
			r=ch.right;
		}
		
		Collections.sort(intervals);
		
		RangeGroup rangeGroup=new RangeGroup(intervals);
		
		if(rangeGroup.ranges.size()<2)
			return null;
		else
			return rangeGroup.ranges.get(1);
	}
	
	public void clearCharRows() {
		for(Char ch:chars)
			//if(ch.row==this)
				ch.row=null;
	}
	
	void merge(Row row) {
		for (Char ch:row.chars) {
			ch.row=this;
			updateRectangle(ch);
		}
		
		chars.addAll(row.chars);
		
		Collections.sort(chars,Char.compareChars);

		charfont=getCharFont();
		
		if (row.block!=null)
			row.block.rows.remove(row);
		page.rows.remove(row);
	}
	
	ArrayList<Row> separateCloseRows() {
		ArrayList<Row> newRows=new ArrayList<Row>();
		
		ArrayList<Stretch> stretches=new ArrayList<Stretch>();
		ArrayList<Stretch> separated=new ArrayList<Stretch>();
		ArrayList<Stretch> allCharStretches=new ArrayList<Stretch>();
		
		for(Char ch:chars) {
			Stretch stretch=new Stretch(ch);
			
			if(! stretches.contains(stretch))
				stretches.add(stretch);
			
			allCharStretches.add(stretch);
		}
		
		for(Stretch stretch1:stretches) {
			boolean intersected=false;
			for(Stretch stretch2:stretches) {
				if(stretch1.isIntersected(stretch2)) {
					intersected=true;
					//inseperated.add(stretch1);
					break;
				}
				if(! intersected)
					separated.add(stretch1);
			}
		}
		
		for(Stretch stretch:separated) {
			Row row=new Row(page);
			newRows.add(row);
		}
		
		for(int i=0; i<chars.size(); i++) {
			Char ch=chars.get(i);
			Stretch stretch=allCharStretches.get(i);
			int interLength=-1;
			int interIndex=-1;
			
			for(int j=0; j<separated.size(); j++) {
				Stretch separatedStretch=separated.get(j);
				if(separatedStretch.equals(stretch)) {
					newRows.get(j).addChar(ch);
					break;
				} else {
					int l=separatedStretch.intersection(stretch).length();
					if(l > interLength) {
						interLength=l;
						interIndex=j;
					}
				}
			}
			if(interIndex>=0)
				newRows.get(interIndex).addChar(ch);
		}
		
		for(Row row:newRows)
			Collections.sort(row.chars,Char.compareChars);
			
		return newRows;
	}
	
	public ArrayList<Row> getAboveConnected() {		
		ArrayList<Row> rows=new ArrayList<Row>();
		
		if (upper<1) return rows;
		
		Row row=null;
		
		for (int i=left; i<=right; i++) {
			int j=1;
			int j1=1;
			float maxDist=charfont.height*Common._CharVGapRatio;
			for (;j1<maxDist;j1++) {
				j=upper-j1;
				if (j<0) break;
				if (page.pageBitmap.points[i][j]!=null) break;
			}
			
			if (j<0 || page.pageBitmap.points[i][j]==null) continue;
			
			if (row==page.pageBitmap.points[i][j].ch.row) continue;
			
			row=page.pageBitmap.points[i][j].ch.row;
			
			if(!checkSameBlock(row)) continue;
			
			if( j1 > row.charfont.height*Common._CharVGapRatio ) continue;
			
			rows.add(row);
		}
		
		return rows;
	}

	public ArrayList<Row> getBelowConnected() {		
		ArrayList<Row> rows=new ArrayList<Row>();
		
		if (lower >= page.lower) return rows;
		
		Row row=null;
		
		for (int i=left; i<=right; i++) {
			int j=1;
			int j1=1;
			float maxDist=charfont.height*Common._CharVGapRatio;
			for (;j1<maxDist;j1++) {
				j=lower+j1;
				if (j>=page.lower) break;
				if (page.pageBitmap.points[i][j]!=null) break;
			}
			
			if(j>page.lower || page.pageBitmap.points[i][j]==null) continue;
			if (row==page.pageBitmap.points[i][j].ch.row) continue;
			
			row=page.pageBitmap.points[i][j].ch.row;
			
			if(!checkSameBlock(row))
				continue;
			
			if( j1 > row.charfont.height*Common._CharVGapRatio ) continue;
			
			rows.add(row);
		}
		
		return rows;
	}
	
	private boolean checkSameBlock(Row row) {
		return charfont.equals(row.charfont);
	}
	
	private CharFont getCharFont() {
		if(chars.size()==0)
			return null;
		
		TreeMap<CharFont,Integer> charFonts=new TreeMap<>();
		
		for (Char ch: chars) {
			CharFont cf=new CharFont(ch.fontname,ch.height);
			
			if(cf.equals(page.content.textCharfont))
				return cf;
			
			int n=charFonts.compute(cf, (k,v) -> (v == null ? 0 : v) + 1);
        	charFonts.put(cf,n);
		}
		
		CharFont cf=charFonts.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		
		return cf;
	}
	
	public void print(FileWriter fw) throws IOException {
		int x0=chars.get(0).right;
		for(Char ch:chars) {
			if(spaceWidth<0 &&
					ch.left > x0 + ch.width * Common._HSpaceMin) {
				fw.write(" ");
			}
			fw.write(ch.str);
			x0=ch.right;
		}
	}
	
	String string() {
		String str="";
		
		Char ch0=chars.get(0);
		for(Char ch:chars) {
			float h=Math.max(ch0.height,ch.height);
			
			if(spaceWidth<0 &&
					ch.left > ch0.right + h * Common._HSpaceMin) {
				str+=" ";
			}
			str+=ch.str;
			//x0=ch.right;
			ch0=ch;
		}
		
		return str;
	}
	
	
	static public class CharFont implements Comparable<CharFont>{
		String name;
		float height;
		int bold;
		
		public CharFont(String name,float height) {
			this.name=name;
			this.height=height;
					
			bold=CheckBold.check(name);
		}
		
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
}
