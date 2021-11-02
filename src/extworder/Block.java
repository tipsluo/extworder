package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import extworder.Common.AdditionalSubtitleFormatFilter;
import extworder.Common.BigBlockFilter;
import extworder.Common.SubtitleBlockFilter;
import extworder.Common.BodyBlockFilter;
import extworder.Common.RangeGroup.Range;
import extworder.Common.StatGroup;
import extworder.Page.Column;
import extworder.Row.CharFont;

public class Block extends Rectangle {
	BlockFormat format;
	ArrayList<Row> rows;
	Page page;
	Column column;
	String type="";
    
	final static CompareBlocks compareBlocks=new CompareBlocks();
	final static CompareBlockLeftUppers compareBlockLeftUppers=new CompareBlockLeftUppers();
	final static BodyBlockFilter bodyBlockFilter=new BodyBlockFilter();
	final static BigBlockFilter bigBlockFilter=new BigBlockFilter();
	final static SubtitleBlockFilter subtitleBlockFilter=new SubtitleBlockFilter();
	static AdditionalSubtitleFormatFilter additionalSubtitleFormatFilter;
	
	public Block() {
		super();
	}
	
	public Block(Page page, Row row) {
		super();
		
		this.page=page;
		build(row);
		
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
	
	public void build(Row row) {
		rows=new ArrayList<Row>();
		expand(row);
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
		if(column!=null)
			column.blocks.remove(block);
		
		Collections.sort(rows,Row.compareRows);
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
	
	ArrayList<Block> split(ArrayList<Integer> rowIds) {
		ArrayList<Block> newBlocks=new ArrayList<Block>();
		
		rowIds.add(rows.size());
		
		int r=0;
		for(int i=0; i<rowIds.size(); i++) {
			ArrayList<Row> newRows=new ArrayList<Row>();
			
			for(;r<rowIds.get(i); r++)
				newRows.add(rows.get(r));
			
			Block block=new Block(page,column,newRows);
			newBlocks.add(block);
		}
		
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
	
	boolean isSimilar(Block block, boolean verifyVertical) {
		if(rows.size() != block.rows.size())
			return false;
		
		String s1=string();
		String s2=string();
		
		if(s1==s2)
			return true;
		
		if(! format.equals(block.format))
			return false;
		
		Pattern pattern=Pattern.compile("\\s+");
		long i1=pattern.matcher(s1).results().count();
		long i2=pattern.matcher(s2).results().count();
		if(i1!=i2)
			return false;
		
		int allowedDisplace = (int) (Common._BlockDisplaceRatio * format.charfont.height);
		
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
	
	int alignment() {
		if(column!=null)
			return super.alignment(column,page.content.centralAlignmentAdjustment);
		else
			return super.alignment(page,page.content.centralAlignmentAdjustment);
	}
	
	public void setIgnored(String ignoredString) {
		type=Common._IgnoredBlockPrefix+ignoredString;
	}
	
	boolean separateUpperLeftBigChar() {
		Row row=rows.get(0);
		
		if(row.chars.size()<=2 || 
				row.chars.get(0).width * Common._MaxUpperLeftWidthRatio > width )
			return false;
		
		ArrayList<Row> newRows=new ArrayList<Row>();
		
		Char ul=row.chars.get(0);
		
		ArrayList<Char> rights=ul.getRightConnected(this.page,row.wordReach,0);
		
		if(rights.size()<2)
			return false;
		
		Range r=new Range(9999,-9999);
		for(Char ch: rights) {
			r.add(ch.left);
		}
		if(r.length()>Common._CharLeftAdjustment)
			return false;
		
		row.clearCharRows();
		
		int index=rows.indexOf(row);
		
		ul.row=row; //set row temporarily so that it will not be expanded.

		Row highestRight=null;
		int i=0;
		for(;i<rights.size();i++) {
			Char ch=rights.get(i);
			
			if(ch.row!=null) {
				rights.remove(i);
				i--;
				continue;
			}
			
			Row row1=new Row(this.page,this,ch);
			
			if(row1.chars.size()==0) continue;
			
			row1.left=ul.left;
			row1.width=row1.right-row1.left;
			
			newRows.add(row1);
			if(highestRight==null || row1.upper<highestRight.upper)
				highestRight=row1;
		}
		
		if(newRows.size()<2)
			return false;
		
		ul.row=highestRight;
		highestRight.chars.add(ul);
		
		rows.remove(row);
		rows.addAll(index,newRows);
		page.rows.remove(row);
		page.rows.addAll(newRows);
		
		Collections.sort(rows,Row.compareRows);
		
		for(Char ch:rights) {
			ch.row.render();
		}
		
		return true;
	}
	
	protected ArrayList<Block> separateFarRows() {
		if(rows.size()<4)
			return null;

		int[] gaps=new int[rows.size()];
		StatGroup<Integer> gapCounts=new StatGroup<Integer>();
		
		Row row1=rows.get(0);
		for(int i=1;i<rows.size();i++) {
			Row row2=rows.get(i);
			
			gaps[i]=row2.upper-row1.lower;
			
			row1=row2;
		}
		
		for(int i=1;i<rows.size();i++) {
			gapCounts.add(gaps[i]);
		}
		
		int maxGap=Math.round(Common._MaxIntraBlockRowGapRatio * gapCounts.maxByValue());
		
		for(int i=1;i<rows.size();i++) {
			if(gaps[i]>maxGap) {
				return split(i);
			}
		}
		return null;
	}
	
	ArrayList<Block> separateRowsByCharfont() {
		ArrayList<Integer> splits=new ArrayList<Integer>();
		
		if(rows.size()<2)
			return null;
		
		Row row1;
		Row row2=rows.get(0);
		
		for(int i=1;i<rows.size();i++) {
			row1=row2;
			row2=rows.get(i);
			
			if(row1.charfont.allEquals(row2.charfont)) {
			//if(row1.charfont.equals(row2.charfont)) {
				continue;
			}
			
			Char row1last=row1.chars.get(row1.chars.size()-1);
			Char row2first=row2.chars.get(0);
			
			if(row1last.fontname==row2first.fontname && 
					row1last.height==row2first.height &&
					row1.rightAligned(this) &&
					row2.leftAligned(this))
				continue;
			
			splits.add(i);
		}
		
		if(splits.size()==0)
			return null;
			
		return split(splits);
	}
	
	boolean isBodyCharfontFullBlock() {
		return format.equals(page.content.bodyBlockformat) && 
				isFull(page.content,column);
	}
	
	boolean isBodyBlock() {
		return format.equals(page.content.bodyBlockformat) && 
				likeBodyBlock1()>=Common._ParaSentDefaultUno;
	}
	
	boolean isBodyInfinished() {
		if(! isBodyCharfontFullBlock())
			return false;
			
		String lastStr=rows.get(rows.size()-1).string();
			
		return Common.infinishedBodyBlock.matcher(lastStr.trim()).find();
	}
	
	boolean isAllScarce() {
		for(Row row:rows) {
			if(! row.scarceInBlock())
				return false;
		}
		return true;
	}

	int likeTitleBlock( ) {
		if(string().trim().isEmpty())
			return -1;
		
		Row row=rows.get(0);
		boolean raRow=row.rightAligned(this);
		boolean laRow=row.leftAligned(this);
		boolean caRow=row.centralAligned(this);
		boolean fullRow=row.isFull(page.content,column);
		int lendiff=Math.round(Common._SameBlockRowWidthDiff*row.width);
		
		for(int i=1; i<rows.size(); i++) {
			Row row1=rows.get(i);
			
			boolean raRow1=row1.rightAligned(this);
			boolean laRow1=row1.leftAligned(this);
			boolean caRow1=row1.centralAligned(this);
			boolean fullRow1=row1.isFull(page.content,column);
			int lendiff1=Math.round(Common._SameBlockRowWidthDiff*row1.width);
			
			if(! fullRow || ! fullRow1) {
				if( (fullRow && !laRow1 && !raRow1 && !caRow1) ||
						(fullRow1 && !laRow && !raRow && !caRow))
					return -1;
			}
			
			if(caRow != caRow1 && laRow!=laRow1 && raRow!=raRow1)
				return -1;
			
			if(laRow && row1.width-row.width > lendiff)
				return -1;
				
			row=row1;
			raRow=raRow1;
			laRow=laRow1;
			caRow=caRow1;
			fullRow=fullRow1;
			lendiff=lendiff1;
		}
		
		return 0;
	}
	
	int isParagraphBlock(CharFont charfont) {
//if(string().contains("amine to") )
//				System.out.println("");
		if(Common.leadingCapitalCount(string()) > Common._MaxLeadingCapitalRatio)
			return Common._TooManyLeadingCapital;
		
		int paraSentUnoNoTerm=0;
		
		for(Row row:rows) {
			if(charfont!=null && ! row.charfont.allEquals(charfont))
				continue;
			
			int f=row.firstParagraphLine();
			boolean p=row.isParaphaphLine();
			int l=row.lastParagraphLine();
			
			if(f>=Common._ParaSentDefaultTrue || p || l>=Common._ParaSentDefaultTrue) {
				paraSentUnoNoTerm=0;
				continue;	
			}
			
			if(f<=Common._ParaSentDefaultFalse && !p && l<=Common._ParaSentDefaultFalse)
				return Common._ParaSentDefaultFalse;
			
			if(l==Common._ParaSentUnoNoTerm) {
				paraSentUnoNoTerm++;
				if(paraSentUnoNoTerm>1)
					return Common._ParaSentDefaultFalse;
			} else
				paraSentUnoNoTerm=0;
		}
		
		if(paraSentUnoNoTerm>0)
			return Common._ParaSentUnoNoTerm;
		
		return Common._ParaSentDefaultTrue;
	}
	
	int likeBodyBlock1() {
		Rectangle rect = column==null ? page : column;

		if(type==Common._PageFooterBlock || type==Common._PageHeaderBlock)
			return Common._ParaSentDefaultFalse;

		if(page.content.bodyBlockformat!=null &&
				! format.equals(page.content.bodyBlockformat))
			return Common._ParaSentDefaultFalse;
		
		if(rows.size()==1) {
			Row row=rows.get(0);
			
			// row need to be the lastlineinblock
			if(row.lastParagraphLine()<=Common._ParaSentDefaultFalse)
				return Common._ParaSentDefaultFalse;
			
			//for ",   (2)"
			if(row.firstParagraphLineInRect(rect)<=Common._ParaSentDefaultFalse)
				return Common._ParaSentDefaultFalse;
		}
		
		int ret;
		if(page.content.bodyBlockformat!=null)
			ret=isParagraphBlock(page.content.bodyBlockformat.charfont);
		else
			ret=isParagraphBlock(null);
		
		return ret;
	}
	
	int likeBodyBlock2() {
		if(page.content.bodyBlockformat!=null &&
				! format.equals(page.content.bodyBlockformat))
			return Common._ParaSentDefaultFalse;
		
		float noAlignedColumn=0;
		float noAlignedBlock=0;
	
		for(Row row:rows) {
			if(! row.leftAligned(column) && ! row.rightAligned(column))
				noAlignedColumn++;
			if(! row.leftAligned(this) || ! row.rightAligned(this))
				noAlignedBlock++;
		}
		
		if(noAlignedColumn/rows.size() > Common._MaxMissingAlignedInColumnRation)
			return Common._BodyNoAligned;
		if(noAlignedBlock/rows.size() > Common._MaxNoAlignedInBlockRation)
			return Common._BodyNoAligned;

		return Common._BodyAlignedColumn;
	}
	
	boolean likeBodyBlock3() {
		int maxGap=Math.round(Common._MaxInterBodyBlockGapRatio * format.charfont.height);

		Block virtualBlock=new Block();
				
		virtualBlock.updateRectangle(this);
		
		ArrayList<Block> ubs=traceAllAbove(page.blocks,maxGap);
		for(Block ub:ubs) {
			if(ub.type==Common._PageHeaderBlock || ub.type==Common._PageFooterBlock ||
					ub.column!=column)
				break;
			virtualBlock.updateRectangle(ub);
		}
		
		ArrayList<Block> lbs=traceAllBelow(page.blocks,maxGap);
		for(Block lb:lbs) {
			if(lb.type==Common._PageHeaderBlock || lb.type==Common._PageFooterBlock ||
					lb.column!=column)
				break;
			virtualBlock.updateRectangle(lb);
		}
		
		if( Math.abs(column.width-virtualBlock.width) > column.width * Common._ColumnWidthAdjustment)
			return false;
		
		int c=page.columns.indexOf(column);
		
		if( c == page.columns.size()-1)
			return true;
		
		if(page.columns.get(c+1).blocks.size()==0)
			return true;
		
		lbs=getAllBelow(column.blocks);
		if(lbs.size()>0)
			return true;

		
		if(virtualBlock.lower < column.lower - maxGap)
			return false;
		
		return true;
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
		else if (format.equals(content.bodyBlockformat))
			fw.write(String.format("type: "));
		else 
			fw.write(String.format("type: undefined"));
		
		int columnLeft;
		if(column==null)
			columnLeft=-1;
		else
			columnLeft=column.left;
					
		fw.write(String.format("\ntypeindex=%d left=%d right=%d upper=%d lower=%d\n====>\n",
				content.blockformatIndexes.get(format),left,right,upper,lower));
		fw.write(String.format("charfont height=%f, charfont bold=%d, alignment=%d, allupper=%d, column left=%d, likeBody=%d\n\n",
				format.charfont.height, format.charfont.bold, format.alignment,format.allUppercase, columnLeft, likeBodyBlock1()));
		
		fw.write(string());
		
		fw.write("\n==============================\n\n");
	}
	
	public String string() {
		String str="";
		
		int y=rows.get(0).lower;
		for(Row row:rows) {
		//	if(row.scarce())
		//		continue;
			if (row.upper>=y) {
				str+="\n";
				y=row.lower;
			}
			
			str+=row.string();
		}
		
		return str+"\n";
	}
	
	
	static class BlockFormat implements Comparable<BlockFormat> {
		final CharFont charfont;
		int alignment;
		int allUppercase;
		
		public BlockFormat(CharFont charfont, int indent, int alignment) {
			this.charfont=charfont;
			//this.indent=indent;
			this.alignment=alignment;
			allUppercase=Common._NOTALLUPPERCASE;
		}
		
		public BlockFormat(Block block) {
			this(block.mostCharFont(), block.indent(), block.alignment());
		}
		
		public void update(Block block) {
			this.alignment=block.alignment();
			
			String s=block.string();
			if(s.length()<Common._MinUppercaseBlockCount || Common.lowercaseExisting.matcher(s).find())
				this.allUppercase=-1;
			else {
				int c=0;
				Matcher m=Common.uppercase.matcher(s);
				while(m.find())
					c++;
				if(c < s.length() * Common._MinUppercaseBlockRatio)
					this.allUppercase=Common._NOTALLUPPERCASE;
				else
					this.allUppercase=Common._ALLUPPERCASE;
			}
		}
		
		public BlockFormat(CharFont charfont) {
			this.charfont=charfont;
			this.alignment=Common._UNKNOWNALIGNED;
			this.allUppercase=Common._NOTALLUPPERCASE;
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
			int hash=charfont.value() * 2 + allUppercase;
	        
	        return hash;
		}
	}
	
	static class CompareBlocks implements Comparator<Block> {
		public int compare(Block b1, Block b2) {
			if(b1.page!=b2.page)
				return b1.page.id-b2.page.id;
			
			int b1column= b1.column==null ? -1 : b1.column.left;
			int b2column= b2.column==null ? -1 : b2.column.left;
			
			return b1column!=b2column ? b1column-b2column :
						b1.upper!=b2.upper ? b1.upper-b2.upper :
							b1.left-b2.left;
		}
	}
	
	static class CompareBlockLeftUppers implements Comparator<Block> {
		public int compare(Block b1, Block b2) {
			if(b1.page!=b2.page)
				return b1.page.id-b2.page.id;
			
			int b1column= b1.column==null ? -1 : b1.column.left;
			int b2column= b2.column==null ? -1 : b2.column.left;
			
			return b1column!=b2column ? b1column-b2column :
						b1.left!=b2.left ? b1.left-b2.left :
							b1.upper-b2.upper;
		}
	}
}
