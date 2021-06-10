package extworder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class Block {
	int left=9999,top=9999,right=0,bottom=0;
	float width,height;
	Content content;
	ArrayList<Char> chars;
	
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
}
