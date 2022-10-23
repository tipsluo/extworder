package extworder;

import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.TextPosition;

import extworder.Block2.BlockFilter;
import extworder.Common.SortedList;
import extworder.Common.Stretch;

public class Page2 extends Rectangle {
	public Content2 content;
	public int pid;
    public ArrayList<Char2> chars;
    public ArrayList<Block2> blocks;
    public ArrayList<Row2> rows;
    SortedList<Block2> blockCandidates;
    SortedList<Row2> rowCandidates;
    public ArrayList<Column2> columns;
    PageBitmap pageBitmap;
    public PageImg pageImg;
	private int xOffset;
	private int yOffset;
	private List<BlockSet> blocksetCandidates;
    int headerY,footerY;
	
	final static int _RowIntervalFactor1=3;
	final static int _RowIntervalFactor2=2;
	final static float _ColumnWidthAdjustment=0.05f;
	final static float _MaxBlockIntervalRatio=1.5f;
	final static float _CenterAlignAdjustment=0.05f;
	final static int _InBlockRowIntevalAdjustment=1;
	
	final static CompareColumns compareColumns=new CompareColumns();
    
	public Page2(Content2 content,int pid) {
		this.content=content;
		this.pid=pid;
		chars=new ArrayList<Char2>();
		blocks=new ArrayList<Block2>();
		columns=new ArrayList<Column2>();
		rows=new ArrayList<Row2>();
	}
	
	public void writeString(TextPosition text) {
		//if rotated skip it
    	if(text.getX()!=text.getXDirAdj()) {
    		return;
    	}
    	
    	String str;
    	str=text.toString();
    	
    	float w=text.getWidthDirAdj();
    	
    	// In 1-2colmn-Confidence_reports_in_decision, there is char which width is 0
    	if(w<=0)
    		return;
    	
    	Char2 ch=new Char2(str, text.getXDirAdj(), (text.getYDirAdj()-text.getHeight()), w, text.getHeight(),
    	    		text.getFont());
    	
       	chars.add(ch);
       	
    	updateRectangle(ch);
	}
	
	public void complete(PDPage pdPage, boolean ignoreColoredBlock) throws IOException {
		if(chars.size()==0)
			return;
		
		adjustCoordinates();
		
		pageBitmap=new PageBitmap(this);
		eliminateCharIntersections();
		
		Collections.sort(chars,Char2.compareChars);
	}
	
	public void analyze() {
//if(pid==4) 
	//		System.out.println();
//		
		
		System.out.printf("Analyzing page %d.\n",pid);
		System.out.println("Generating rows.");
		generateAllRowCandidates();
		System.out.println("Generating blocks."); 
		generateAllBlockCandidates();
		System.out.println("Generating blocksets.");
		generateBlocksetCandidates();
		
		long lowest=999999999;
		BlockSet blockset=null;
		for(BlockSet bs: blocksetCandidates) {
			//if(!bs.validate())
			//	continue;
			if(bs.getValue()<lowest) {
				lowest=bs.value;
				blockset=bs;
			}
		}
		if(blockset==null) {
			System.out.printf("ERROR: Page %d doesn't have qualified block set.\n", pid);
			return;
		}
			
		blocks=blockset.list;
		for(Block2 block:blocks) {
			for(Row2 row:block.rows) {
				for(Char2 ch:row.chars)
					ch.row=row;
				row.block=block;
			}
		}
		
		blocksetCandidates=null;
		blockCandidates=null;
		for(Row2 r: rowCandidates.list)
			r.blockCandidates=null;
		rowCandidates=null;
		for(Char2 c: chars)
			c.rowCandidates=null;
		
		separateAllUppers();
	}
	
