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
import org.apache.pdfbox.text.TextPosition;

import extworder.Char.CharFont;
import extworder.Char.Point;
import extworder.Common.BlockFilter;
import extworder.Content.HStretch;

public class Page extends Rectangle{
	Content content;
	int id;
    ArrayList<Char> chars;
    ArrayList<Block> blocks;
    ArrayList<Row> rows;
    ArrayList<Column> columns;
    public float width,height;
    PageBitmap pageBitmap;
    int headerY,footerY;
    BufferedImage pageImg;
    ArrayList<ColoredBlock> coloredBlocks;
	
	public Page(Content content,int id) {
		this.content=content;
		this.id=id;
		width=height=-1;
		chars=new ArrayList<Char>();
		blocks=new ArrayList<Block>();
		columns=new ArrayList<Column>();
		rows=new ArrayList<Row>();
	}
	
	public void writeString(TextPosition text) {
		CharFont charfont=new CharFont(text.getFont().getName(),text.getHeightDir());
    	
    	Integer n;
    	n=content.charfonts.compute(charfont, (k,v) -> (v == null ? 0 : v) + 1);
    	content.charfonts.put(charfont,n);
    	String str;
    	str=text.toString();
    	//Char ch=new Char(str, text.getXDirAdj(),text.getYDirAdj(),text.getWidthDirAdj(),text.getHeightDir(),
    	Char ch=new Char(str, text.getX(),text.getY()-text.getHeight(),text.getWidthDirAdj(),text.getHeight(),
    			text.getFont().getName());
    	chars.add(ch);
	}
	
	public void complete(PDPage pdPage) throws IOException {
		width=pdPage.getMediaBox().getWidth();
		height=pdPage.getMediaBox().getHeight();
		
		pageBitmap=new PageBitmap(this);
		
		getAllRows();
		getAllCharBlocks();
		
		//content.pdProcessor.processPage(pdPage);
		
		pageBitmap=null;
		
		for(Char ch:chars) {
			updateRectangle(ch);
		}
		
		markColoredCharBlocks();
	}

	void markHeaderFooter() {
		headerY=1;
		footerY=Math.round(height);
		
		for(Block block:blocks) {
			if(block.type==Common._PageHeaderBlock)
				if(headerY<block.lower)
					headerY=block.lower;
			
			if(block.type==Common._PageFooterBlock)
				if(footerY>block.upper)
					footerY=block.upper;
		}
	}
	
	private void clearHighlightedRows() {
		int w=pageImg.getWidth();
		int h=pageImg.getHeight();
		
		for(Row row:rows) {
			for(int x=row.left; x<=row.right; x++) {
				for(int y=row.upper; y<=row.lower; y++) {
					if(x<w && y<h)
						pageImg.setRGB(x,y,content.bgRGB);
				}
			}
		}
	}
	
	private void getAllColoredBlocks() {
		coloredBlocks=new ArrayList<ColoredBlock>();
		
		for(int x=left; x<=right; x++)
			for(int y=upper; y<=lower; y++)
				if(pageImg.getRGB(x,y)!=content.bgRGB) {
					ColoredBlock coloredBlock=new ColoredBlock(x,y);
					
					if(! coloredBlock.isTrivial())
						coloredBlocks.add(coloredBlock);
					
					coloredBlock.clear(coloredBlock.left,coloredBlock.upper);
				}
	}
	
	private void markColoredCharBlocks() {
		clearHighlightedRows();
		
		getAllColoredBlocks();
		
		for(ColoredBlock coloredBlock: coloredBlocks)
			for(Block charBlock:blocks)
				if(coloredBlock.contains(charBlock))
					charBlock.type=Common._IgnoredBlockHighlighted;
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
	
	private void getAllCharBlocks() {
		for(int x=0; x<=width;x++)
			for(int y=0;y<=height;y++) {
				Point p=pageBitmap.points[x][y];
				if ( p == null ) continue;
				
				Char ch=p.ch;
				if(ch==null) continue;
				
				if(ch.row.block==null) {
					blocks.add(new Block(this,x,y));
				}
			}
		
		for(int i=0;i<blocks.size();i++) {
			Block b1=blocks.get(i);
			for(int j=0;j<blocks.size();j++) {
				Block b2=blocks.get(j);
				if(b1!=b2 && b1.contains(b2)) {
					b1.merge(b2);
					j--;
				}
			}
		}
		
		Collections.sort(blocks,Block.compareBlocks);
	}
	
	private void getAllRows() {
		for(int x=0; x<=width;x++)
			for(int y=0;y<=height;y++) {
				Point p=pageBitmap.points[x][y];
				if ( p == null ) continue;
				
				Char ch=p.ch;
				if(ch==null) continue;
				
				if(ch.row==null) {
					rows.add(new Row(this,x,y));
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
				if( tb.isHIntersected(block) && tb.upper>block.lower ) {
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
				if( bb.isHIntersected(block) && bb.lower<block.upper ) {
					bbs.remove(bb);
					i--;
				}
			}
		
		Collections.sort(bbs,Block.compareBlocks);
		
		return bbs;
	}
	
	public void print(FileWriter fw) throws IOException {
		fw.write("==============================\n");
		fw.write(String.format("Page %d\nWidth %f Height %f Left %d Right %d Top %d Bottom %d\n",
				id,width,height,left,right,upper,lower));
		fw.write(String.format("BufferImage Width %d Height %d\n",
				pageImg.getWidth(),pageImg.getHeight()));
		
		fw.write("\n\nBlocks:\n----------------------\n");
		for(Block block:blocks)
			block.print(fw);
	}
	
	String text() {
		String str="";
		
		for(Column column: columns) {
			str+=column.string(Block.textBlockFilter,Block.bigBlockFilter);
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
	
	String string() {
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
		String str=string();
		if (str.contains(Common._LenderStr) || str.contains(Common._BorrowerStr))		
			return true;
		else
			return false;
	}

	class PageBitmap {
		Point[][] points;
		
		public PageBitmap(Page page) {
			points=new Point[Math.round(page.width)+1][Math.round(page.height)+1];
			
			for (Char ch: page.chars) {
				for (int x=Math.round(ch.x); x<=ch.right; x++)
					for (int y=Math.round(ch.y); y<=ch.lower; y++) {
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
		}
		
		public void print(FileWriter fw) throws IOException  {
			fw.write(String.format("Column left:%d upper:%d right:%d lower %d\n",
									left,upper,right,lower));
			for(Block block:blocks)
				block.print(fw);
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
		private int rgb;
		
		public ColoredBlock(int x,int y) {
			rgb=pageImg.getRGB(x,y);
			build(x,y);
		}
		
		public ColoredBlock(int left,int upper,int right, int lower) {
			super(left,upper,right,lower);
			rgb=pageImg.getRGB(left,upper);
		}
		
		private void build(int x,int y) {
			if(pageImg.getRGB(x,y)!=rgb)
				return;
			
			updateRectangle(x,y);
			
			if(x>left)
				build(x-1,y);
			if(x<right)
				build(x+1,y);
			if(y>upper)
				build(x,y-1);
			if(y<lower)
				build(x,y+1);
		}
		
		boolean isTrivial() {
			return (right-left <= Common._MaxTrivialLength) &&
					(lower-upper <= Common._MaxTrivialLength);
		}
		
		void clear(int x,int y) {
			if(pageImg.getRGB(x,y)==content.bgRGB)
				return;
			
			pageImg.setRGB(x,y,content.bgRGB);			
			
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
}
