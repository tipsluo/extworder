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

public class Row extends Rectangle {
	CharFont charfont;
	ArrayList<Char> chars;
	Block block;
	Page page;
	float width,height;
	int wordInterval;
	int spaceWidth;
	
	static Comparator<Row> compareRows = (Row r1, Row r2) ->
		r1.upper != r2.upper ? Common.compareValue(r1.upper,r2.upper) : Common.compareValue(r1.left,r2.left);
	
	public Row(Page page, int x, int y) {
		this.page=page;
		wordInterval=-1;
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
	
	public void build(int x,int y) {
		chars=new ArrayList<Char>();
		
		if ( page.pageBitmap.points[x][y].ch == null)
			return;
		
		wordInterval=Common._CharHGap;
		
		expand(page.pageBitmap.points[x][y].ch);
		
		if(spaceWidth>0)
			wordInterval=Common._CharHGapSpaceTimes * spaceWidth;
		else
			wordInterval=getWordInterval(chars);
		
		if(wordInterval==-1)
			wordInterval=Common._CharHGap;
		
		clear();
		
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
			
			ch.getLeftConnected(page,wordInterval).forEach(this::expand);
			ch.getRightConnected(page,wordInterval).forEach(this::expand);
		}
	}
	
	private int getWordInterval(ArrayList<Char> chs) {
		if(chars.size()<2)
			return -1;
		
		HashMap<Integer,Integer> intervals=new HashMap<Integer,Integer>();
		
		Collections.sort(chars,Char.compareChars);
		
		int r=chs.get(0).right;
		
		for(int i=1; i<chs.size(); i++) {
			Char ch=chs.get(i);
			
			int interval=ch.left-r;
			
			if(intervals.containsKey(interval))
				intervals.put(interval, intervals.get(interval)+1);
			else
				intervals.put(interval,1);
			
			r=ch.right;
		}
		
		Map<Integer, Integer> sortedMap = intervals.entrySet().stream()
		        .sorted(Comparator.comparingInt(e -> e.getValue()))
		        .collect(Collectors.toMap(
		                Map.Entry::getKey,
		                Map.Entry::getValue,
		                (a, b) -> { throw new AssertionError(); },
		                LinkedHashMap::new
		        ));
		Iterator<Map.Entry<Integer, Integer>> itr = sortedMap.entrySet().iterator();
		
		int i=0;
		int interval=-1;
		for(;itr.hasNext();i++) {
			interval=itr.next().getKey();
			if(i==1)
				break;
		}
		if(i<1)
			return -1;
		else
			return interval;
		
		
		/*List<Integer> arr=new ArrayList<>(intervals.keySet());
		
		if(arr.size()<2)
			return -1;
		
		return arr.get(1);*/
	}
	
	private void clear() {
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
	
	boolean separateUpperLeftBigChar() {
		if(chars.size()<=2)
			return false;
		
		Char ul=chars.get(0);
		
		ArrayList<Char> rights=ul.getRightConnected(page,wordInterval);
		
		if(rights.size()<2)
			return false;
		
		for(Char ch:chars)
			ch.row=null;
		
		
		int index=page.rows.indexOf(this);

		page.rows.remove(this);
		if(block!=null)
			block.rows.remove(this);
		
		ul.row=this; //set row temporarily so that it will not be expanded.
		
		for(Char ch:rights) {
			Row row=new Row(page,ch);
			
			if(index>=0) {
				page.rows.add(index,row);
				index=-1;
			} else
				page.rows.add(row);
		}
		
		ul.row=rights.get(0).row;
		ul.row.chars.add(ul);
		
		for(Char ch:rights)
			Collections.sort(ch.row.chars,Char.compareChars);
		
		return true;
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
