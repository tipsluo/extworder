package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import extworder.Block.BlockFormat;
import extworder.Common.RangeGroup.Range;
import extworder.Common.StatGroup;
import extworder.Page.Column;
import extworder.Row.CharFont;

public class Block extends Rectangle {
	public BlockFormat format;
	public ArrayList<Row> rows;
	public Page page;
	Column column;
	public String type="";
	private String str;
    
	final static CompareBlocks compareBlocks=new CompareBlocks();
	final static CompareBlockLeftUppers compareBlockLeftUppers=new CompareBlockLeftUppers();
	final static BodyBlockFilter bodyBlockFilter=new BodyBlockFilter();
	final static BigBlockFilter bigBlockFilter=new BigBlockFilter();
	public final static SubtitleBlockFilter subtitleBlockFilter=new SubtitleBlockFilter();
	//static AdditionalSubtitleFormatFilter additionalSubtitleFormatFilter;
	
	public Block() {
		super();
		
		rows=new ArrayList<Row>();
		format=new BlockFormat();
	}
	
	public Block(Page page) {
		super();
		
		rows=new ArrayList<Row>();
		format=new BlockFormat();
		
		this.page=page;
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
			//updateRectangle(row);
		}
		
		rows.addAll(block.rows);
		
		page.blocks.remove(block);
		if(block.column!=null)
			block.column.blocks.remove(block);
		
		//format.update(this);
		
		//Collections.sort(rows,Row.compareRows);
		render();
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
		int ali;
		Rectangle parent = column==null ? page:column;
		
		ali=super.alignment(parent,page.content.centralAlignmentAdjustment,page.content);
		
		if(ali==Common._LEFTALIGNED && (rows.size()>1 && allRowsLongEnough()))
			ali=Common._FULLALIGNED;
		
		/*if(ali==Common._FULLALIGNED) {
			int ali1=rows.get(0).alignment(this,page.content.centralAlignmentAdjustment,page.content);
			if(ali1==Common._INDENTLEFTALIGNED) {
				ali=Common._FIRSTROWINDENTFULLALIGNED;
			}
		}*/
		
		/*if(rows.size()==1 && ali==Common._INDENTLEFTALIGNED)
			ali=Common._FIRSTROWINDENTFULLALIGNED;*/
		
