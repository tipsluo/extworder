package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.regex.Pattern;

import extworder.Char.CharFont;
import extworder.Common.AdditionalSubtitleFormatFilter;
import extworder.Common.BigBlockFilter;
import extworder.Common.SubtitleBlockFilter;
import extworder.Common.TextBlockFilter;
import extworder.Page.Column;

public class Block extends Rectangle {
	CharFont charfont;
	ArrayList<Row> rows;
	Page page;
	Column column;
	String type="";
    
	final static CompareBlocks compareBlocks=new CompareBlocks();
	final static TextBlockFilter textBlockFilter=new TextBlockFilter();
	final static BigBlockFilter bigBlockFilter=new BigBlockFilter();
	final static SubtitleBlockFilter subtitleBlockFilter=new SubtitleBlockFilter();
	static AdditionalSubtitleFormatFilter additionalSubtitleFormatFilter;
	
	public Block(Page page, int x, int y) {
		super();
		
		this.page=page;
		build(x,y);
		
		Collections.sort(rows,Row.compareRows);

		charfont=mostCharFont();
	}
	
	public Block(int left,int top,int right, int bottom) {
		super(left,top,right,bottom);
	}
	
	public void build(int x, int y) {
		rows=new ArrayList<Row>();
		
		if ( page.bitmap.points[x][y].ch != null )
			expand(page.bitmap.points[x][y].ch.row);
	}
	
	private void expand(Row row) {
		if (row.block==null) {
			rows.add(row); 
			row.block=this;
			
			updateRectangle(row);
			
			row.getAboveConnected().forEach(this::expand);
			row.getBelowConnected().forEach(this::expand);
		}	
	}
	
	void merge(Block block) {
		for (Row row:block.rows) {
			row.block=this;
			updateRectangle(row);
		}
		
		rows.addAll(block.rows);
		
		Collections.sort(rows,Row.compareRows);
		
		page.blocks.remove(block);
	}
	
	Block closestBlock() {
		Block block=null;
		float minDistance=999;
		
		for(Block block1 : page.blocks) {
			float dist=distance(block1);
			
			if(minDistance>dist) {
				minDistance=dist;
				block=block1;
			}
		}
		return block;
	}
	
	boolean isSimilar(Block block) {
		int allowedDisplace = (int) (Common._BlockDisplaceRatio * charfont.getHeight());
		
		if( Math.abs(left - block.left) <= allowedDisplace &&
				Math.abs(top - block.top) <= allowedDisplace &&
				Math.abs(right - block.right) < allowedDisplace &&
				Math.abs(bottom - block.bottom) < allowedDisplace )
			return true;
		return false;
	}
	
	public CharFont mostCharFont() {
		TreeMap<CharFont,Integer> charFonts=new TreeMap<>();
		
		for (Row row: rows) {
			int n=charFonts.compute(row.charfont, (k,v) -> (v == null ? 0 : v) + 1);
        	charFonts.put(row.charfont,n);
		}
		
		CharFont cf=charFonts.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return cf;
	}
	
	int indent() {
		int l;
		
		if(column==null) {
			l=column.left;
		} else {
			l=page.left;
		}
		
		return left-l;
	}
	
	int alignment() {
		int l,r;
		
		if(column==null) {
			l=column.left;
			r=column.right;
		} else {
			l=page.left;
			r=page.right;
		}
		
		int leftIndent=left-l;
		int rightIndent=r-right;
		
		if(leftIndent==rightIndent)
			return Common._CENTERALIGNED;
		else if(leftIndent==0)
			return Common._LEFTALIGNED;
		else if(rightIndent==0)
			return Common._RIGHTALIGNED;
		else
			return Common._NOALIGNED;
	}
	
	BlockFormat blockformat() {
		return new BlockFormat(this);
	}
	
	boolean isTextBlock() {
		int blockWidth=right-left+1;
		
		return charfont.equals(page.content.textCharfont) && 
				blockWidth >= page.content.lowColumnWidth &&
				blockWidth <= page.content.highColumnWidth;
	}
	
	boolean isTextInfinished() {
		if(! isTextBlock())
			return false;
			
		String lastStr=rows.get(rows.size()-1).string();
			
		return Common.infinishedTextBlock.matcher(lastStr.trim()).find();
	}
	
	public void print(FileWriter fw) throws IOException {
		fw.write("==============================\n");
		
		Content content=page.content;
		
		if (this==page.content.titleBlock)
			fw.write("type: title");
		else if (this==page.content.abstractBlock)
			fw.write("type: abstract");
		else if (!type.isBlank())
			fw.write(String.format("type: %s",type));
		else if (charfont.equals(content.textCharfont))
			fw.write(String.format("type: text"));
		else 
			fw.write(String.format("type: undefined"));
					
		fw.write(String.format("\ntypeindex=%d left=%d right=%d top=%d bottom=%d\n====>\n\n",
				content.charfontIndexes.get(charfont),left,right,top,bottom));
		
		int y=rows.get(0).bottom;
		for(Row row:rows) {
			if (row.top>y) {
				fw.write("\n");
				y=row.bottom;
			} else {
				fw.write(" ");
			}
			row.print(fw);
		}
		
		fw.write("\n==============================\n\n");
	}
	
	String string() {
		String str="";
		
		int y=rows.get(0).bottom;
		for(Row row:rows) {
			if (row.top>y) {
				str+="\n";
				y=row.bottom;
			}
			str+=row.string();
		}
		
		return str+"\n";
	}
	
	
	static class BlockFormat {
		private final CharFont charfont;
		private final int indent;
		private final int alignment;
		
		public BlockFormat(Block block) {
			charfont=block.charfont;
			indent=block.indent();
			alignment=block.alignment();
		}
		
		boolean equals(BlockFormat blockformat) {
			if(alignment==Common._CENTERALIGNED || 
					blockformat.alignment==Common._CENTERALIGNED)
				return charfont.equals(blockformat.charfont);
			else 
				return charfont.equals(blockformat.charfont) &&
						indent==blockformat.indent;
		}
		
		public int hashCode() {
	        int hash=charfont.hashCode() + (indent<<12) + (alignment<<24) ;
	        
	        return hash;
		}
	}
	
	static class CompareBlocks implements Comparator<Block> {
		public int compare(Block b1, Block b2) {
			if( ( b1.top >= b2.top && b1.top <= b2.bottom ) || 
					( b2.top >= b1.top && b2.top <= b1.bottom ) )
				return b1.left - b2.left;
			else
				return b1.top - b2.top;
		}
	}
}
