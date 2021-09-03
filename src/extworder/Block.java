package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.regex.Pattern;

import extworder.Block.BlockFormat;
import extworder.Common.AdditionalSubtitleFormatFilter;
import extworder.Common.BigBlockFilter;
import extworder.Common.SubtitleBlockFilter;
import extworder.Common.TextBlockFilter;
import extworder.Page.Column;
import extworder.Row.CharFont;

public class Block extends Rectangle {
	BlockFormat format;
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
		
		format=new BlockFormat(mostCharFont());
	}
	
	public Block(int left,int upper,int right, int lower) {
		super(left,upper,right,lower);
	}
	
	public Block(Page page, Column column, ArrayList<Row> rows) {
		super();
		
		this.page=page;
		this.column=column;
		this.rows=rows;
		
		for(Row row:rows) {
			updateRectangle(row);
			row.block=this;
		}
		
		Collections.sort(rows,Row.compareRows);
		
		format=new BlockFormat(mostCharFont());
		updateFormat();
	}
	
	public void build(int x, int y) {
		rows=new ArrayList<Row>();
		
		if ( page.pageBitmap.points[x][y].ch != null )
			expand(page.pageBitmap.points[x][y].ch.row);
	}
	
	public void updateFormat() {
		format.update(this);
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
		
		page.blocks.remove(block);
		
		Collections.sort(rows,Row.compareRows);
		
		boolean merged=false;
		for(int i1=0; i1<rows.size(); i1++) {
			Row row1=rows.get(i1);
			for(int i2=0; i2<rows.size(); i2++) {
				Row row2=rows.get(i2);
				if(row1!=row2 && 
					(row1.isVIntersected(row2) && row1.isHIntersected(row2)) ) {
					row1.merge(row2);
					i2--;
					merged=true;
				}
			}
			if(merged)
				i1--;
			merged=false;
		}
	}
	
	ArrayList<Block> split(int rowNum) {
		ArrayList<Block> newBlocks=new ArrayList<Block>();
		
		ArrayList<Row> newRows1=new ArrayList<Row>();
		for(int i=0;i<rowNum;i++)
			newRows1.add(rows.get(i));
		Block block=new Block(page,column,newRows1);
		newBlocks.add(block);
		
		ArrayList<Row>newRows2=new ArrayList<Row>();
		for(int i=rowNum;i<rows.size();i++)
			newRows2.add(rows.get(i));
		block=new Block(page,column,newRows2);
		newBlocks.add(block);
		
		return newBlocks;
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
		if(rows.size() != block.rows.size())
			return false;
		
		Pattern pattern=Pattern.compile("\\s+");
		long i1=pattern.matcher(string()).results().count();
		long i2=pattern.matcher(block.string()).results().count();
		if(i1!=i2)
			return false;
		
		int allowedDisplace = (int) (Common._BlockDisplaceRatio * format.charfont.height);
		
		if( Math.abs(left - block.left) <= allowedDisplace &&
				Math.abs(upper - block.upper) <= allowedDisplace &&
				Math.abs(right - block.right) < allowedDisplace &&
				Math.abs(lower - block.lower) < allowedDisplace )
			return true;
		return false;
	}
	
	protected CharFont mostCharFont() {
		TreeMap<CharFont,Integer> charFonts=new TreeMap<>();
		
		for (Row row: rows) {
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
	
	protected int alignment() {
		int l,r;
		
		if(column!=null) {
			l=column.left;
			r=column.right;
		} else {
			l=page.left;
			r=page.right;
		}
		
		int leftIndent=left-l;
		int rightIndent=r-right;
		
		/*if(string().contains("INTRODUCTION"))
			System.out.println("");*/
		
		if(Math.abs(leftIndent-rightIndent) < 
				(int)(page.content.columnWidth*Common._CenterAlignAdjustment))
			if(leftIndent==0)
				return Common._CENTERALIGNED;
			else
				return Common._CENTERALIGNED_WI_INDENT;
		else if(leftIndent==0)
			return Common._LEFTALIGNED;
		else if(rightIndent==0)
			return Common._RIGHTALIGNED;
		else
			return Common._NOALIGNED;
	}
	
	public void setIgnored(String ignoredString) {
		type=Common._IgnoredBlockPrefix+ignoredString;
	}
	
	boolean isTextFullBlock() {
		return format.charfont.equals(page.content.textCharfont) && 
				isFull(page.content,column);
	}
	
	boolean isTextInfinished() {
		if(! isTextFullBlock())
			return false;
			
		String lastStr=rows.get(rows.size()-1).string();
			
		return Common.infinishedTextBlock.matcher(lastStr.trim()).find();
	}
	
	boolean isAllScarce() {
		for(Row row:rows) {
			if(! Common.scarceRow.matcher(row.string()).find())
				return false;
		}
		return true;
	}

	boolean isNonTitle() {
		if(rows.size()<2)
			return false;
		
		Row row1=rows.get(0);
		for(int i=1; i<rows.size(); i++) {
			Row row2=rows.get(i);
			String s=row2.string();
			
			int p=s.indexOf(' ');
			
			if(p<0)
				continue;
			
			int l=row2.chars.get(p).right-row2.chars.get(0).left+1;
			int minRight=column.right - l;
			
			if(row1.right < minRight)
				return true;
			
			row1=row2;
		}
		return false;
	}
	
	boolean isNotTextBlock() {
		int width=right-left+1;
		
		if(column==null)
			return width<=page.content.maxContentTrivalBlockWidth;
		
		return width<=page.content.maxColumnTrivalBlockWidth;
	}
	
	public boolean priorTo(Block block) {
		if(page!=block.page)
			return page.id<block.page.id;
		
		return compareBlocks.compare(this,block)<0 ? true : false;
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
		else if (format.charfont.equals(content.textCharfont))
			fw.write(String.format("type: text"));
		else 
			fw.write(String.format("type: undefined"));
					
		fw.write(String.format("\ntypeindex=%d left=%d right=%d upper=%d lower=%d\n====>\n",
				content.charfontIndexes.get(format.charfont),left,right,upper,lower));
		fw.write(String.format("charfont height=%f, charfont bold=%d, alignment=%d\n\n",
				format.charfont.height, format.charfont.bold, format.alignment));
		
		int y=rows.get(0).lower;
		for(Row row:rows) {
			if (row.upper>y) {
				fw.write("\n");
				y=row.lower;
			} else {
				fw.write(" ");
			}
			row.print(fw);
		}
		
		fw.write("\n==============================\n\n");
	}
	
	String string() {
		String str="";
		
		int y=rows.get(0).lower;
		for(Row row:rows) {
			if (row.upper>y) {
				str+="\n";
				y=row.lower;
			}
			str+=row.string();
		}
		
		return str+"\n";
	}
	
	
	static class BlockFormat {
		final CharFont charfont;
		int indent;
		int alignment;
		int allUppercase;
		
		public BlockFormat(CharFont charfont, int indent, int alignment) {
			this.charfont=charfont;
			this.indent=indent;
			this.alignment=alignment;
			allUppercase=Common._UNKNOWNALLUPPERCASE;
		}
		
		public BlockFormat(Block block) {
			this(block.mostCharFont(), block.indent(), block.alignment());
		}
		
		public void update(Block block) {
			this.indent=block.indent();
			this.alignment=block.alignment();
			this.allUppercase = Common.lowercaseExisting.matcher(block.string()).find() ? -1 : 1;
		}
		
		public BlockFormat(CharFont charfont) {
			this.charfont=charfont;
			this.indent=Common._RIGHTALIGNED;
			this.alignment=Common._UNKNOWNINDENT;
		}
		
		boolean equals(BlockFormat blockformat) {
			if(alignment==Common._CENTERALIGNED || 
					blockformat.alignment==Common._CENTERALIGNED)
				return charfont.equals(blockformat.charfont) &&
						allUppercase==blockformat.allUppercase;
			else 
				return charfont.equals(blockformat.charfont) &&
						indent==blockformat.indent &&
						allUppercase==blockformat.allUppercase;
		}
		
		int compareFormat(BlockFormat blockformat) {
			int r=charfont.compareTo(blockformat.charfont);
			
			if(r!=0)
				return r;
			
			if(alignment==Common._CENTERALIGNED_WI_INDENT && 
					blockformat.alignment!=Common._CENTERALIGNED_WI_INDENT)
				return 1;
			else if((alignment!=Common._CENTERALIGNED_WI_INDENT && 
					blockformat.alignment==Common._CENTERALIGNED_WI_INDENT))
				return -1;
			
			if(alignment==Common._CENTERALIGNED && 
					blockformat.alignment!=Common._CENTERALIGNED)
				return 1;
			else if((alignment!=Common._CENTERALIGNED && 
					blockformat.alignment==Common._CENTERALIGNED))
				return -1;
			
			return allUppercase-blockformat.allUppercase;
		}
		
		public int hashCode() {
	        int hash=charfont.hashCode() + (indent<<12) + (alignment<<24) ;
	        
	        return hash;
		}
	}
	
	static class CompareBlocks implements Comparator<Block> {
		public int compare(Block b1, Block b2) {
			if(b1.isVIntersected(b2) && b1.isHIntersected(b2)) {
				System.out.println("CompareBlocks error");
			}
			
			if(b1.isVIntersected(b2))
				return Common.compareValue(b1.left,b2.left);
			else if(b1.isHIntersected(b2))
				return Common.compareValue(b1.upper,b2.upper);
			else return Common.compareValue(b1.upper,b2.upper);
		}
	}
}
