package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import extworder.Common.RangeGroup;
import extworder.Common.RangeGroup.Range;

public class Row extends Rectangle {
	CharFont charfont;
	ArrayList<Char> chars;
	Block block;
	Page page;
	//RangeGroup.Range wordInterval;
	int charReach, wordReach;
	int spaceWidth;
	int vAdjustment;
	private String str="";
	
	static Comparator<Row> compareRows = (Row r1, Row r2) ->
		r1.upper != r2.upper ? Common.compareValue(r1.upper,r2.upper) : Common.compareValue(r1.left,r2.left);
	static Comparator<Row> compareRowHeights = (Row r1, Row r2) ->
		r1.height-r2.height ;
	
	/*public Row(Row from) {
		page=from.page;
		block=from.block;
		chars=new ArrayList<Char>();
		wordInterval=from.wordInterval;
		spaceWidth=from.spaceWidth;
		charfont=new CharFont(from.charfont.name,from.charfont.height);
		block=from.block;
	}*/
		
	public Row(Page page, Block block, int x, int y) {
		this.page=page;
		
		if(block!=null)
			this.block=block;
	
		spaceWidth=-1;
		buildTentative(x,y);
		width=right-left;
		height=lower-upper;
	}
	
	public Row(Page page, Block block, Char ch) {
		this(page,block,ch.left,ch.upper);
	}
	
	public void render() {
		Collections.sort(chars,Char.compareChars);
		str="";
		string();
		charfont=getCharFont();
	}
	
	public void addChar(Char ch) {
		chars.add(ch);
		ch.row=this;
		updateRectangle(ch);
	}
	
	public void buildTentative(int x,int y) {
		for(vAdjustment=Common._MaxCharVGapAdj; vAdjustment>=Common._MinCharVGapAdj; vAdjustment--)  {
			resetChars();
			build(x,y);

			if(Validation.verifyRow(this)) {
				break;
			}
		}
	}
	
	public void build(int x,int y) {
		chars=new ArrayList<Char>();
		
		if ( page.pageBitmap.points[x][y].ch == null)
			return;
		
		Char ch=page.pageBitmap.points[x][y].ch;
		
		charReach=wordReach=(int) (ch.height * Common._CharHGapRatio);
		
		expand(page.pageBitmap.points[x][y].ch);
		Collections.sort(chars,Char.compareChars);

		getWordInterval();

		resetChars();		
		expand(page.pageBitmap.points[x][y].ch);

		render();
	}
	
	private void expand(Char ch) {
		if (ch==null) return;
		
		if(ch.str.contains(" "))
			spaceWidth=(int) ch.width;
		
		if (ch.row==null) {
			chars.add(ch); 
			ch.row=this;
			
			updateRectangle(ch);
			
			ch.getLeftConnected(page,wordReach,vAdjustment).forEach(this::expand);
			ch.getRightConnected(page,wordReach,vAdjustment).forEach(this::expand);
		}
	}
	
	public void resetChars() {
		if(chars!=null)
			clearCharRows();
		chars=new ArrayList<Char>();
		resetRectangle();
	}
	
	private void getWordInterval() {
		if(spaceWidth>0) {
			charReach=spaceWidth;
			wordReach=(int)(spaceWidth + Common._CharHSpaceAddGap);
			return;
		}
			
		if(chars.size()==1) {
			charReach=wordReach=(int) (chars.get(0).height * Common._CharHGapRatio);
			return;
		}
		
		ArrayList<Integer> intervals=new ArrayList<Integer>();
		HashMap<Integer,Integer> intervalCounts=new HashMap<Integer,Integer>();
		
		Char ch1=chars.get(0);
		
		for(int i=1; i<chars.size(); i++) {
			Char ch=chars.get(i);

			if(!ch.vIntersected(ch1)) {
				ch1=ch;
				continue;
			}
			
			int interval=ch.left-ch1.right;
			
			if(interval<0)
				continue;
			
			if(! intervals.contains(interval))
				intervals.add(interval);
			
			intervalCounts.put(interval,intervalCounts.getOrDefault(interval,0)+1);
			
			ch1=ch;
		}
		
		Collections.sort(intervals);
		
		if(intervalCounts.size()==1) {
			charReach=intervals.get(0)+1;
			wordReach=intervals.get(intervals.size()-1)+1;
		}
			//return new Range(intervals.get(0)+1, intervals.get(intervals.size()-1)+1);
		
		List<Entry<Integer, Integer>> list = new ArrayList<>(intervalCounts.entrySet());
        list.sort(Entry.<Integer, Integer>comparingByValue().reversed());
        int i=0;
        int cInterval=-1;
        int wInterval=-1;
        for (Entry<Integer, Integer> entry : list) {
        	if(i==0)
        		cInterval=entry.getKey();
        	else if(i==1) {
        		wInterval=entry.getKey();
        		break;
        	}
        	i++;
        }
        
		if(wInterval==-1) {
			charReach=cInterval;
			wordReach=(int)(chars.get(0).height*Common._CharHGapRatio);
			//ret=new Range(cInterval,(int)(chars.get(0).height*Common._CharHGapRatio));
		} else {
			charReach=(int)Math.round(cInterval*Common._SpaceAdjustment);
			wordReach=(int)(wInterval+Common._CharHSpaceAddGap);
			//ret=new Range((int)Math.round(cInterval*Common._SpaceAdjustment),(int)(wInterval+Common._CharHSpaceAddGap));
		}
	}

	public void clearCharRows() {
		for(Char ch:chars)
			ch.row=null;
	}
	
