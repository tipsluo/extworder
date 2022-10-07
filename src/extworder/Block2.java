package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

import extworder.Common.StatGroup;
import extworder.Row2.CharFont;

public class Block2  extends Rectangle {
	final Page2 page;
	Page2.Column2 column;
	ArrayList<Row2> rows;
	final float interval;
	private String str;
	BlockFormat format;
	private long value;
	public String type="";
	boolean purge;
	
	static final float _FirstRowLeftIndentRatio=0.1f;
	static final float _RowRightDifferenceRatio=0.1f;
	
	static Comparator<Block2> compareBlockRowNumber = (Block2 b1,Block2 b2) ->
		b1.rows.size()>b2.rows.size() ? 1 : 
			b1.rows.size()==b2.rows.size() ? 0 : -1;
	static Comparator<Block2> compareBlockHashValue = (Block2 b1,Block2 b2) ->
		b1.hashValue>b2.hashValue ? 1 : 
			b1.hashValue==b2.hashValue ? 0 : -1;
	
	final static CompareBlocks compareBlocks=new CompareBlocks();
	final static BodyBlockFilter bodyBlockFilter=new BodyBlockFilter();
	//final static BigBlockFilter bigBlockFilter=new BigBlockFilter();
	public final static SubtitleBlockFilter subtitleBlockFilter=new SubtitleBlockFilter();
	
	final static float _BlockDisplaceRatio=1f;
	final static String _PageHeaderBlock="PAGEHEADER";
	final static String _PageFooterBlock="PAGEFOOTER";
	
	public Block2(Row2 row) {
		this.page=row.page;
		
		this.rows=new ArrayList<Row2>();
		
		registerRow(row);
		
		this.interval=0;
		format=new BlockFormat(mostCharFont());
		purge=false;
		render();
		if(page.blockCandidates.addSortUniq(this))
			row.registerBlock(this);
		else
			purge=true;
	}
	
	public Block2(float interval, Block2 ...blocks) {
		this.page=blocks[0].page;
		
		this.rows=new ArrayList<Row2>();
		this.interval=interval;
		
		for(Block2 block:blocks)
			for(Row2 row:block.rows)
				registerRow(row);
		purge=false;
		render();
		if(page.blockCandidates.addSortUniq(this))
			for(Block2 block:blocks)
				for(Row2 row:block.rows)
					row.registerBlock(this);
		else
			purge=true;
	}
	
	public Block2(Row2 row1,Row2 row2) {
		page=row1.page;
		
		interval=row1.medium-row2.medium;
		
		registerRow(row1);
		registerRow(row2);
		purge=false;
		render();
		if(page.blockCandidates.addSortUniq(this)) {
			row1.registerBlock(this);
			row2.registerBlock(this);
		} else
			purge=true;
	}
	
	public Block2(float interval, Block2 block, Row2 row) {
		this(interval,block);
		if(!purge) {
			addRow(row);
			render();
		}
	}
	
	public void addRow(Row2 row) {
		registerRow(row);
		row.registerBlock(this);
	}
	
	public void render() {
		format=new BlockFormat(mostCharFont());
		Collections.sort(rows,Row2.compareRows);
		renderString();
	}
	
	public long getValue() {
		if(rows.size()<=1)
			return -1;
		
		boolean leftInAlign=true;
		boolean rightInAlign=true;
		boolean centralInAlign=true; 
		float rightDifference=_RowRightDifferenceRatio * (right-left);
		float medium=(width)/2 + left;
		
		for(int i=0; i<rows.size(); i++) {
			Row2 row=rows.get(i);
			
			if(medium != (row.right-row.right)/2)
				centralInAlign=false;
			
			if(i==0) {
				if((row.left-left) > _FirstRowLeftIndentRatio * width)
					leftInAlign=false;
			} else {
				if(row.left-left >= 1)
					leftInAlign=false;
			}
			
			if(i==rows.size()-1)
				if(rows.size()>3)
					continue;
		
			if(right-row.right >= rightDifference)
				rightInAlign=false;
		}
		
		if(leftInAlign || rightInAlign || centralInAlign)
			return 0;
		
		long v=0;
		for(int i=0; i<rows.size(); i++) {
			Row2 row=rows.get(i);
			
			float diff=width-row.width;
			v+=diff*diff;
		}
		v*=rows.size();
		
		return v;
	}
	
