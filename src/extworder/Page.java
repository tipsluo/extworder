package extworder;

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

public class Page {
	Content content;
	int id;
    ArrayList<Char> chars;
    ArrayList<Block> blocks;
    ArrayList<Row> rows;
    ArrayList<Column> columns;
    ArrayList<Block> images;
    public float width,height;
    Bitmap bitmap;
    int headerY,footerY;
	
	public Page(Content content,int id) {
		this.content=content;
		this.id=id;
		width=height=-1;
		chars=new ArrayList<Char>();
		blocks=new ArrayList<Block>();
		columns=new ArrayList<Column>();
		rows=new ArrayList<Row>();
		images=new ArrayList<Block>();
	}
	
	public void writeString(TextPosition text) {
		CharFont charfont=new CharFont(text.getFont().getName(),text.getHeightDir());
    	
    	Integer n;
    	n=content.charfonts.compute(charfont, (k,v) -> (v == null ? 0 : v) + 1);
    	content.charfonts.put(charfont,n);
    	String str;
    	str=text.toString();
    	Char ch=new Char(str, text.getXDirAdj(),text.getYDirAdj(),text.getWidthDirAdj(),text.getHeightDir(),
    			text.getFont().getName());
    	chars.add(ch);
    	
    	float width1=(float)(ch.x+ch.width-0.001);
    	float height1=(float)(ch.y+ch.height-0.001);
    	if (width1>width) width=width1;
    	if (height1>height) height=height1;
	}
	
	public void complete(PDPage pdPage) throws IOException {
		bitmap=new Bitmap(this);
		
		getAllRows();
		getAllCharBlocks();
		
		content.pdProcessor.processPage(pdPage);
		
		bitmap=null;
	}
	
	void markHeaderFooter() {
		headerY=1;
		footerY=Math.round(height);
		
		for(Block block:blocks) {
			if(block.type==Common._PageHeaderBlock)
				if(headerY<block.bottom)
					headerY=block.bottom;
			
			if(block.type==Common._PageFooterBlock)
				if(footerY>block.top)
					footerY=block.top;
		}
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
		
		for(HStretch columnStretch:columnStretches)
			columns.add(new Column(columnStretch.left,headerY,
					columnStretch.right,footerY));
	}
	
	private void getAllCharBlocks() {
		for(int x=0; x<=width;x++)
			for(int y=0;y<=height;y++) {
				Point p=bitmap.points[x][y];
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
				Point p=bitmap.points[x][y];
				if ( p == null ) continue;
				
				Char ch=p.ch;
				if(ch==null) continue;
				
				if(ch.row==null) {
					rows.add(new Row(this,x,y));
				}
			}
		
		Collections.sort(rows,Row.compareRows);
	}
	
	protected ArrayList<Block> topBlocks() {
		ArrayList<Block> tbs=new ArrayList<Block>();
		
		for(Block block:blocks)
			tbs.add(block);
		
		for(Block block:blocks)
			for(int i=0; i<tbs.size(); i++) {
				Block tb=tbs.get(i);
				if( tb.isHIntersected(block) && tb.top>block.bottom ) {
					tbs.remove(tb);
					i--;
				}
			}
		
		Collections.sort(tbs,Block.compareBlocks);
		
		return tbs;
	}
	
	protected ArrayList<Block> bottomBlocks() {
		ArrayList<Block> bbs=new ArrayList<Block>();
		
		for(Block block:blocks)
			bbs.add(block);
		
		for(Block block:blocks)
			for(int i=0; i<bbs.size(); i++) {
				Block bb=bbs.get(i);
				if( bb.isHIntersected(block) && bb.bottom<block.top ) {
					bbs.remove(bb);
					i--;
				}
			}
		
		Collections.sort(bbs,Block.compareBlocks);
		
		return bbs;
	}
	
	public void print(FileWriter fw) throws IOException {
		fw.write("==============================\n");
		fw.write(String.format("Page %d\nWidth %f Height %f\n",id,width,height));
		
		fw.write("\n\nColumns:\n----------------------\n");
		for(Column column:columns) {
			column.print(fw);
		}
		
		fw.write("\n\nBlocks:\n----------------------\n");
		for(Block block:blocks)
			block.print(fw);
		
		printImageBlocks(fw);
	}
	
	void printImageBlocks(FileWriter fw) throws IOException {
		fw.write("==============================\n");
		fw.write(String.format("Page %d images\n",id));
		for(Block block: images) {
			fw.write(String.format("image => left: %d, top: %d, right: %d, bottom: %d\n",
									block.left,block.top,block.right,block.bottom));
		}
	}
	
	String text() {
		String str="";
		
		for(Column column: columns) {
			str+=column.string(Block.textBlockFilter);
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

	class Bitmap {
		Point[][] points;
		
		public Bitmap(Page page) {
			points=new Point[Math.round(page.width)+1][Math.round(page.height)+1];
			
			for (Char ch: page.chars) {
				for (int x=Math.round(ch.x); x<=ch.right; x++)
					for (int y=Math.round(ch.y); y<=ch.bottom; y++) {
						Point point=new Char.Point(x,y,ch);
						points[x][y]=point;
					}
			}
		}
	}
	
	class Column extends Rectangle {
		ArrayList<Block> blocks;
		
		public Column(int left,int top,int right, int bottom) {
			super(left,top,right,bottom);
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
			fw.write(String.format("Column left:%d top:%d right:%d bottom %d\n",
									left,top,right,bottom));
			for(Block block:blocks)
				block.print(fw);
		}
		
		String string(BlockFilter blockFilter) {
			String str="";
			
			for(Block block:blocks) {
				if(! blockFilter.filter(block))
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
}