	void mergeUpdateWidth(Row row) {
		for (Char ch:row.chars) {
			ch.row=this;
		}
		
		chars.addAll(row.chars);
		
		if(left>row.left)
			left=row.left;
		if(right<row.right)
			right=row.right;
		
		width=right-left;
		height=lower-upper;
		
		render();
		
		if (row.block!=null) {
			row.block.rows.remove(row);
		}
		page.rows.remove(row);
	}
	
	void reupdateRectangle() {
		for(Char ch:chars)
			updateRectangle(ch);
		
		width=right-left;
		height=lower-upper;
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
			
			if (j<0 || 
					page.pageBitmap.points[i][j]==null) 
				continue;
			
			if (row==page.pageBitmap.points[i][j].ch.row) continue;
			
			row=page.pageBitmap.points[i][j].ch.row;
			
			if(! charfont.equals(row.charfont))
				continue;
			
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
				if (page.pageBitmap.points[i][j]!=null) 
					break;
			}
			
			if(j>page.lower || 
					page.pageBitmap.points[i][j]==null) 
				continue;
			if (row==page.pageBitmap.points[i][j].ch.row) continue;
			
			row=page.pageBitmap.points[i][j].ch.row;
			
			if(! charfont.equals(row.charfont))
				continue;
			
			if( j1 > row.charfont.height*Common._CharVGapRatio ) continue;
			
			rows.add(row);
		}
		
		return rows;
	}
	
	int hDistance(Row row) {
		if(left>row.left)
			return left-row.right;
		else
			return row.left-right;
	}
	
	/*boolean joinUpperLeftBigChar() {
		boolean toRemove=false;
		
		Char ul=chars.get(0);
		
		//ArrayList<Char> rights=ul.getRightConnected(this.page,Common._CharHGap,0);
		ArrayList<Char> rights=ul.getRightConnected(this.page,(int)(ul.height*Common._CharHGapRatio),0);

		if(rights.size()<2)
			return false;
		
		ul.row=null;
		chars.remove(ul);
		reupdateRectangle();
		
		ArrayList<Row> rows=new ArrayList<Row>();
		for(Char r:rights)
			rows.add(r.row);
		Collections.sort(rows,Row.compareRows);
		
		if(rows.get(0)==this) {
			ul.row=this;
			chars.add(ul);
			left=ul.left;
			render();
			return false;
		}
			
		if(chars.size()==0)
			toRemove=true;
		
		//Collections.sort(chars,Char.compareChars);
		render();
		
		Row r1=rows.get(0);
		ul.row=r1;
		r1.chars.add(ul);
		r1.render();
		//Collections.sort(r1.chars,Char.compareChars);
		
		for(Row r:rows)
			r.left=ul.left;
		
		return toRemove;
	}*/
	
	/*private boolean checkSameBlock(Row row) {
		return charfont.equals(row.charfont);
	}*/
	
	boolean scarceInBlock() {
		if(isFull(page.content,block))
			return false;
		
		if(alignmentInFrame()!=Common._NOALIGNED)
			return false;
		
		return Common.scarceRow.matcher(string()).find();
	}
	
	int alignmentInFrame() {
		int a;
		if(block.column!=null) {
			a=super.alignment(block.column,page.content.centralAlignmentAdjustment);
		} else {
			a=super.alignment(page,page.content.centralAlignmentAdjustment);
		}
		
		return a;
	}
	
	int alignmentInBlock() {
		int a=super.alignment(block,page.content.centralAlignmentAdjustment);
		
		return a;
	}
	
	public boolean terminatedSentence() {
		String s=chars.get(chars.size()-1).str;
		
		return Common.terminated.matcher(s).find();
	}
	
	int firstParagraphLineInRect(Rectangle rect) {
		if( ! rightAligned(rect) && ! terminatedSentence())
			return Common._ParaSentDefaultFalse;
		
		int d=left-rect.left;
		if(d>0 && 
			d < charfont.height * Common._FirstLineIndentRatio)
			return Common._ParaSentDefaultTrue;
		
		if(d==0)
			return Common._ParaSentDefaultUno;
					
		return Common._ParaSentDefaultFalse;
	}
	
	int firstParagraphLine() {
		return firstParagraphLineInRect(block);
	}
	
	int lastParagraphLineInRect(Rectangle rect) {
		if(!leftAligned(rect) && firstParagraphLine()<100)
			return Common._ParaSentDefaultFalse;

		if(terminatedSentence())
			return Common._ParaSentDefaultTrue;
		else
			return Common._ParaSentUnoNoTerm;
	}
	
	int lastParagraphLine() {
		return lastParagraphLineInRect(block);
	}
	
	boolean isParaphaphLine() {
		return leftAligned(block) && rightAligned(block);
	}

	private CharFont getCharFont() {
		if(chars.size()==0)
			return null;
		
		TreeMap<CharFont,Integer> charFonts=new TreeMap<>();
		
		for (Char ch: chars) {
			CharFont cf=new CharFont(ch.fontname,ch.height);
			
			int n=charFonts.compute(cf, (k,v) -> (v == null ? 0 : v) + 1);
        	charFonts.put(cf,n);
		}
		
		CharFont cf=charFonts.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		
		return cf;
	}
	
	public void print(FileWriter fw) throws IOException {
		fw.write(string());
	}
	
	String string() {
		if(str!="")
			return str;
		
		Char ch0=chars.get(0);
	
		for(Char ch:chars) {
			if(charReach < ch.left-ch0.right) {
				str+=" ";
			}
			str+=ch.str;
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
