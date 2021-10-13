package extworder;

import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.TextPosition;
import extworder.Char.Point;
import extworder.Common.BlockFilter;
import extworder.Content.HStretch;
import extworder.Row.CharFont;

public class Page extends Rectangle{
	Content content;
	int id;
    ArrayList<Char> chars;
    ArrayList<Block> blocks;
    ArrayList<Row> rows;
    ArrayList<Column> columns;
    PageBitmap pageBitmap;
    int headerY,footerY;
    PageImg pageImg;
    ArrayList<ColoredBlock> coloredBlocks;
	private int xOffset;
	private int yOffset;
	
	public Page(Content content,int id) {
		this.content=content;
		this.id=id;
		chars=new ArrayList<Char>();
		blocks=new ArrayList<Block>();
		columns=new ArrayList<Column>();
		rows=new ArrayList<Row>();
	}
	
	public void writeString(TextPosition text) {
		//if rotated skip it
    	if(text.getX()!=text.getXDirAdj()) {
    		return;
    	}
    	
    	String str;
    	str=text.toString();
    	
    	Char ch=new Char(str, text.getXDirAdj(),text.getYDirAdj()-text.getHeight(),text.getWidthDirAdj(),text.getHeight(),
    	    		text.getFont().getName());
    	
    	//--- Don't remove
    	//The following code worked for imageblock
    	//Char ch=new Char(str, text.getX(),text.getY()-text.getHeight(),text.getWidth(),text.getHeight(),
    	//		text.getFont().getName());
    
    	//Char ch=new Char(str, text.getXDirAdj(),text.getYDirAdj()-text.getHeight(),text.getWidthDirAdj(),text.getHeight(),
    	//		text.getFont().getName());
		
    	chars.add(ch);	
    	updateRectangle(ch);
	}
	
	public void complete(PDPage pdPage, boolean ignoreColoredBlock) throws IOException {
		//getPDPageWH(pdPage);
		adjustCoordinates();
		
		pageBitmap=new PageBitmap(this);
		eliminateCharIntersections();

		getAllRows();
		getAllCharBlocks();
	
		//mergeBlocks();
		
		pageBitmap=null;
		
		if((! ignoreColoredBlock) && pageImg!=null)
			markColoredCharBlocks();
	}
	
	private void getPDPageWH(PDPage pdPage) {
		PDRectangle pdRectangle=pdPage.getCropBox();
		width=Math.round(pdRectangle.getWidth());
		height=Math.round(pdRectangle.getHeight());
	}
	
	private void adjustCoordinates() {
		xOffset=left-1;
		yOffset=upper-1;
		
		for(Char ch:chars) {
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
				
				Char ch=pageBitmap.points[x][y].ch;
				if(ch==null)
					continue;
				
				ArrayList<Char> rights=ch.getRightConnected(this,1,0);
				if(rights.size()==0)
					continue;
				
				for(Char ch1:rights) {
					if(ch1.left<=ch.right) {
						for(int i=ch1.left; i<=ch.right; i++) {
							for(int j=ch1.upper; j<=ch1.lower; j++)
								pageBitmap.points[i][j]=new Point(i,j,ch1);
						}
						
						for(int i=ch1.left; i<=ch.right; i++) {
							for(int j=ch.upper;j<=ch.lower;j++)
								if(pageBitmap.points[i][j]!=null &&
										pageBitmap.points[i][j].ch==ch)
									pageBitmap.points[i][j]=null;
						}
						ch.right=ch1.left-1;
					}
				}
				
				ArrayList<Char> lowers=ch.getLowerConnected(this,1);
				if(lowers.size()==0)
					continue;
				
				for(Char ch1:lowers) {
					if(ch1.upper<=ch.lower) {
						for(int i=ch1.upper; i<=ch.lower; i++) {
							for(int j=ch1.left; j<=ch1.right; j++)
								pageBitmap.points[j][i]=new Point(j,i,ch1);
						}
						
						for(int i=ch1.upper; i<=ch.lower; i++) {
							for(int j=ch.left;j<=ch.right;j++)
								if(pageBitmap.points[j][i]!=null &&
										pageBitmap.points[j][i].ch==ch)
									pageBitmap.points[j][i]=null;
						}
						ch.lower=ch1.upper-1;
					}
				}
			}
	}