	/*static ArrayList<Row2> checkRows(Block2 ...blocks) {
		ArrayList<Row2> output=new ArrayList<Row2>();
		
		for(Block2 block:blocks)
			output.addAll(block.rows);
		
		//Collections.sort(output,Row2.compareRows);
		
		for(int i=0; i<output.size(); i++) {
			Row2 row1=output.get(i);
			for(int j=i+1; j<output.size(); j++) {
				Row2 row2=output.get(j);
				
				int dis=row1.distance(row2);
				if(dis<=0)
					return null;
				
				if(row1.vIntersected(row2) && row1.differentRow(row2))
					return null;
			}
		}
		
		return output;
	}*/
	
	void registerRow(Row2 row) {
		rows.add(row);
		updateRectangle(row);
	}
	
	/*boolean checkConflict(Block2 ...blocks) {
		for(Block2 block1:blocks) {
			if(distance(block1)>0)
				continue;
			
			for(Row2 row:rows) {
				if(row.distance(block1)>0)
					continue;
				
				for(Row2 row1:block1.rows) {
					if(row==row1) continue;
					
					if(row.distance(row1)>0) continue;
					
					for(Char2 ch:row.chars)
						for(Char2 ch1: row1.chars)
							if(ch==ch1)
								return true;
				}
			}
		}
		
		return false;
	}*/
	
	boolean isSimilar(Block2 block, boolean verifyVertical) {
		if(rows.size() != block.rows.size())
			return false;
		
		String s1=renderString();
		String s2=block.renderString();
		
		if(s1.equals(s2))
			return true;
		
		if(! format.equals(block.format))
			return false;
		
		Pattern pattern=Pattern.compile("\\s+");
		long i1=pattern.matcher(s1).results().count();
		long i2=pattern.matcher(s2).results().count();
		if(i1!=i2)
			return false;
		
		int allowedDisplace = (int) (_BlockDisplaceRatio * format.charfont.height);
		
		if(verifyVertical) {
			if( Math.abs(left - block.left) <= allowedDisplace &&
					Math.abs(upper - block.upper) <= allowedDisplace &&
					Math.abs(right - block.right) < allowedDisplace &&
					Math.abs(lower - block.lower) < allowedDisplace )
				return true;
		} else {
			if( Math.abs(left - block.left) <= allowedDisplace &&
					Math.abs(right - block.right) < allowedDisplace )
				return true;
		}
		return false;
	}
	
	protected CharFont mostCharFont() {
		TreeMap<CharFont,Integer> charFonts=new TreeMap<>();
		
		for (Row2 row: rows) {
			int n=charFonts.compute(row.charfont, (k,v) -> (v == null ? 0 : v) + 1);
        	charFonts.put(row.charfont,n);
		}
		
		CharFont cf=charFonts.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return cf;
	}
	
	static class CompareBlocks implements Comparator<Block2> {
		public int compare(Block2 b1, Block2 b2) {
			if(b1.page!=b2.page)
				return b1.page.pid-b2.page.pid;
			
			int b1column= b1.column==null ? -1 : b1.column.left;
			int b2column= b2.column==null ? -1 : b2.column.left;
			
			return b1column!=b2column ? b1column-b2column :
						b1.upper!=b2.upper ? b1.upper-b2.upper :
							b1.left-b2.left;
		}
	}
	
	protected int indent() {
		int l;
		
		if(column!=null) {
			l=column.left;
		} else {
			l=page.left;
		}
		
		return left-l;
	}
	
	int alignment() {
		int ali;

		Rectangle parent = column==null ? page:column;
		
		ali=super.alignment(parent,page.content.centralAlignmentAdjustment,page.content);
		
		if(ali==Common._LEFTALIGNED && (rows.size()>1 && allRowsLongEnough()))
			ali=Common._FULLALIGNED;
		
		return ali;
	}
	
	boolean allRowsLongEnough() {
		if(rows.size()<2)
			return true;
		
		Row2 row0=rows.get(0);
		
		int n=rows.size()-1;
		for(int i=1;i<n;i++) {
			Row2 row=rows.get(i);
			if(!row0.isLongEnough(this,row))
				return false;
			row0=row;
		}
		
		return true;
	}
	
	public String renderString() {
		str="";
		str=string();
		return str;
	}
	