	private void generateAllRowCandidates() {
		rowCandidates=new SortedList<Row2>(); 
		for(Char2 ch:chars) {
			for(int i=1; i<_RowIntervalFactor1 ;i++) {
				int interval=(int)(ch.height * _RowIntervalFactor2 * i );
				if(ch.rowCandidates.list.size()>0) {
					boolean done=false;
					for(Row2 row:ch.rowCandidates.list) 
						if(row.interval==interval) {
							done=true;
							break;
						}
					if(done)
						continue;
				}
				
 				new Row2(ch,this,interval);
			}
		}
		
		int i;
		long v=0;
		for(i=0; i<rowCandidates.list.size(); i++) {
			Row2 row=rowCandidates.list.get(i);
			
			long v1=row.hashValue();
			if(v==v1) {
				for(Char2 ch:row.chars)
					ch.rowCandidates.list.remove(row);
				rowCandidates.list.remove(i);
				i--;
				continue;
			}
				
			row.render();
			v=v1;
		}
		
		ArrayList<Char2> charChecks=new ArrayList<Char2>();	
		charChecks.addAll(chars);
		for(Row2 row: rowCandidates.list)
			for(Char2 ch:row.chars) {
				charChecks.remove(ch);
			}
		if(charChecks.size()!=0) {
			System.out.printf("ERROR: Page %d doesn't have all chars in the row candidates.\n", pid);
		}
	}
	
	private void generateAllBlockCandidates() {
		blockCandidates=new SortedList<Block2>();
		
		Collections.sort(rowCandidates.list,Row2.compareRows);
		for(int i=0; i<rowCandidates.list.size(); i++) {
			Row2 row=rowCandidates.list.get(i); 
			
			if(row.blockCandidates!=null && row.blockCandidates.list.size()!=1)
				continue;
			
			float itrv=checkRowsBelow(row,-1);

			if(itrv==-1) {
				//new Block2(row,0);
				continue;
			}
			
			Block2 block=new Block2(row,itrv);
			
			buildBlocks(row,block,itrv+0.01f);
		}
		
		rowCandidates.sort();
		
		updateConflictBlocks();
	}
	
	private void updateConflictBlocks() {
		for(Block2 b1:blockCandidates.list) {
			b1.conflicts=new SortedList<Block2>();
			for(Row2 row:b1.rows)
				for(Block2 b2:row.blockCandidates.list)
					if(b1!=b2)
						b1.conflicts.addSortUniq(b2);
		}
	}
	
	private float checkRowsBelow(Row2 row, float itrv) {
		float maxInterval=row.height * _MaxBlockIntervalRatio;
			
		SortedList<Row2> rs=row.getAllBelowCandidates(maxInterval);
		
		if(rs.list.size()==0)
			return itrv;
		
		float newItrv=999;
		ArrayList<Row2> rs1=new ArrayList<Row2>();
		for(Row2 row1: rs.list) {
			float itrv1=Row2.rowVDistance(row,row1);
			
			if(itrv1<newItrv) {
				newItrv=itrv1;
				rs1=new ArrayList<Row2>();
			}
			
			if(itrv1>newItrv)
				continue;
			
			rs1.add(row1);
		}
		
		if(itrv<0)
			itrv=newItrv;
		else {
			if(itrv>newItrv+_InBlockRowIntevalAdjustment) 
				return -1;
			
			if(Math.abs(itrv-newItrv)>_InBlockRowIntevalAdjustment)
				return itrv;
		}
		
		for(Row2 row1:rs1) {
			if(checkRowsBelow(row1,itrv)==-1)
				return -1;
		}
		
		return itrv;
	}
	
	private void buildBlocks(Row2 row, Block2 block, float maxInterval) {
		SortedList<Row2> rs=row.getAllBelowCandidates(maxInterval);
		
		boolean changed=false;
		for(Row2 r: rs.list) {
			if(r.charfont.height!=row.charfont.height)
				continue;
			
			float itrv1=Row2.rowVDistance(row,r);
			if(Math.abs(itrv1-block.interval)>_InBlockRowIntevalAdjustment)
				continue;

			Block2 b;
			
			if(rs.list.size()>1) {
				b=new Block2(block.interval,block);
				changed=true;
			} else
				b=block;
			
			b.addRow(r);
			 
			boolean intersected=true;
			for(Row2 row1:rs.list) {
				if(row!=row1 && !row.hIntersected(row1) && row.vIntersected(row1)) {
					intersected=false;
					break;
				}
			}
		
			if(!intersected) {
				return;
			}
			
			buildBlocks(r,b,maxInterval);
		}
		
		if(changed)
			Block2.removeBlock(block);;
	}
	
