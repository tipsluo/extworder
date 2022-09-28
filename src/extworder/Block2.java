package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import extworder.Common.StatGroup;
import extworder.Row2.CharFont;

public class Block2  extends Rectangle {
	final Page2 page;
	Column2 column;
	ArrayList<Row2> rows;
	final float interval;
	String str;
	BlockFormat format;
	long value;
	
	public Block2(Row2 row) {
		this.page=row.page;
		
		this.rows=new ArrayList<Row2>();
		
		registerRow(row);
		
		interval=0;
		format=new BlockFormat(mostCharFont());
		str="";
		
		page.blockCandidates.addSortUniq(this);
	}
	
	public Block2(float interval, Block2 ...blocks) {
		this.page=blocks[0].page;
		
		this.rows=new ArrayList<Row2>();
		this.interval=interval;
	
		for(Block2 block:blocks)
			for(Row2 row:block.rows)
				registerRow(row);
		
		format=new BlockFormat(mostCharFont());
		Collections.sort(rows,Row2.compareRows);
		value=getValue();
		
		page.blockCandidates.addSortUniq(this);
	}
	
	public Block2(Row2 row1,Row2 row2) {
		page=row1.page;
		
		interval=row1.medium-row2.medium;
		
		registerRow(row1);
		registerRow(row2);
		
		format=new BlockFormat(mostCharFont());
		Collections.sort(rows,Row2.compareRows);
		value=getValue();
		
		page.blockCandidates.addSortUniq(this);
	}
	
	public long getValue() {
		int v=0;
		for(Row2 row:rows)
			v += row.value * row.chars.size();
		
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
		
		row.registerBlock(this);
	}
	
	boolean checkConflict(Block2 ...blocks) {
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
		
		Collections.sort(b1.rows, Row2.compareRows);
		Collections.sort(b2.rows, Row2.compareRows);
		
		float d1=b2.rows.get(0).medium - b1.rows.get(b1.rows.size()-1).medium;
		float d2=b1.rows.get(0).medium - b2.rows.get(b2.rows.size()-1).medium;
		
		if(d1>0)
			return d1;
		if(d2>0)
			return d2;
		
		return -1;
	}
	
	public void print(FileWriter fw) throws IOException {
		fw.write("==============================\n");
		
		/*if (this==page.content.titleBlock)
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
					
		fw.write(String.format("\ntypeindex=%d left=%d right=%d upper=%d lower=%d\n====>\n",
				page.content.blockformatIndexes.get(format),left,right,upper,lower));
		fw.write(String.format("charfont name=%s charfont height=%f, charfont bold=%d, alignment=%d, allupper=%d, column left=%d, likeBody=%d\n\n",
				format.charfont.name,format.charfont.height, format.charfont.bold, alignment(),format.allUppercase, columnLeft, likeBodyBlock1()));
		*/
		fw.write(string());
		
		fw.write("\n==============================\n\n");
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
}