	private CharFont getCharFont() {
		if(rows.size()==0)
			return null;
		
		StatGroup<CharFont> charfonts=new StatGroup<CharFont>();
		
		for (Row2 row: rows) {
			charfonts.add(row.charfont);
		}
			
		return charfonts.maxByValue();
	}
	
	static float blockVDistance(Block2 b1, Block2 b2) {
		if(b1.vIntersected(b2))
			return -1;
		
		// Both rows must have been sorted
		
		float d1=b2.rows.get(0).medium - b1.rows.get(b1.rows.size()-1).medium;
		float d2=b1.rows.get(0).medium - b2.rows.get(b2.rows.size()-1).medium;
		
		if(d1>0)
			return d1;
		if(d2>0)
			return d2;
		
		return -1;
	}
	
	boolean containsAllRows(Block2 b) {
		if(contains(b)) {
			for(Row2 r2:b.rows) {
				boolean match=false;
				for(Row2 r1:rows)
					if(r1.hashValue==r2.hashValue) {
						match=true;
						break;
					}
				if(match)
					continue;
				else
					return false;
			}
		} else
			return false;
		
		return true;
	}
	
	public void print(FileWriter fw) throws IOException {
		fw.write("==============================\n");
		
		if (this==page.content.titleBlock)
			fw.write("type: title");
		else if (this==page.content.abstractBlock)
			fw.write("type: abstract");
		else if (!type.isBlank())
			fw.write(String.format("type: %s",type));
		
		int columnLeft;
		if(column==null)
			columnLeft=-1;
		else
			columnLeft=column.left;
					
		//fw.write(String.format("\ntypeindex=%d left=%d right=%d upper=%d lower=%d\n====>\n",
		//		page.content.blockformatIndexes.get(format),left,right,upper,lower));
		fw.write(String.format("charfont name=%s charfont height=%f, charfont bold=%d, type=%s, alignment=%d, allupper=%d, column left=%d\n\n",
				format.charfont.name,format.charfont.height, format.charfont.bold, type, alignment(),format.allUppercase, columnLeft));
		
		fw.write(string());
		
		fw.write("\n==============================\n\n");
	}
	
	static boolean overlap(Block2 block1, Block2 block2) {
		if(!block1.vIntersected(block2) && !block1.hIntersected(block2))
			return false;
		
		for(Row2 row1: block1.rows) {
			for(Row2 row2: block2.rows)
				if(row1.vIntersected(row2) && row1.hIntersected(row2)) {
					return true;
				}
		}
		
		return false;
	}
	
	static boolean overlap(Block2 block, Row2 row) {
		if(!block.vIntersected(row) && !block.hIntersected(row))
			return false;
		
		for(Row2 row1: block.rows) {
			if(row1.vIntersected(row) && row1.hIntersected(row)) {
				return true;
			}
		}
		
		return false;
	}
	
	/*static void removeBlock(Block2 block) {
		for(Row2 row:block.rows)
			row.blockCandidates.list.remove(block);
		
		block.page.blockCandidates.list.remove(block);
	}*/
	
	static void removeBlock(Page2 page, int index) {
		Block2 block=page.blockCandidates.list.get(index);
		for(Row2 row:block.rows)
			row.blockCandidates.list.remove(block);
		
		page.blockCandidates.list.remove(index);
	}
	
	public String string() {
		if(str!=null && str!="")
			return str;
		
		if(rows.size()==0)
			return "";
		
		str="";
		
		int y=rows.get(0).lower;
		for(Row2 row:rows) {
			if (row.upper>=y) {
				str+="\n";
				y=row.lower;
			}
			
			str+=row.string();
		}
		
		str=str.trim();
		return str;
	}
	
    @Override
    public int compareTo(Rectangle rect) {
    	long h1=hashValue();
    	long h2=((Block2) rect).hashValue();
    	
        if(h1>h2)
        	return 1;
        else if(h1<h2)
        	return -1;
        else
        	return interval > ((Block2) rect).interval ? 
        				1 : 
        				interval < ((Block2) rect).interval ?
        						-1 : 0;
    }
	
	protected long hashValue() {
		if(hashValue==0)
			hashValue=super.hashValue() + string().hashCode();
		return hashValue;
	}
	
	public boolean abbrOnly() {
		if(page.content.abbrPatterns==null)
			return false;
		
		String s=string();
		s.replace("\n"," ");
		s.replace("\r"," ");
		for(Pattern p:page.content.abbrPatterns)
			s=p.matcher(s).replaceAll("");
		s=s.replace(" ","");
		if(s.length()==0)
			return true;
		return false;
	}
	