	void markHeaderFooter() {
		headerY=upper;
		footerY=lower;
		
		for(Block block:blocks) {
			if(block.type==Common._PageHeaderBlock)
				if(headerY<block.lower)
					headerY=block.lower;
			
			if(block.type==Common._PageFooterBlock)
				if(footerY>block.upper)
					footerY=block.upper;
		}
		
		for(Block block:blocks)
			if(block.lower<=headerY)
				block.type=Common._PageHeaderBlock;
			else if(block.upper>=footerY)
				block.type=Common._PageFooterBlock;

	}
	
	void updateBlockFormats() {
		for(Block block:blocks) {
			block.format.update(block);
		}
	}
	
	private void clearCharRows() {
		for(Row row:rows) {
			for(int x=row.left; x<=row.right; x++) {
				for(int y=row.upper; y<=row.lower; y++) {
					pageImg.setPageRGB(x,y,content.bgRGB);
				}
			}
		}
	}
	
	private void getAllColoredBlocks() {
		coloredBlocks=new ArrayList<ColoredBlock>();
		
		/*if(right>pageImg.img.getWidth()+1 ||
			lower>pageImg.img.getHeight()+1) {
			System.out.printf("Error getAllColoredBlocks. Page ID %d\n",id);
			return;
		}*/
		
		for(int x=left; x<=right; x++)
			for(int y=upper; y<=lower; y++) {
				if(! pageImg.in(x,y)) {
					System.out.printf("Error getAllColoredBlocks. x=%d  y=%d\n",x,y);
					continue;
				}
					
				if(pageImg.getPageRGB(x,y)!=content.bgRGB) 	{
					ColoredBlock coloredBlock=new ColoredBlock(x,y);
					
					if(! coloredBlock.isTrivial())
						coloredBlocks.add(coloredBlock);
				}
		}
	}
	
	private void markColoredCharBlocks() {
		clearCharRows();
		
		getAllColoredBlocks();
		Collections.sort(coloredBlocks,new ComparePerimeter<ColoredBlock>());
		
		for(int i1=0; i1<coloredBlocks.size();i1++) {
			ColoredBlock coloredBlock1=coloredBlocks.get(i1);
			
			for(int i2=i1+1; i2<coloredBlocks.size();) {
				ColoredBlock coloredBlock2=coloredBlocks.get(i2);
				
				if(coloredBlock1==coloredBlock2) {
					i2++;
					continue;
				}
				
				if (coloredBlock1.contains(coloredBlock2))
					coloredBlocks.remove(coloredBlock2);
				else
					i2++;
			}
		}
		
		for(ColoredBlock coloredBlock: coloredBlocks)
			for(Block charBlock:blocks)
				if(coloredBlock.contains(charBlock) &&
					((charBlock.right-charBlock.left)/(coloredBlock.right-coloredBlock.left)) < Common._IgnoredColoredBlockRatio &&
						((charBlock.lower-charBlock.upper)/(coloredBlock.lower-coloredBlock.upper)) < Common._IgnoredColoredBlockRatio)
					charBlock.type=Common._IgnoredBlockColored;
	}
	