	/*private void (Block2 block, Row2 row) {
		SortedList<Row2> rs=row.getAllBelowCandidates(maxInterval);
		
		//boolean changed=false;
		for(Row2 r:rs.list) {
			if(block!=null) {
				if(Block2.overlap(block,r))
					continue;
			}

			float dis=Math.abs(row.medium-r.medium);
			
			if(block.rows.size()>=2 && Math.abs(dis-block.interval)>1)
				continue;
			
			boolean exist=false;
			for(Block2 b:r.blockCandidates.list) {
				if(b.interval==dis) {
					exist=true;
					break;
				}
			}
			if(exist)
				continue;
			
			Block2 b=new Block2(dis,block,r);
			if(b.purge) {
				continue;
			}
			
			//changed=true;
			
			buildBlockCandidatesBelow(b,r,maxInterval);
		}
		
		//return changed;
	}*/
	/*private void generateAllBlockCandidates() {
		blockCandidates=new SortedList<Block2>();
		
		for(int i=0; i<rowCandidates.list.size(); i++) {
			Row2 row=rowCandidates.list.get(i); 
			float maxInterval=row.height * _MaxBlockIntervalRatio;
				
			buildBlockCandidatesAbove(new Block2(row),row,maxInterval);
			buildBlockCandidatesBelow(new Block2(row),row,maxInterval); 
		}
		
		Collections.sort(blockCandidates.list, Block2.compareBlockRowNumber);
		Collections.reverse(blockCandidates.list); 
		for(int i=0; i<blockCandidates.list.size();i++) {
			Block2 b1=blockCandidates.list.get(i);
			
			if(b1.rows.size()==2 && b1.rows.get(0).charfont.height!=b1.rows.get(1).charfont.height) {
				Block2.removeBlock(this,i);
				i--;
				continue;
			}
			
			for(int j=i+1; j<blockCandidates.list.size();j++) {
				Block2 b2=blockCandidates.list.get(j);
				if(b1.containsAllRows(b2)) {
					Block2.removeBlock(this,j);
					j--;
				}
			}
		}
		Collections.sort(blockCandidates.list, Block2.compareBlockHashValue);

		ArrayList<Char2> charChecks=new ArrayList<Char2>();	
		charChecks.addAll(chars);
		for(Block2 block:blockCandidates.list)
			for(Row2 row: block.rows)
				for(Char2 ch:row.chars) {
					charChecks.remove(ch);
				}
		if(charChecks.size()!=0) {
			System.out.printf("ERROR: Page %d doesn't have all chars in the block candidates.\n", pid);
		}
	}
	
	private void buildBlockCandidatesBelow(Block2 block, Row2 row, float maxInterval) {
		SortedList<Row2> rs=row.getAllBelowCandidates(maxInterval);
		
		//boolean changed=false;
		for(Row2 r:rs.list) {
			if(block!=null) {
				if(Block2.overlap(block,r))
					continue;
			}

			float dis=Math.abs(row.medium-r.medium);
			
			if(block.rows.size()>=2 && Math.abs(dis-block.interval)>1)
				continue;
			
			boolean exist=false;
			for(Block2 b:r.blockCandidates.list) {
				if(b.interval==dis) {
					exist=true;
					break;
				}
			}
			if(exist)
				continue;
			
			Block2 b=new Block2(dis,block,r);
			if(b.purge) {
				continue;
			}
			
			//changed=true;
			
			buildBlockCandidatesBelow(b,r,maxInterval);
		}
		
		//return changed;
	}
	
	private void buildBlockCandidatesAbove(Block2 block, Row2 row, float maxInterval) {
		SortedList<Row2> rs=row.getAllAboveCandidates(maxInterval);
		
		//boolean changed=false;
		for(Row2 r:rs.list) {
			if(block!=null) {
				if(Block2.overlap(block,r))
					continue;
			}

			float dis=Math.abs(row.medium-r.medium);
			
			if(block.rows.size()>=2 && Math.abs(dis-block.interval)>1)
				continue;
			
			boolean exist=false;
			for(Block2 b:r.blockCandidates.list) {
				if(b.interval==dis) {
					exist=true;
					break;
				}
			}
			if(exist)
				continue;
			
			Block2 b=new Block2(dis,block,r);
			if(b.purge) {
				continue;
			}
			
			buildBlockCandidatesAbove(b,r,maxInterval);
		}
		
		//return changed;
	}*/
	