	public boolean trivial() {
		return string().length()<=Common._TrivialBlockMaxLength;
	}
	
	public static class BlockFormat implements Comparable<BlockFormat> {
		public CharFont charfont;
		int alignment;
		int allUppercase;
		
		public BlockFormat() {
			super();
		}
		
		public BlockFormat(CharFont charfont, int indent, int alignment) {
			this.charfont=null;
			this.alignment=alignment;
			allUppercase=Common._NOTALLUPPERCASE;
		}
		
		public BlockFormat(Block2 block) {
			this(block.mostCharFont(), block.indent(), block.alignment());
		}
		
		public BlockFormat(BlockFormat bf) {
			this.charfont=new CharFont(bf.charfont);
			this.alignment=bf.alignment;
			this.allUppercase=bf.allUppercase;
		}
		
		public void update(Block2 block) {
			this.alignment=block.alignment();
			
			String s=block.renderString();
			if(Common.lowercaseExisting.matcher(s).find())
				this.allUppercase=Common._NOTALLUPPERCASE;
			else if (Common.uppercase.matcher(s).find())
				this.allUppercase=Common._ALLUPPERCASE;
			
			charfont=block.getCharFont();
		}
		
		public BlockFormat(CharFont charfont) {
			this.charfont=charfont;
			this.alignment=Common._UNKNOWNALIGNED;
			this.allUppercase=Common._NOTALLUPPERCASE;
		}
		
		public boolean similar(BlockFormat bf) {
			if(alignment==Common._UNKNOWNALIGNED ||
					bf.alignment==Common._UNKNOWNALIGNED)
				return false;
			
			boolean charfontSimilar=charfont.similar(bf.charfont);
					
			boolean alignmentSame=sameAlignment(bf);
					
			return charfontSimilar && alignmentSame && allUppercase==bf.allUppercase;
		}
		
		public boolean same(BlockFormat bf) {
			boolean charfontSame=charfont.equals(bf.charfont);
					
			boolean alignmentSame=sameAlignment(bf);
					
			return charfontSame && alignmentSame && allUppercase==bf.allUppercase;
		}
		
		public boolean sameAlignment(BlockFormat bf) {
			if(alignment==bf.alignment ||
					(alignment==Common._LEFTALIGNED && bf.alignment==Common._FULLALIGNED) ||
					(bf.alignment==Common._LEFTALIGNED && alignment==Common._FULLALIGNED) || 
					(alignment==Common._RIGHTALIGNED && bf.alignment==Common._FULLALIGNED) ||
					(bf.alignment==Common._RIGHTALIGNED && alignment==Common._FULLALIGNED))
				return true;
			
			return false;
		}
		
		@Override
		public boolean equals(Object obj) {
			return compareTo((BlockFormat)obj)==0;
		}
		
		@Override
		public int compareTo(BlockFormat blockformat) {
			return hashCode()-blockformat.hashCode();
		}
		
		@Override
		public int hashCode() {
			int hash=(int)( (charfont.value() * 2) + allUppercase) * 32 + alignment;
	        
	        return hash;
		}
	}

	public interface BlockFilter {
		public boolean filter(Block2 block);
	}
	
	public static class BodyBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block2 block) {
			if(block.abbrOnly())
				return false;
			if(block.column==null)
				return false;
			return block.type==Common._Body;
		}
	}
	
	/*static class BigBlockFilter implements BlockFilter {
		final static Pattern pattern;
		
		static {
			pattern=Pattern.compile("^"+Common._IgnoredBlockPrefix);
		}
		
		@Override
		public boolean filter(Block2 block) {
			if(block.abbrOnly() || block.trivial() || block.alignment()==Common._UNKNOWNALIGNED)
				return false;
			
			int charfontDiff=block.format.compareTo(block.page.content.bodyBlockformat);
			
			if(block.likeBodyBlock1()>=Common._ParaSentDefaultUno)
				return true;
			
			if(charfontDiff>=0 && block.likeTitleBlock()>=0)
				return true;
			
			return false;
		}
	}*/
	
	public static class SubtitleBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block2 block) {
			if(block.abbrOnly() || block.trivial())
				return false;
			return block.format.compareTo(block.page.content.bodyBlockformat) >= 0 &&
					block.type.contains(Common._SubtitlePrefix);
		}
	}
}