		return ali;
	}
	
	public void setIgnored(String ignoredString) {
		type=Common._IgnoredBlockPrefix+ignoredString;
	}
	
	void mergeVIntersectedRows( ) {
		for(int i=0;i<rows.size();i++) {
			Row row1=rows.get(i);
			for(int j=i+1;j<rows.size();j++) {
				Row row2=rows.get(j);
				if(row1.vIntersected(row2)) {
					int d=(int) row1.distance(row2);
					if(d>=row1.height || d>=row2.height) {
						row1.merge(row2);
						j--;
					}
				}
			}
		}
			
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
				continue;
			}
			
			Char row1last=row1.chars.get(row1.chars.size()-1);
			Char row2first=row2.chars.get(0);
			
			if(row1last.font.getName()==row2first.font.getName() && 
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
	
	boolean allRowsLongEnough() {
		if(rows.size()<2)
			return true;
		
		Row row0=rows.get(0);
		
		int n=rows.size()-1;
		for(int i=1;i<n;i++) {
			Row row=rows.get(i);
			if(!row0.isLongEnough(this,row))
				return false;
			row0=row;
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
		boolean longEnoughRow=true;
		
		int firstRowIndent;
		if(column==null)
			firstRowIndent=row.alignment(page,Common._AlignAdjustment,page.content);
		else
			firstRowIndent=row.alignment(column,Common._AlignAdjustment,page.content);
		if(firstRowIndent==Common._INDENTLEFTALIGNED)
			laRow=true;
		
		Rectangle rect;
		if(column==null)
			rect=page;
		else
			rect=column;
		
		if(rows.size()>1)
			longEnoughRow=row.isLongEnough(rect,rows.get(1));	
		
		int lendiff=Math.round(Common._SameBlockRowWidthDiff*row.width);
		
		for(int i=1; i<rows.size(); i++) {
			Row row1=rows.get(i);
			
			boolean raRow1=row1.rightAligned(this);
			boolean laRow1=row1.leftAligned(this);
			boolean caRow1=row1.centralAligned(this);
			boolean fullRow1=row1.isFull(page.content,column);
			boolean longEnoughRow1=true;
			if(i<rows.size()-1)
				longEnoughRow1=row1.isLongEnough(rect,rows.get(i+1));
			int lendiff1=Math.round(Common._SameBlockRowWidthDiff*row1.width);
			
			if(! (fullRow || longEnoughRow) || ! (fullRow1 || longEnoughRow1) ) {
				if( ((fullRow || longEnoughRow) && !laRow1 && !raRow1 && !caRow1) ||
						((fullRow1 || longEnoughRow1) && !laRow && !raRow && !caRow))
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
			longEnoughRow=longEnoughRow1;
			lendiff=lendiff1;
		}
		
		return 0;
	}
	
	int isParagraphBlock(CharFont charfont) {
		if(Common.leadingCapitalCount(string()) > Common._MaxLeadingCapitalRatio)
			return Common._TooManyLeadingCapital;
		
		int paraSentUnoNoTerm=0;
		int rightMargin=0;
		
		for(Row row:rows) {
			if(charfont!=null && ! row.charfont.allEquals(charfont))
				continue;
			
			int rightMargin1=right-row.right;
			
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
					//If two consecutive row with the same right margins, that could means there is picture which makes the row widths different
					if(rightMargin1!=rightMargin) {
						return Common._ParaSentDefaultFalse;
					} else
						paraSentUnoNoTerm=0;
			} else
				paraSentUnoNoTerm=0;
			
			rightMargin=rightMargin1;
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
	
	private boolean allRowsSeparate() {
		if(rows.size()<=1)
			return true;
		
		Row row0=rows.get(0);
		for(int i=1; i<rows.size(); i++) {
			Row row=rows.get(i);
			
			float rowOverlap=row.vOverlap(row0);
			
			if(rowOverlap>0 && rowOverlap<Common._MinSameRowOverlap) {
				return false;
			}
			row0=row;
		}
		
		return true;
	}
	
	private boolean consecutive() {
		for(Row row:rows) {
			Char ch0=row.chars.get(0);
			int wordReachAdj=(int) (row.wordReach * Common._RowWordReachAdjRatio);
			for(Char ch:row.chars) {
				if(ch.left-ch0.right > wordReachAdj)
					return false;
				ch0=ch;
			}
		}
		
		return true;
	}
	
	private boolean rowsLeftAligned() {
		if(rows.size()<=1)
			return true;
		
		for(int i=1; i<rows.size(); i++) {
			Row row=rows.get(i);
			
			if(! row.leftAligned(this))
				return false;
		}
		
		return true;
	}
	
	List<Block> removeRightAlignedRow() {
		List<Block> newBlocks=new ArrayList<Block>();
		
		Block newBlock=new Block(page);
		
		int i=0;
		for(;i<rows.size();i++) {
			Row row=rows.get(i);
			
			int ali=row.alignment(this,Common._AlignAdjustment,page.content);
			if(ali==Common._RIGHTALIGNED) {
				newBlock.rows.add(row);
				rows.remove(row);
				i--;
			} else {
				if(newBlock.rows.size()>0) {
					newBlocks.add(newBlock);
					newBlock.render();
					newBlock=new Block(page);
				}
			}
		}
		
		if(newBlock.rows.size()>0) {
			newBlocks.add(newBlock);
			newBlock.render();
		}
		
		if(newBlocks.size()>0) {
			Column column=this.column;
			Page page=this.page;
			
			if(column!=null)
				column.blocks.addAll(newBlocks);
			page.blocks.addAll(newBlocks);
			
			if(rows.size()>0)
				render();
			else {
				if(column!=null)
					column.blocks.remove(this);
				page.blocks.remove(this);
			}
			
			page.sortBlocks();
		}
		
		return newBlocks;
	}
	
	/*boolean allRowsLongEnough() {
		if(rows.size()<=1)
			return true;
	
		Row row0=rows.get(0);
	
		for(int i=1; i<rows.size(); i++) {
			Row row=rows.get(i);
			if(! row0.isLongEnough(this,row0,row))
				return false;
			row0=row;
		}
		
		return true;
	}*/
	
	boolean isParagraphBlock2() {
		// 1. Don't check first-row-indent because some blocks such as the abrstract of 
		// "Spagna-1998-Dyslexia marker variables(AC2)" have different indents.
		// 2. Ideally, check first-uppercase
		
		return allRowsSeparate() && 
				consecutive() &&
				rowsLeftAligned();
	}
	
	public boolean priorTo(Block block) {
		if(page!=block.page)
			return page.id<block.page.id;
		
		return compareBlocks.compare(this,block)<0 ? true : false;
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
					
		fw.write(String.format("\ntypeindex=%d left=%d right=%d upper=%d lower=%d\n====>\n",
				page.content.blockformatIndexes.get(format),left,right,upper,lower));
		fw.write(String.format("charfont name=%s charfont height=%f, charfont bold=%d, alignment=%d, allupper=%d, column left=%d, likeBody=%d\n\n",
				format.charfont.name,format.charfont.height, format.charfont.bold, alignment(),format.allUppercase, columnLeft, likeBodyBlock1()));
		
		fw.write(string());
		
		fw.write("\n==============================\n\n");
	}
	
	public String string() {
		if(str!=null && str!="")
			return str;
		
		if(rows.size()==0)
			return "";
		
		int y=rows.get(0).lower;
		for(Row row:rows) {
			if (row.upper>=y) {
				str+="\n";
				y=row.lower;
			}
			
			str+=row.string();
		}
		
		str=str.trim();
		return str;
	}
	
	public void render() {
		for (Row row:rows) {
			updateRectangle(row);
		}
		format.update(this);
		
		Collections.sort(rows,Row.compareRows);
		
		renderString();
	}
	
	public String renderString() {
		str="";
		str=string();
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
		
		public BlockFormat(Block block) {
			this(block.mostCharFont(), block.indent(), block.alignment());
		}
		
		public BlockFormat(BlockFormat bf) {
			this.charfont=new CharFont(bf.charfont);
			this.alignment=bf.alignment;
			this.allUppercase=bf.allUppercase;
		}
		
		public void update(Block block) {
			this.alignment=block.alignment();
			
			String s=block.renderString();
			if(Common.lowercaseExisting.matcher(s).find())
				this.allUppercase=Common._NOTALLUPPERCASE;
			else if (Common.uppercase.matcher(s).find())
				this.allUppercase=Common._ALLUPPERCASE;
			
			this.charfont=block.getCharFont();
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
			if(alignment==Common._UNKNOWNALIGNED ||
					bf.alignment==Common._UNKNOWNALIGNED)
				return false;
			
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
	
	private CharFont getCharFont() {
		if(rows.size()==0)
			return null;
		
		StatGroup<CharFont> charfonts=new StatGroup<CharFont>();
		
		for (Row row: rows) {
			charfonts.add(row.charfont);
		}
			
		return charfonts.maxByValue();
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
	
	public interface BlockFilter {
		public boolean filter(Block block);
	}
	
	public static class BodyBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block block) {
			if(block.abbrOnly())
				return false;
			if(block.column==null)
				return false;
			return block.type==Common._Body;
		}
	}
	
	static class BigBlockFilter implements BlockFilter {
		final static Pattern pattern;
		
		static {
			pattern=Pattern.compile("^"+Common._IgnoredBlockPrefix);
		}
		
		@Override
		public boolean filter(Block block) {
			if(block.abbrOnly() || block.trivial() || block.alignment()==Common._UNKNOWNALIGNED)
				return false;
			
			int charfontDiff=block.format.compareTo(block.page.content.bodyBlockformat);
			
			if(block.likeBodyBlock1()>=Common._ParaSentDefaultUno)
				return true;
			
			if(charfontDiff>=0 && block.likeTitleBlock()>=0)
				return true;
			
			return false;
		}
	}
	
	public static class SubtitleBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block block) {
			if(block.abbrOnly() || block.trivial())
				return false;
			return block.format.compareTo(block.page.content.bodyBlockformat) >= 0 &&
					block.type.contains(Common._SubtitlePrefix);
		}
	}
	
	/*public static class SectionBlockFilter implements BlockFilter {
		@Override
		public boolean filter(Block block) {
			if(block.abbrOnly() || block.trivial())
				return false;
			return block.format.compareTo(block.page.content.bodyBlockformat) > 0 &&
					block.type.contains(Common._SectionPrefix);
		}
	}
	
	static class AdditionalSubtitleFormatFilter implements BlockFilter {
		private ArrayList<BlockFormat> blockformats;
		
		public AdditionalSubtitleFormatFilter(ArrayList<BlockFormat> blockformats) {
			this.blockformats=blockformats;
		}
		
		@Override
		public boolean filter(Block block) {
			if(block.abbrOnly() || block.trivial())
				return false;
			for(BlockFormat blockformat: blockformats)
				if(blockformat.equals(block.format))
					return true;
				
			return false;
		}
	}*/
}


