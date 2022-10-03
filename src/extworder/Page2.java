package extworder;

import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.TextPosition;

import extworder.Block2.BlockFilter;
import extworder.Common.SortedList;

public class Page2 extends Rectangle {
	public Content2 content;
	public int pid;
    public ArrayList<Char2> chars;
    public ArrayList<Block2> blocks;
    public ArrayList<Row2> rows;
    public SortedList<Block2> blockCandidates;
    public SortedList<Row2> rowCandidates;
    PageBitmap pageBitmap;
    public PageImg pageImg;
	private int xOffset;
	private int yOffset;
	private List<BlockSet> blocksetCandidates;
	
	//final static int _MaxCharInRowInterval=1;
	final static int _RowIntervalFactor1=3;
	final static int _RowIntervalFactor2=2;
	final static float _MaxBlockIntervalRatio=3.5f;
    
	public Page2(Content2 content,int pid) {
		this.content=content;
		this.pid=pid;
		chars=new ArrayList<Char2>();
		blocks=new ArrayList<Block2>();
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
    	
    	Char2 ch=new Char2(str, text.getXDirAdj()*3, (text.getYDirAdj()-text.getHeight())*3, w*3, text.getHeight()*3,
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
		System.out.printf("Analyzing page %d.\n",pid);
		System.out.println("Generating rows.");
		generateAllRowCandidates();
		System.out.println("Generating blocks.");
		generateAllBlockCandidates();
		System.out.println("\nGenerating blocksets.");
		generateBlocksetCandidates();
		
		long lowest=999999999;
		BlockSet blockset=null;
		for(BlockSet bs: blocksetCandidates) 
			if(bs.value<lowest) {
				lowest=bs.value;
				blockset=bs;
			}
		
		blocks=blockset.list;
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
				rowCandidates.list.remove(i);
				i--;
				continue;
			}
				
			row.render();
			v=v1;
		}
	}
	
	private void generateAllBlockCandidates() {
		blockCandidates=new SortedList<Block2>();
		
		for(int i=0; i<rowCandidates.list.size(); i++) {
			Row2 row=rowCandidates.list.get(i); 
			
			new Block2(row);
		}
		
		mateBlocks();
		
		Collections.sort(blockCandidates.list, Block2.compareBlockRowNumber);
		Collections.reverse(blockCandidates.list); 
		for(int i=0; i<blockCandidates.list.size();i++) {
			Block2 b1=blockCandidates.list.get(i);
			for(int j=i+1; j<blockCandidates.list.size();j++) {
				Block2 b2=blockCandidates.list.get(j);
				if(b1.containsAllRows(b2)) {
					for(Row2 row:b2.rows)
						row.blockCandidates.list.remove(b2);
					blockCandidates.list.remove(j);
					j--;
				}
			}
		}
		Collections.sort(blockCandidates.list, Block2.compareBlockHashValue);
	}

	private void mateBlocks() {
		SortedList<Block2> tempBlocks=new SortedList<Block2>();
		
		tempBlocks.list.addAll(blockCandidates.list);
		
		System.out.printf("\nMating blocks. Block candidates proecessed: %d. Row candidate count: %d.", blockCandidates.list.size(), rowCandidates.list.size());
		
		for(int i=0; i<tempBlocks.list.size(); i++) {
			Block2 block1=tempBlocks.list.get(i);
				
			for(int j=i+1; j<tempBlocks.list.size(); j++) {
				Block2 block2=tempBlocks.list.get(j);
				
				if(block1.format.charfont.height!=block2.format.charfont.height || !block1.hIntersected(block2))
					continue;
				
				float dis=Block2.blockVDistance(block1,block2);
				float maxInterval=Math.max(block1.format.charfont.height, block2.format.charfont.height) * _MaxBlockIntervalRatio;
				
				if(dis==-1 && Block2.overlap(block1,block2)) {
					continue;
				}
				
				if(block1.rows.size()>1 && block2.rows.size()>1 && Math.abs(block1.interval-block2.interval)>1)
					continue;
					
				if(dis>maxInterval || (block1.rows.size()>1 && Math.abs(dis-block1.interval)>1) || (block2.rows.size()>1 && Math.abs(dis-block2.interval)>1))
					continue;
				
				new Block2(dis,block1,block2);
				/*if(b.value==0) {
					block1.purge=block2.purge=true;
					System.out.printf("%s \n===================> \n%s\n++++++++++++++++++++++++\n%s\n----------------------\n", b.string(),block1.string(),block2.string());
				}*/
			}
		}
		
		/*int i;
		long v=0;
		for(i=0; i<blockCandidates.list.size(); i++) {
			Block2 block=blockCandidates.list.get(i);
			
			if(block.purge) {
				for(Row2 row:block.rows)
					row.blockCandidates.list.remove(block);
				
				blockCandidates.list.remove(i);
				i--;
				continue;
			}
			
			long v1=block.hashValue();
			if(v==v1) {
				blockCandidates.list.remove(i);
				i--;
				continue;
			}
				
			block.renderString();
			v=v1;
		}*/
		
		if(tempBlocks.list.size()==blockCandidates.list.size())
			return;
		
		mateBlocks();
	}
	
	private void generateBlocksetCandidates() {
		blocksetCandidates=new ArrayList<BlockSet>();
		
		System.out.println();
		
		//if(pid==2) 		
		//	System.out.println();

		
		BlockSet blockset=new BlockSet(this);
		buildBlockset(blockset,0);
	}
	
	private void buildBlockset(BlockSet blockset, int level) {
		if(blockset.rowsAvailable.size()==0) {
			blocksetCandidates.add(blockset);
			return;
		}
		Row2 row=blockset.rowsAvailable.get(0);
		
		if(row.blockCandidates.list.size()==0)
			System.out.printf("Row %s doesn't have any block candidate. (%d,%d)\n",row.string(),row.left,row.upper);
		
		boolean next=false;
		for(int i=0; i<row.blockCandidates.list.size(); i++) {
			Block2 b=row.blockCandidates.list.get(i);
			
			if(blockset.overlap(b))
				continue;
			
			next=true;
				
			BlockSet newBlockset=new BlockSet(blockset,b);
			
			newBlockset.addSort(b);
			
			//System.out.printf("level=%d, i=%d, block set available row count: %d\n",level, i,newBlockset.rowsAvailable.size());

			buildBlockset(newBlockset,level+1);
		}
		
		if(!next)
			blocksetCandidates.add(blockset);
	}
	