	private void generateBlocksetCandidates() {
		blocksetCandidates=new ArrayList<BlockSet>();

		BlockSet blockset=new BlockSet();
		buildBlockset(blockset);
	}
	
	private void buildBlockset(BlockSet blockset) {
		if(blockset.blocksAvailable.size()==0) {
		/*	List<Char2> missingChars=blockset.validate();
			
			if(missingChars.size()==0) {*/
				blocksetCandidates.add(blockset);
				return;
			/*}
			
			SortedList<Block2> missingBlocks=new SortedList<Block2>();
			
			for(Char2 ch:missingChars) {
				for(Row2 r: ch.rowCandidates.list) {
					Block2 b=new Block2(r);
					if(! b.purge)
						missingBlocks.addSortUniq(b);
				}
			}
			
			for(Block2 b:missingBlocks.list) {
				if(blockset.search(b)<0)
					blockset.blocksAvailable.add(b);
			}
			
			updateConflictBlocks();*/
		}
		
		SortedList<Block2> bs=new SortedList<Block2>();
		Block2 block=blockset.blocksAvailable.get(0);
		for(Row2 r:block.rows)
			bs.addSortUniq(r.blockCandidates);
		
		boolean changed=false;
		for(int i=0; i<bs.list.size(); i++) {
			Block2 b=bs.list.get(i);
			
			int n=blockset.search(b);
			if(n>=0)
				continue;
			if(blockset.overlap(b))
				continue;
			
			changed=true;
			BlockSet newBlockset=new BlockSet(blockset,b);
			buildBlockset(newBlockset);
		}
		
		if(!changed) {
			blockset.addSort(block);
			blockset.blocksAvailable.remove(block);
			
			//if(blockset.blocksAvailable.size()>0) {
				buildBlockset(blockset);
			//}
		}
	}
	
	/*private void generateBlocksetCandidates() {
		blocksetCandidates=new ArrayList<BlockSet>();

		BlockSet blockset=new BlockSet();
		buildBlockset(blockset);
	}
	
	private void buildBlockset(BlockSet blockset) {
		if(blockset.rowsAvailable.size()==0) {
			blocksetCandidates.add(blockset);
			return;
		}
		Row2 row=blockset.rowsAvailable.get(0);
		blockset.rowsAvailable.remove(0);
		
		if(row.blockCandidates.list.size()==0)
			System.out.printf("Row %s doesn't have any block candidate. (%d,%d)\n",row.string(),row.left,row.upper);
		
		boolean changed=false;
		for(int i=0; i<row.blockCandidates.list.size(); i++) {
			Block2 b=row.blockCandidates.list.get(i);
			
			if(blockset.overlap(b))
				continue;
			
			changed=true;
			
			BlockSet newBlockset=new BlockSet(blockset,b);
			
			newBlockset.addSort(b);
			
			//System.out.printf("level=%d, i=%d, block set available row count: %d\n",level, i,newBlockset.rowsAvailable.size());

			buildBlockset(newBlockset);
		}
		
		if(!changed) {
			if(blockset.rowsAvailable.size()>0) {
				buildBlockset(blockset);
				//} else
				//	blocksetCandidates.add(blockset);
			}
		}

	}*/
	
