package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.pdfbox.text.TextPosition;

public class Page {
	Content content;
	int id;
    ArrayList<Char> chars;
    ArrayList<Block> blocks;
    ArrayList<Row> rows;
    public float width,height;
    //public int right,bottom;
    Bitmap bitmap;
    //public String title;
	
	public Page(Content content,int id) {
		this.content=content;
		this.id=id;
		width=height=-1;
		chars=new ArrayList<Char>();
		blocks=new ArrayList<Block>();
		rows=new ArrayList<Row>();
	}
	
	public void writeString(TextPosition text) {
    	Float h;
    	h=text.getHeightDir();
    	
    	Integer n;
    	n=content.charHeights.compute(h, (k,v) -> (v == null ? 0 : v) + 1);
    	content.charHeights.put(h,n);
    	
    	String str;
    	str=text.toString();
    	Char ch=new Char(str, text.getXDirAdj(),text.getYDirAdj(),text.getWidthDirAdj(),text.getHeightDir(),
    			text.getFont().getName());
    	//Char ch=new Char(str, text.getXDirAdj(),text.getYDirAdj(),text.getFont().getFontDescriptor().getFontBoundingBox().getHeight(),text.getWidthDirAdj(),
            		//	text.getFont().getName());
    	chars.add(ch);
    	
    	float width1=(float)(ch.x+ch.width-0.001);
    	float height1=(float)(ch.y+ch.height-0.001);
    	if (width1>width) width=width1;
    	if (height1>height) height=height1;
	}
	
	public void complete() {
		/*right=Math.round(width);
		bottom=Math.round(height);*/
		
		bitmap=new Bitmap(this);
		
		getAllRows();
		getAllBlocks();
	}
	
	public void getAllBlocks() {
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
		
		Comparator<Block> compareByYX = (Block b1, Block b2) ->
			b1.top != b2.top ? (int)(b1.top-b2.top) : (int) (b1.left-b2.left);
		Collections.sort(blocks,compareByYX);
	}
	
	public void getAllRows() {
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
		
		Comparator<Row> compareByYX = (Row r1, Row r2) ->
			r1.left != r2.left ? (int)(r1.left-r2.left) : (int) (r1.top-r2.top);
		Collections.sort(rows,compareByYX);
	}
	
	
	public void print(FileWriter fw) throws IOException {
		fw.write("==============================\n");
		fw.write(String.format("Page %d\n",id));
		for(Block block:blocks)
			block.print(fw);
	}

	class Bitmap {
		Point[][] points;
		
		public Bitmap(Page page) {
			points=new Point[Math.round(page.width)+1][Math.round(page.height)+1];
			
			for (Char ch: page.chars) {
				for (int x=Math.round(ch.x); x<=ch.right; x++)
					for (int y=Math.round(ch.y); y<=ch.bottom; y++) {
						Point point=new Point(x,y,ch);
						points[x][y]=point;
					}
			}
		}
	}
}
