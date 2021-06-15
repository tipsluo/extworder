package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeMap;

public class Block {
	int left=9999,top=9999,right=0,bottom=0;
	float width,height;
	float charHeight;
	Content content;
	ArrayList<Char> chars;
	
	final static float _CharHGapRatio=1f;
	final static float _CharVGapRatio=1f;
	final static float _HSpaceMin=1;
	
	public static void getAllBlocks(Content content) {
		for(int x=0; x<=content.width;x++)
			for(int y=0;y<=content.height;y++) {
				Point p=content.bitmap.points[x][y];
				if ( p == null ) continue;
				
				Char ch=p.ch;
				if(ch==null) continue;
				
				if(ch.block==null) {
					content.blocks.add(new Block(content,x,y));
				}
			}
		
		Comparator<Block> compareByYX = (Block b1, Block b2) ->
			b1.top != b2.top ? (int)(b1.top-b2.top) : (int) (b1.left-b2.left);
		Collections.sort(content.blocks,compareByYX);
	}
	
	public Block(Content content,int x, int y) {
		this.content=content;
		
		chars=new ArrayList<Char>();
		
		if ( content.bitmap.points[x][y].ch != null )
			expand(content.bitmap.points[x][y].ch);
		
		Comparator<Char> compareByYX = (Char ch1, Char ch2) ->
											ch1.y != ch2.y ? (int)(ch1.y-ch2.y) : (int) (ch1.x-ch2.x);
		Collections.sort(chars,compareByYX);
		
		charHeight=mostCharHeight();
	}
	
	private void expand(Char ch) {
		if (ch==null) return;
				
		if (ch.block==null) {
			chars.add(ch); 
			ch.block=this;
			
			updateRectangle(ch);
			
			ch.getAboveConnected(content).forEach(this::expand);
			ch.getBelowConnected(content).forEach(this::expand);
			ch.getLeftConnected(content).forEach(this::expand);
			ch.getRightConnected(content).forEach(this::expand);
		}	
	}
	
	private void updateRectangle(Char ch) {
		if (left>ch.left) left=ch.left;
		if (right<ch.right) right=ch.right;
		if (top>ch.top) top=ch.top;
		if (bottom<ch.bottom) bottom=ch.bottom;
	}
	
	private float mostCharHeight() {
		TreeMap<Float,Integer> charHeights=new TreeMap<>();
		
		for (Char ch: chars) {
			int n=charHeights.compute(ch.height, (k,v) -> (v == null ? 0 : v) + 1);
        	charHeights.put(ch.height,n);
		}
		
		Float maxHeight = charHeights.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return maxHeight;
	}
	
	public void write(FileWriter fw) throws IOException {
		fw.write("==============================\n");
		
		if (charHeight>=content.titleCharHeight)
			fw.write("type: title");
		else if (charHeight>content.textCharHeight)
			fw.write(String.format("type: subtitle level%d",content.charHeightIndexes.get(charHeight)));
		else if (charHeight==content.textCharHeight)
			fw.write(String.format("type: text"));
		else
			fw.write(String.format("type: notes"));
					
		fw.write(String.format("\ntypeindex=%d left=%d right=%d top=%d bottom=%d \n====>\n\n",
				content.charHeightIndexes.get(charHeight),left,right,top,bottom));
		
		int y0=chars.get(0).bottom;
		int x0=chars.get(0).right;
		for(Char ch:chars) {
			if (ch.top>y0) {
				fw.write("\n");
				y0=ch.bottom;
				x0=ch.right;
			}
			if(ch.left > x0+_HSpaceMin) {
				fw.write(" ");
			}
			fw.write(ch.str);
			
			x0=ch.right;
		}
		
		fw.write("\n==============================\n\n");
	}
}
