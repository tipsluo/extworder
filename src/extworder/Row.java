package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	float width,height;
	RangeGroup.Range wordInterval;
	int spaceWidth;
	int vAdjustment;
	
	static Comparator<Row> compareRows = (Row r1, Row r2) ->
		r1.upper != r2.upper ? Common.compareValue(r1.upper,r2.upper) : Common.compareValue(r1.left,r2.left);
	
	public Row(Row from) {
		page=from.page;
		block=from.block;
		chars=new ArrayList<Char>();
		wordInterval=from.wordInterval;
		spaceWidth=from.spaceWidth;
		charfont=new CharFont(from.charfont.name,from.charfont.height);
		block=from.block;
	}
		
	public Row(Page page, Block block, int x, int y) {
		this.page=page;
		
		if(block!=null)
			this.block=block;
	
		spaceWidth=-1;
		//build(x,y);
		buildTentative(x,y);
		width=right-left;
		height=lower-upper;
	}
	
	public Row(Page page, Block block, Char ch) {
		this(page,block,ch.left,ch.upper);
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
		
		//wordInterval=new Range(Common._CharHGap);
		wordInterval=new Range((int) (ch.height * Common._CharHGapRatio));
		
		expand(page.pageBitmap.points[x][y].ch);
		Collections.sort(chars,Char.compareChars);

		wordInterval=getWordInterval();

		resetChars();		
		expand(page.pageBitmap.points[x][y].ch);
		Collections.sort(chars,Char.compareChars);

		charfont=getCharFont();
		wordInterval=getWordInterval();
	}
	
	private void expand(Char ch) {
		if (ch==null) return;
		
		if(ch.str.contains(" "))
			spaceWidth=(int) ch.width;
		
		if (ch.row==null) {
			chars.add(ch); 
			ch.row=this;
			
			updateRectangle(ch);
			
			ch.getLeftConnected(page,
					Common._CharHSpaceAddGap+wordInterval.max,
					vAdjustment).forEach(this::expand);
			ch.getRightConnected(page,
					Common._CharHSpaceAddGap+wordInterval.max,
					vAdjustment).forEach(this::expand);
		}
	}
	
	private void resetChars() {
		if(chars!=null)
			clearCharRows();
		chars=new ArrayList<Char>();
		resetRectangle();
		width=height=-1;
	}
	
	private RangeGroup.Range getWordInterval() {
		Range ret;
		
		if(spaceWidth>0)
			return new Range(spaceWidth,Common._CharHSpaceAddGap+spaceWidth);
		
		if(chars.size()==1)
			//return new Range(Common._CharHGap);
			return new Range((int) (chars.get(0).height * Common._CharHGapRatio));
		
		ArrayList<Integer> intervals=new ArrayList<Integer>();
		
		Char ch1=chars.get(0);
		
		for(int i=1; i<chars.size(); i++) {
			Char ch=chars.get(i);

			if(!ch.isVIntersected(ch1))
				continue;
			
			int interval=ch.left-ch1.right;
			
			if(interval<0) 
				continue;
			// The internal could be less than 0 because of the upper/lower signs.
			
			if(! intervals.contains(interval))
				intervals.add(interval);
			
			ch1=ch;
		}
		
		Collections.sort(intervals);
		
		RangeGroup rangeGroup=new RangeGroup(intervals);

		if(rangeGroup.ranges.size()<2)
			ret=new Range(2,(int)(chars.get(0).height*Common._CharHGapRatio));
			//ret=new Range(1,Common._CharHGap);
		else {
			ret=new Range(rangeGroup.ranges.get(0).max+1, rangeGroup.ranges.get(1).max+1);
		}
		
		
		return ret;
	}

	public void clearCharRows() {
		for(Char ch:chars)
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
	
	void reupdateRectangle() {
		for(Char ch:chars)
			updateRectangle(ch);
		
		width=right-left;
		height=lower-upper;
	}
	
	/*ArrayList<Row> separateCloseRows() {
		ArrayList<Row> newRows=new ArrayList<Row>();
		
		if(chars.size()<2)
			return null;
		
		ArrayList<VStretch> vStretches=new ArrayList<VStretch>();
		ArrayList<VStretch> shared2=new ArrayList<VStretch>();
		ArrayList<VStretch> allCharVStretches=new ArrayList<VStretch>();
		ArrayList<Stretch> newStretches=new ArrayList<Stretch>();
		
		for(Char ch:chars) {
			VStretch vStretch=new VStretch(ch);
			
			allCharVStretches.add(vStretch);
		}
		allCharVStretches.set(0,new VStretch(allCharVStretches.get(1).copy()));
		
		for(VStretch vStretch: allCharVStretches) {
			if(! vStretches.contains(vStretch))
				vStretches.add(vStretch);
		}
		
		// Remove duplicate
		for(int i=0;i<vStretches.size();i++) {
			VStretch vStretch1=vStretches.get(i);
			for(VStretch vStretch2:vStretches) {
				if(vStretch1==vStretch2)
					continue;
				if(vStretch2.contains(vStretch1) || vStretch1.contains(vStretch2)) {
					vStretches.add(new VStretch(vStretch1.add(vStretch2)));
					vStretches.remove(vStretch1);
					vStretches.remove(vStretch2);
					
					i--;
					break;
				}
			}
		}
		
		if(vStretches.size()<2)
			return null;
		
		for(int i=0; i<vStretches.size(); i++) {
			VStretch vStretch1=vStretches.get(i);
			ArrayList<VStretch> intersected=new ArrayList<VStretch>();
			for(VStretch vStretch2:vStretches) {
				if(vStretch1==vStretch2)
					continue;
				if(vStretch1.isIntersected(vStretch2))
					intersected.add(vStretch2);
			}
			if(intersected.size()==0) {
				newStretches.add(vStretch1);
				vStretches.remove(vStretch1);
				i--;
			} else if(intersected.size()>=2) {
				shared2.add(vStretch1);
			}
		}
		
		for(int i=0;i<shared2.size();i++) {
			VStretch vStretch1=shared2.get(i);
			
			VStretch vStretch=null;
			int interLength=-1;
			for(int j=0;j<vStretches.size();j++) {
				VStretch vStretch2=vStretches.get(j);
				if(vStretch2==vStretch1) {
					continue;
				}
				Stretch intersection=vStretch1.intersection(vStretch2);
				if(intersection!=null) {
					int l=intersection.length();
					if(l>interLength) {
						interLength=l;
						vStretch=vStretch2;
					}
				}
			}
			
			Stretch newVStretch=vStretch1.add(vStretch);
			newStretches.add(newVStretch);
		}
		
		// for those only have one intersection:
		for(int i=0; i<vStretches.size(); i++) {
			Stretch stretch1=vStretches.get(i);
			if(shared2.contains(stretch1))
				continue;
			for(VStretch vStretch2:vStretches) {
				if(stretch1==vStretch2)
					continue;
				if(stretch1.isIntersected(vStretch2)) {
					Stretch newStretch=stretch1.add(vStretch2);
					newStretches.add(newStretch);
					vStretches.remove(stretch1);
					i--;
					break;
				}
			}
		}
			
		// Remove duplicate
		for(int i=0;i<newStretches.size();i++) {
			Stretch stretch1=newStretches.get(i);
			for(Stretch stretch2:newStretches) {
				if(stretch1==stretch2)
					continue;
				if(stretch2.contains(stretch1) || stretch1.contains(stretch2)) {
					newStretches.add(stretch1.add(stretch2));
					newStretches.remove(stretch1);
					newStretches.remove(stretch2);
					
					i--;
					break;
				}
			}
		}
		
		if(newStretches.size()<2)
			return null;
		
		for(int i=0; i<newStretches.size(); i++) {
			Row row=new Row(this);
			newRows.add(row);
		}
		
		for(int i=0; i<chars.size(); i++) {
			Char ch=chars.get(i);
			VStretch vStretch=allCharVStretches.get(i);
			
			for(int j=0; j<newStretches.size(); j++) {
				Stretch separatedVStretch=newStretches.get(j);
				if(separatedVStretch.contains(vStretch)) {
					newRows.get(j).addChar(ch);
					break;
				}
			}
		}
		
		for(int i=0;i<newRows.size();i++) {
			Row row=newRows.get(i);
			if(row.chars.size()==0) {
				newRows.remove(row);
				i--;
				continue;
			}
				
			row.charfont=row.getCharFont();
			Collections.sort(row.chars,Char.compareChars);
		}
			
		return newRows;
	}*/
	
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
				if (page.pageBitmap.points[i][j]!=null) 
					break;
			}
			
			if(j>page.lower || 
					page.pageBitmap.points[i][j]==null) 
				continue;
			if (row==page.pageBitmap.points[i][j].ch.row) continue;
			
			row=page.pageBitmap.points[i][j].ch.row;
			
			if(!checkSameBlock(row))
				continue;
			
			if( j1 > row.charfont.height*Common._CharVGapRatio ) continue;
			
			rows.add(row);
		}
		
		return rows;
	}
	
	boolean joinUpperLeftBigChar() {
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
			Collections.sort(chars,Char.compareChars);
			return false;
		}
			
		if(chars.size()==0)
			toRemove=true;
		
		Collections.sort(chars,Char.compareChars);
		
		Row r1=rows.get(0);
		ul.row=r1;
		r1.chars.add(ul);
		Collections.sort(r1.chars,Char.compareChars);
		for(Row r:rows)
			r.left=ul.left;
		
		return toRemove;
	}
	
	private boolean checkSameBlock(Row row) {
		return charfont.equals(row.charfont);
	}
	
	boolean scarce() {
		if(isFull(page.content,block.column))
			return false;
		
		if(alignment()!=Common._NOALIGNED)
			return false;
		
		return Common.scarceRow.matcher(string()).find();
	}
	
	int alignment() {
		if(block.column!=null)
			return super.alignment(block.column,page.content.centralAlignmentAdjustment);
		else
			return super.alignment(page,page.content.centralAlignmentAdjustment);
	}

	
	private CharFont getCharFont() {
		if(chars.size()==0)
			return null;
		
		TreeMap<CharFont,Integer> charFonts=new TreeMap<>();
		
		for (Char ch: chars) {
			CharFont cf=new CharFont(ch.fontname,ch.height);
			
			/*if(cf.equals(page.content.textCharfont))
				return cf;*/
			
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
		String str="";
		
		Char ch0=chars.get(0);
	
		for(Char ch:chars) {
			if(wordInterval.min<ch.left-ch0.right) {
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