	protected void makeColumns() {
		TreeMap<HStretch,Integer> hStretches=new TreeMap<>();
		
		for(Row row:rows) {
			if(row.width < content.lowColumnWidth || row.width > content.highColumnWidth)
				continue;
			
			HStretch hStretch=new HStretch(row.left,row.right);
			
			int n=hStretches.compute(hStretch, (k,v) -> (v == null ? 0 : v) + 1);
			hStretches.put(hStretch,n);
		}
		
		ArrayList<HStretch> columnStretches=new ArrayList<>();
		
		LinkedHashMap<HStretch, Integer> reverseSortedMap = new LinkedHashMap<>();
		hStretches.entrySet()
	    	.stream()
	    	.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) 
	    	.forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));
		int i=0;
		for (Map.Entry<HStretch,Integer> entry : reverseSortedMap.entrySet()) {
			if(i>=content.columnNumber) break;
			columnStretches.add(entry.getKey());
			i++;
		}
		
		Collections.sort(columnStretches);
		
		for(HStretch columnStretch:columnStretches) {
			int a=(int) (content.columnWidth * Common._ColumnWidthAdjustment / 2);
			int l=columnStretch.left - a;
			int r=columnStretch.right + a;
			
			columns.add(new Column(l,headerY,
					r,footerY));
		}
	}
	
	void separateAllUppers(CharFont textCharfont) {
		Block block;
		for(int i=0; i<blocks.size(); i++) {
			block=blocks.get(i);
			
			if( block.format.charfont.compareTo(textCharfont) <= 0 || 
					block.rows.size()<2 )
				continue;
			
			Row row1=block.rows.get(0);
			if(Common.lowercaseExisting.matcher(row1.string()).find())
				continue;

			Row row2=block.rows.get(1);
			if(Common.leading2Uppercase.matcher(row2.string()).find())
				continue;
			
			// Split lines of all upper case
			ArrayList<Block> newBlocks=block.split(1);
			
			blocks.remove(block);
			blocks.addAll(newBlocks);
			
			Page.this.blocks.remove(block);
			Page.this.blocks.addAll(newBlocks);
			
			i--;
		}
		
		Collections.sort(blocks,Block.compareBlocks);
	}
	
	private void mergeBlocks() {
		boolean merged=false;
		for(int i=0;i<blocks.size();i++) {
			Block b1=blocks.get(i);
			for(int j=0;j<blocks.size();j++) {
				Block b2=blocks.get(j);
				if(b1!=b2 && 
					 b1.contains(b2) ) {
					 /*&&
					 ( ! b2.isNotTextBlock() || b2.format.indent==Common._LEFTALIGNED)) {*/
					b1.merge(b2);
					j--;
					merged=true;
				}
			}
			if(merged)
				i--;
			merged=false;
		}
		Collections.sort(blocks,Block.compareBlocks);
	}
	
	private void getAllCharBlocks() {
		for(Row row:rows) {
			if(row.block==null) {
				Block block=new Block(this,row);
				blocks.add(block);
			}
		}
		
		for(Block block:blocks) {
			block.separateUpperLeftBigChar();
		}
		
		Collections.sort(blocks,Block.compareBlocks);
	}
	
	private void getAllRows() {
		for(int x=left; x<=right;x++)
			for(int y=upper;y<=lower;y++) {
				Point p=pageBitmap.points[x][y];
				if ( p == null) continue;
				
				Char ch=p.ch;
				if(ch==null) continue;
				
				if(ch.row==null) {
					Row row=new Row(this,null,x,y);
						rows.add(row);
				}
			}
		
		Collections.sort(rows,Row.compareRows);
	}

	protected ArrayList<Block> upperBlocks() {
		ArrayList<Block> tbs=new ArrayList<Block>();
		
		for(Block block:blocks)
			tbs.add(block);
		
		for(Block block:blocks)
			for(int i=0; i<tbs.size(); i++) {
				Block tb=tbs.get(i);
				if( tb.hIntersected(block) && tb.upper>block.lower ) {
					tbs.remove(tb);
					i--;
				}
			}
		
		Collections.sort(tbs,Block.compareBlocks);
		
		return tbs;
	}
	
	protected ArrayList<Block> lowerBlocks() {
		ArrayList<Block> bbs=new ArrayList<Block>();
		
		for(Block block:blocks)
			bbs.add(block);
		
		for(Block block:blocks)
			for(int i=0; i<bbs.size(); i++) {
				Block bb=bbs.get(i);
				if( bb.hIntersected(block) && bb.lower<block.upper ) {
					bbs.remove(bb);
					i--;
				}
			}
		
		Collections.sort(bbs,Block.compareBlocks);
		
		return bbs;
	}
	
	public void print(FileWriter fw) throws IOException {
		fw.write("==============================\n");

		
		fw.write(String.format("Page %d\n Left %d Right %d Top %d Bottom %d\n",
				id,left,right,upper,lower));
		
		if(pageImg!=null)
			fw.write(String.format("BufferImage Width %d Height %d\n",
					pageImg.img.getWidth(),pageImg.img.getHeight()));
		
		fw.write("\n\nColumn meta:\n");
		for(Column column:columns) {
			column.printMeta(fw);
		}
		
		fw.write("\n\nBlocks:\n----------------------\n");
		for(Block block:blocks) {
				block.print(fw);
		}
	}
	
	String body() {
		String str="";
		
		for(Column column: columns) {
			//str+=column.string(Block.textBlockFilter,Block.bigBlockFilter);
			str+=column.string(Block.bodyBlockFilter);
		}
		
		return str;
	}
	
	String subtitles() {
		String str="";
		
		for(Column column: columns) {
			str+=column.subtitles();
		}
		
		return str;
	}
	
	public String string() {
		String str="";

		for(Block block:blocks) {
			str+=block.string()+"\n";
		}
		
		return str;
	}
	
	ArrayList<Block> getBigBlockList() {
		ArrayList<Block> blocklist=new ArrayList<Block>();
		
		for(Column column:columns) {
			blocklist.addAll(column.getBigBlockList());
		}
		
		return blocklist;
	}
	
	boolean ignored() {
		return content.ignorePage.isIgnored(this);
	}

	class PageBitmap {
		Point[][] points;
		
		public PageBitmap(Page page) {
			points=new Point[page.right+1][page.lower+1];
			
			for (int i=0; i<chars.size();i++) {
				Char ch=chars.get(i);
				
				for (int x=ch.left; x<=ch.right; x++)
					for (int y=ch.upper; y<=ch.lower; y++) {
						Point point=new Char.Point(x,y,ch);
						points[x][y]=point;
					}
			}
		}
	}
	
	class Column extends Rectangle {
		ArrayList<Block> blocks;
		
		public Column(int left,int upper,int right, int lower) {
			super(left,upper,right,lower);
			build();
		}
		
		private void build() {
			blocks=new ArrayList<Block>();
			
			for(Block block:Page.this.blocks) {
				if(contains(block)) {
					block.column=this;
					blocks.add(block);
				}
			}
			Collections.sort(blocks,Block.compareBlocks);
			
			resetRectangle();
			for(Block block:blocks)
				updateRectangle(block);
		}
		
		public void print(FileWriter fw) throws IOException  {
			fw.write(String.format("Column left:%d upper:%d right:%d lower %d\n",
									left,upper,right,lower));
			for(Block block:blocks)
				block.print(fw);
		}
		
		public void printMeta(FileWriter fw) throws IOException  {
			fw.write(String.format("Column left:%d upper:%d right:%d lower %d\n",
									left,upper,right,lower));
		}
		
		String string(BlockFilter ...blockFilters) {
			String str="";
			
			for(Block block:blocks) {
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
			
			/*for(Block block:blocks) {
				boolean unmatched=false;
				
				for(BlockFilter filter : blockFilters)
					if(! filter.filter(block)) {
						unmatched=true;
						break;
					}
				
					if(unmatched)
						continue;
				
					str+=block.string()+"\n";
			}*/
			
			return str;
		}
		
		String subtitles() {
			String str="";
			
			for(Block block:blocks) {
				if(! Block.subtitleBlockFilter.filter(block))
					continue;
				
				String spaces="";
				for(int i=0;i<Common.subtitleLevel(block.type);i++)
					spaces+=" ";
				str+=spaces+block.string()+"\n";
			}
			
			return str;
		}
		
		ArrayList<Block> getBigBlockList() {
			ArrayList<Block> blocklist=new ArrayList<Block>();
			
			for(Block block:blocks) {
				if(! Block.bigBlockFilter.filter(block))
					continue;
				blocklist.add(block);
			}
			
			return blocklist;
		}
		
		String string() {
			String str="";
			
			for(Block block:blocks) {
				str+=block.string()+"\n";
			}
			
			return str;
		}
	}

	class ColoredBlock extends Rectangle {
		public ColoredBlock(int x,int y) {
			build(x,y);
		}
		
		public ColoredBlock(int left,int upper,int right, int lower) {
			super(left,upper,right,lower);
		}
		
		private void build(int x,int y) {
			if(pageImg.getPageRGB(x,y)==content.bgRGB)
				return;
			
			pageImg.setPageRGB(x,y,content.bgRGB);
			
			updateRectangle(x,y);
			
			if(x>Page.this.left)
				build(x-1,y);
			if(x<Page.this.right)
				build(x+1,y);
			if(y>Page.this.upper)
				build(x,y-1);
			if(y<Page.this.lower)
				build(x,y+1);
		}
		
		boolean isTrivial() {
			return (right-left <= Common._MaxTrivialColoredLength) &&
					(lower-upper <= Common._MaxTrivialColoredLength);
		}
		
		void clear() {
			clear(left,upper);
		}
		
		private void clear(int x,int y) {
			if(pageImg.getPageRGB(x,y)==content.bgRGB)
				return;
			
			pageImg.setPageRGB(x,y,content.bgRGB);			
			
			if(x>left)
				clear(x-1,y);
			if(x<right)
				clear(x+1,y);
			if(y>upper)
				clear(x,y-1);
			if(y<lower)
				clear(x,y+1);
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
			return left<=Page.this.left-1 && right>=Page.this.right-1 &&
						upper<=Page.this.upper-1 && lower>=Page.this.lower-1;
		}
		
		void setPageRGB(int xPage, int yPage, int rgb) {
			img.setRGB(xPage-1,yPage-1,rgb);
		}
		
		int getPageRGB(int xPage, int yPage) {
			return img.getRGB(xPage-1,yPage-1);
		}
	}
}
