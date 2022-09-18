package extworder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import extworder.Block.BlockFormat;
import extworder.Common.StatGroup;
import extworder.Row2.CharFont;

public class Block2  extends Rectangle {
	final Page2 page;
	Column2 column;
	ArrayList<Row2> rows;
	final int interval;
	String str;
	BlockFormat format;
	
	public Block2(Row2 row) {
		this.page=row.page;
		
		this.rows=new ArrayList<Row2>();
		
		registerRow(row);
		
		interval=0;
		format=new BlockFormat(mostCharFont());
		
		page.blocks.add(this);
	}
	
	public Block2(int interval, Block2 ...blocks) {
		this.page=blocks[0].page;
		
		this.rows=new ArrayList<Row2>();
		this.interval=interval;
	
		for(Row2 row: rows)
			registerRow(row);
		
		format=new BlockFormat(mostCharFont());
		
		page.blocks.add(this);
	}
	
	void registerRow(Row2 row) {
		rows.add(row);
		row.blocks.add(this);
		updateRectangle(row);
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
	
	private CharFont getCharFont() {
		if(rows.size()==0)
			return null;
		
		StatGroup<CharFont> charfonts=new StatGroup<CharFont>();
		
		for (Row2 row: rows) {
			charfonts.add(row.charfont);
		}
			
		return charfonts.maxByValue();
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

}