/*	private void generateBlocksetCandidates() {
		blocksetCandidates=new ArrayList<BlockSet>();
		
		System.out.println();
		
		BlockSet blockset=new BlockSet(this);
		buildBlockset(blockset);
	}
	
	private void buildBlockset(BlockSet blockset) {
		if(blockset.charsAvailable.size()==0) {
			blocksetCandidates.add(blockset);
			return;
		}

		Char2 ch=blockset.charsAvailable.get(0);
		
		if(ch.blockCandidates.list.size()==0)
			System.out.printf("Character %s doesn't have any block candidate. (%d,%d)\n",ch.str,ch.left,ch.upper);
		
		for(int i=0; i<ch.blockCandidates.list.size(); i++) {
			Block2 b=ch.blockCandidates.list.get(i);
			
			if(blockset.overlap(b))
				continue;
				
			BlockSet newBlockset=new BlockSet(blockset,b);
			
			newBlockset.addSort(b);
			
			System.out.printf("i=%d, block set available char count: %d\n",i,newBlockset.charsAvailable.size());

			buildBlockset(newBlockset);
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
	
	boolean ignored() {
		return content.ignorePage.isIgnored(this);
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
		/*for(Column column:columns) {
			column.printMeta(fw);
		}*/
		
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
	
	class BlockSet extends SortedList<Block2> {
		long value;
		Page2 page;
		List<Row2> rowsAvailable;
		Rectangle area;
		
		BlockSet(Page2 page) {
			super();
			value=0;
			this.page=page;
			rowsAvailable=new ArrayList<Row2>();
			rowsAvailable.addAll(page.rowCandidates.list);
			
			area=new Rectangle();
			for(Block2 b:list)
				area.updateRectangle(b);
		}
		
		BlockSet(BlockSet bs, Block2 b) {
			super();
			value=bs.value;
			page=bs.page;
			list.addAll(bs.list);
			rowsAvailable=new ArrayList<Row2>();
			rowsAvailable.addAll(bs.rowsAvailable );
			
			for(Row2 r:b.rows) {
				rowsAvailable.remove(r);
				
				for(int i=0; i<rowsAvailable.size(); i++) {
					Row2 r1=rowsAvailable.get(i);
					if(r.hIntersected(r1) && r.vIntersected(r1)) {
						rowsAvailable.remove(i);
						i--;
					}
				}
			}
			
			area=new Rectangle(bs.area);
			area.updateRectangle(b);
		}
		
		long getValue() {
			long v=0;
			for(Block2 block:list)
				v+=block.value;
			return v;
		}
		
		void register() {
			getValue();
			page.blocksetCandidates.add(this);
		}
		
		boolean overlap(Block2 block) {
			if(!area.vIntersected(block) && !area.hIntersected(block))
				return false;
			
			for(Block2 b:list)
				if(Block2.overlap(block, b))
					return true;
			
			return false;
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