	private void adjustCoordinates() {
		xOffset=left-1;
		yOffset=upper-1;
		
		for(Char2 ch:chars) {
			ch.left=ch.left-xOffset;
			ch.upper=ch.upper-yOffset;
			ch.right=ch.right-xOffset;
			ch.lower=ch.lower-yOffset;
		}
		
		left=left-xOffset;
		right=right-xOffset;
		upper=upper-yOffset;
		lower=lower-yOffset;
		
		width=right-left;
		height=lower-upper;
	}
	
	private void eliminateCharIntersections() {
		for(int x=left; x<=right;x++)
			for(int y=upper;y<=lower;y++) {
				if(pageBitmap.points[x][y]==null)
					continue;
				
				Char2 ch=pageBitmap.points[x][y].ch;
				if(ch==null)
					continue;
				
				ArrayList<Char2> rights=ch.getRightConnected(this,1,0);
				if(rights.size()==0)
					continue;
				
				for(Char2 ch1:rights) {
					if(ch1!=null && ch1.left<=ch.right) {
						for(int i=ch1.left; i<=ch.right; i++) {
							for(int j=ch1.upper; j<=ch1.lower; j++)
								pageBitmap.points[i][j]=new Char2.Point(i,j,ch1);
						}
						
						for(int i=ch1.left; i<=ch.right; i++) {
							for(int j=ch1.upper;j<=ch1.lower;j++)
								if(pageBitmap.points[i][j]!=null &&
										pageBitmap.points[i][j].ch==ch)
									pageBitmap.points[i][j].ch=ch1;
						}
						
						for(int i=ch1.left; i<=ch.right; i++)
							for(int j=ch.upper; j<=ch.lower; j++)
								if(pageBitmap.points[i][j] != null && pageBitmap.points[i][j].ch!=null && pageBitmap.points[i][j].ch==ch)
									pageBitmap.points[i][j]=null;
							
						ch.right=ch1.left-1;
					}
				}
				
				ArrayList<Char2> lowers=ch.getLowerConnected(this,1);
				if(lowers.size()==0)
					continue;
				
				for(Char2 ch1:lowers) {
					if(ch1!=null && ch1.upper<=ch.lower) {
						for(int i=ch1.upper; i<=ch.lower; i++) {
							for(int j=ch1.left; j<=ch1.right; j++)
								pageBitmap.points[j][i]=new Char2.Point(j,i,ch1);
						}
						
						for(int i=ch1.upper; i<=ch.lower; i++) {
							for(int j=ch1.left;j<=ch1.right;j++)
								if(pageBitmap.points[j][i]!=null &&
										pageBitmap.points[j][i].ch==ch)
									pageBitmap.points[j][i].ch=ch1;
						}
						
						for(int i=ch1.upper; i<=ch.lower; i++)
							for(int j=ch.left; j<=ch.right; j++)
								if(pageBitmap.points[j][i]!=null && pageBitmap.points[j][i].ch!=null && pageBitmap.points[j][i].ch==ch)
									pageBitmap.points[j][i]=null;
						
						ch.lower=ch1.upper-1;
					}
				}
			}
	}
	
	protected ArrayList<Block2> upperBlocks() {
		ArrayList<Block2> tbs=new ArrayList<Block2>();
		
		for(Block2 block:blocks)
			tbs.add(block);
		
		for(Block2 block:blocks)
			for(int i=0; i<tbs.size(); i++) {
				Block2 tb=tbs.get(i);
				if( tb.hIntersected(block) && tb.upper>block.lower ) {
					tbs.remove(i);
					i--;
				}
			}
		
		Collections.sort(tbs,Block2.compareBlocks);
		
		return tbs;
	}
	
	protected ArrayList<Block2> lowerBlocks() {
		ArrayList<Block2> bbs=new ArrayList<Block2>();
		
		for(Block2 block:blocks)
			bbs.add(block);
		
		for(Block2 block:blocks)
			for(int i=0; i<bbs.size(); i++) {
				Block2 bb=bbs.get(i);
				if( bb.hIntersected(block) && bb.lower<block.upper ) {
					bbs.remove(i);
					i--;
				}
			}
		
		Collections.sort(bbs,Block2.compareBlocks);
		
		return bbs;
	}
	
	void markHeaderFooter() {
		headerY=upper;
		footerY=lower;
		
		for(Block2 block:blocks) {

			if(block.type==Block2._PageHeaderBlock)
				if(headerY<block.lower)
					headerY=block.lower;
			
			if(block.type==Block2._PageFooterBlock)
				if(footerY>block.upper)
					footerY=block.upper;
		}
		
		for(Block2 block:blocks)
			if(block.lower<=headerY)
				block.type=Block2._PageHeaderBlock;
			else if(block.upper>=footerY)
				block.type=Block2._PageFooterBlock;
	}
	
	int makeColumns() {
		int columnedBlockCount=0;
		TreeMap<Stretch,Integer> stretches=new TreeMap<>();
		
		for(Row2 row:rows) {
			if(row.width!=row.block.width)
				continue;
			
			if(row.width < content.lowColumnWidth || row.width > content.highColumnWidth)
				continue;
			
			Stretch stretch=new Stretch(row.left,row.right);
			
			int n=stretches.compute(stretch, (k,v) -> (v == null ? 0 : v) + 1);
			stretches.put(stretch,n);
		}
		
		ArrayList<Stretch> columnStretches=new ArrayList<>();
		
		LinkedHashMap<Stretch, Integer> reverseSortedMap = new LinkedHashMap<>();
		stretches.entrySet()
	    	.stream()
	    	.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) 
	    	.forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));
		int i=0;
		for (Map.Entry<Stretch,Integer> entry : reverseSortedMap.entrySet()) {
			if(i>=content.columnNumber) break;
			
			Stretch s=entry.getKey();
			columnStretches.add(s);
			i++;
		}
		
		Collections.sort(columnStretches);
		
		for(Stretch columnStretch:columnStretches) {
			int a=(int) (content.columnWidth * Page2._ColumnWidthAdjustment / 2);
			int l=columnStretch.start - a;
			int r=columnStretch.end + a;
			
			Column2 column=new Column2(l,headerY,r,footerY);
			columns.add(column);
			columnedBlockCount+=column.blocks.size();
		}
		
		sortBlocks();
		
		return columnedBlockCount;
	}
	
	ArrayList<Block2> getBlockList2() {
		ArrayList<Block2> bl=new ArrayList<Block2>();
		
		for(Block2 block:blocks)
			if(block.column==null) {
				bl.add(block);
			}
		
		if(columns.size()>0)
			for(Column2 column:columns)
				for(Block2 block:column.blocks) {
					bl.add(block);
				}
		
		return bl;
	}
	
	ArrayList<Block2> getBigBlockList() {
		ArrayList<Block2> blocklist=new ArrayList<Block2>();
		
		for(Block2 block:blocks) {
			if(! Block2.bigBlockFilter.filter(block))
				continue;
			blocklist.add(block);
		}
		
		return blocklist;
	}
	
	void sortBlocks() {
		if(columns.size()>0) {
			Collections.sort(columns,compareColumns);
			for(Column2 column:columns)
				Collections.sort(column.blocks,Block2.compareBlocks);
		}

		Collections.sort(blocks,Block2.compareBlocks);
	}
	
	boolean ignored() {
		return content.ignorePage.isIgnored(this);
	}
	
	void separateAllUppers() {
		Block2 block;
		for(int i=0; i<blocks.size(); i++) {
			block=blocks.get(i);
		
			if(block.rows.size()<2 )
				continue;
			
			Row2 row1=block.rows.get(0);
			if(Common.lowercaseExisting.matcher(row1.string()).find())
				continue;

			Row2 row2=block.rows.get(1);
			if(Common.leading2Uppercase.matcher(row2.string()).find())
				continue;
			
			ArrayList<Block2> newBlocks=block.split(1);
			
			blocks.remove(i);
			blocks.addAll(newBlocks);
			
			i--;
		}
		
		Collections.sort(blocks,Block2.compareBlocks);
	}
	
	void updateBlockFormats() {
		for(Block2 block:blocks) {
			block.format.update(block);
		}
	}
	
	public void renderStrings() {
		for(Block2 block:blocks)
			block.renderString();
	}
	
	
	class PageBitmap {
		Char2.Point[][] points;
		
		public PageBitmap(Page2 page) {
			points=new Char2.Point[page.right+1][page.lower+1];
			
			for (int i=0; i<chars.size();i++) {
				Char2 ch=chars.get(i);
				
				for (int x=ch.left; x<=ch.right; x++)
					for (int y=ch.upper; y<=ch.lower; y++) {
						Char2.Point point=new Char2.Point(x,y,ch);
						points[x][y]=point;
					}
			}
		}
	}
	
	public void print(FileWriter fw) throws IOException {
		fw.write("==============================\n");
		
		fw.write(String.format("Page %d\n Left %d Right %d Top %d Bottom %d\n",
				pid,left,right,upper,lower));
		
		if(pageImg!=null)
			fw.write(String.format("BufferImage Width %d Height %d\n",
					pageImg.img.getWidth(),pageImg.img.getHeight()));
		
		fw.write("\n\nColumn meta:\n");
		for(Column2 column:columns) {
			column.printMeta(fw);
		}
		
		fw.write("\n\nBlocks:\n----------------------\n");
		for(Block2 block:blocks) {
			block.print(fw);
		}
	}
	
	String string(BlockFilter ...blockFilters) {
		String str="";
		
		for(Block2 block:blocks) {
			boolean unmatched=false;
			
			for(BlockFilter filter : blockFilters)
				if(! filter.filter(block)) {
					unmatched=true;
					break;
				}
			
			if(unmatched)
				continue;
		
			str+=block.string()+"\n";
		}
		
		return str;
	}
	
	String subtitles() {
		String str="";
		
		for(Column2 column: columns) {
			str+=column.subtitles();
		}
		
		return str;
	}
	
	public ArrayList<Block2> filterBlocks(BlockFilter ...blockFilters) {
		ArrayList<Block2> bs=new ArrayList<Block2>();
		
		for(Column2 column: columns) {
			bs.addAll(column.filterBlocks(blockFilters));
		}
		
		return bs;
	}
	
	static class CompareColumns implements Comparator<Column2> {
		public int compare(Column2 c1,Column2 c2) {
			return c1.left-c2.left;
		}
	}
	
	class BlockSet extends SortedList<Block2> {
		long value;
		List<Block2> blocksAvailable;
		Rectangle area;
		
		BlockSet() {
			super();
			value=-1;
			blocksAvailable=new ArrayList<Block2>();
			blocksAvailable.addAll(Page2.this.blockCandidates.list);
			
			/*rowsAvailable=new ArrayList<Row2>();
			rowsAvailable.addAll(Page2.this.rowCandidates.list);*/
			
			area=new Rectangle();
			for(Block2 b:list)
				area.updateRectangle(b);
		}
		
		BlockSet(BlockSet bs, Block2 b) {
			super();
			value=-1;
			value=bs.value;
			//page=bs.page;
			list.addAll(bs.list);
			
			blocksAvailable=new ArrayList<Block2>();
			blocksAvailable.addAll(bs.blocksAvailable );
			area=new Rectangle(bs.area);
			area.updateRectangle(b);
			addSortUniq(b);
			blocksAvailable.remove(b);
			
			for(Block2 b1:b.conflicts.list)
				blocksAvailable.remove(b1);
		}
		
		long getValue() {
			if(value>=0) {
				return value;
			}
			value=0;
			for(Block2 block:list)
				value+=block.getValue();

			return value;
		}
		
		void register() {
			getValue();
			Page2.this.blocksetCandidates.add(this);
		}
		
		boolean overlap(Block2 block) {
			if(!area.vIntersected(block) && !area.hIntersected(block))
				return false;
			
			for(Block2 b:list)
				if(Block2.overlap(block, b))
					return true;
			
			return false;
		}
		
		ArrayList<Char2> validate() {
			ArrayList<Char2> chars=new ArrayList<Char2>();
			
			chars.addAll(Page2.this.chars);
			
			for(Block2 block:list)
				for(Row2 row: block.rows)
					for(Char2 ch:row.chars) {
						chars.remove(ch);
					}
			
			return chars;
		}
	}
	
	public class Column2 extends Rectangle {
		public ArrayList<Block2> blocks;
		
		public Column2(int left,int upper,int right, int lower) {
			super(left,upper,right,lower);
			build();
		}
		
		private void build() {
			blocks=new ArrayList<Block2>();
			
			for(Block2 block:Page2.this.blocks) {
				if(contains(block) && block.column==null) {
					block.column=this;
					blocks.add(block);
				}
			}
			
			render();
		}
		
		void render() {
			Collections.sort(blocks,Block2.compareBlocks);
			resetRectangle();
			for(Block2 block:blocks) {
				if(block.rows.size()==0)
					continue;
				updateRectangle(block);
				block.format.update(block);
				renderStrings();
			}
		}
		
		public void renderStrings() {
			for(Block2 block:blocks)
				block.renderString();
		}
		
		public void print(FileWriter fw) throws IOException  {
			fw.write(String.format("Column left:%d upper:%d right:%d lower %d\n",
									left,upper,right,lower));
			for(Block2 block:blocks)
				block.print(fw);
		}
		
		public void printMeta(FileWriter fw) throws IOException  {
			fw.write(String.format("Column left:%d upper:%d right:%d lower %d\n",
									left,upper,right,lower));
		}
		
		String string(BlockFilter ...blockFilters) {
			String str="";
			
			for(Block2 block:blocks) {
				boolean unmatched=false;
				
				for(BlockFilter filter : blockFilters)
					if(! filter.filter(block)) {
						unmatched=true;
						break;
					}
				
				if(unmatched)
					continue;
			
				str+=block.string()+"\n";
			}
			
			return str;
		}
		
		String subtitles() {
			String str="";
			
			for(Block2 block:blocks) {
				if(! Block2.subtitleBlockFilter.filter(block))
					continue;
				
				str+=block.string()+"\n";
			}
			
			return str;
		}
		
		String string() {
			String str="";
			
			for(Block2 block:blocks) {
				str+=block.string()+"\n";
			}
			
			return str;
		}
		
		public ArrayList<Block2> filterBlocks(BlockFilter ...blockFilters) {
			ArrayList<Block2> bs=new ArrayList<Block2>();
			
			for(Block2 block:blocks) {
				boolean matched=false;
				
				for(BlockFilter blockFilter: blockFilters)
					if(blockFilter.filter(block)) {
						matched=true;
					
						break;
					}
				
				if(matched)
					bs.add(block);
			}
			
			return bs;
		}
	}
	
	class PageImg {
		public BufferedImage img;
		
		private int left;
		private int right;
		private int upper;
		private int lower;

		public PageImg(BufferedImage img) {
			this.img=img;
			left=img.getMinX();
			upper=img.getMinY();
			right=img.getWidth()-1;
			lower=img.getHeight()-1;
		}
		
		boolean in(int xPage, int yPage) {
			return (xPage-1>=left || yPage-1>=upper || xPage-1<=right || yPage-1<=lower);
		}
		
		boolean normal() {
			return left<=Page2.this.left-1 && right>=Page2.this.right-1 &&
						upper<=Page2.this.upper-1 && lower>=Page2.this.lower-1;
		}
		
		void setPageRGB(int xPage, int yPage, int rgb) {
			img.setRGB(xPage-1,yPage-1,rgb);
		}
		
		int getPageRGB(int xPage, int yPage) {
			return img.getRGB(xPage-1,yPage-1);
		}
	}
}
