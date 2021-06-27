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

import extworder.Char.CharFont;

public class Page {
	Content content;
	int id;
    ArrayList<Char> chars;
    ArrayList<Block> blocks;
    ArrayList<Row> rows;
    ArrayList<BlockGroup> blockgroups;
    public float width,height;
    Bitmap bitmap;
	
	public Page(Content content,int id) {
		this.content=content;
		this.id=id;
		width=height=-1;
		chars=new ArrayList<Char>();
		blocks=new ArrayList<Block>();
		rows=new ArrayList<Row>();
		blockgroups=new ArrayList<BlockGroup>();
	}
	
	public void writeString(TextPosition text) {
    	/*Float h;
    	h=text.getHeightDir();*/
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
	
	public void complete() {
		bitmap=new Bitmap(this);
		
		getAllRows();
		getAllBlocks();
		
		bitmap=null;
		
		arrangeBlocks();
	}
	
	private void arrangeBlocks() {
		for (Block block: blocks) {
			boolean joined=false;
			for(BlockGroup blockgroup: blockgroups) {
				if (blockgroup.left == block.left || blockgroup.right==block.right) {
					blockgroup.addBlock(block);
					joined=true;
					break;
				}
			}
			
			if (! joined) {
				blockgroups.add(new BlockGroup(block));
			} 
		}
		
		Collections.sort(blockgroups,BlockGroup.compareBlockgroups);
	}
	
	private boolean validateBlockGroup(ArrayList<BlockGroup> blockGroups) {
		Collections.sort(blockGroups,BlockGroup.compareBlockgroups);

		for(int i=0; i<blockGroups.size(); i++) {
			BlockGroup blockGroup=blockGroups.get(i);
			for(Block block:blockGroup.blocks) {
				for(int j=0;j<i;j++) {
					BlockGroup bg1=blockGroups.get(j);
					for(int k=0;k<bg1.blocks.size();k++) {
						Block b1=bg1.blocks.get(k);
						if ( block.left<b1.right && block.bottom<b1.top ) {
							BlockGroup newBlockGroup=new BlockGroup(b1);
							int n=bg1.blocks.size();
							for(int l=k; l<n; l++)
								newBlockGroup.blocks.add(bg1.blocks.get(l));
							newBlockGroup.reUpdateRectangle();
							blockGroups.add(newBlockGroup);
							
							bg1.truncate(k);
							if(bg1.blocks.size()==0)
								blockGroups.remove(bg1);
							
							return false;
						}
					}
				}
			}
		}
		
		return true;
	}
	
	private void getAllBlocks() {
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
	
	
	public void print(FileWriter fw) throws IOException {
		fw.write("==============================\n");
		fw.write(String.format("Page %d\n",id));
		for(BlockGroup blockGroup:blockgroups)
			blockGroup.print(fw);
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
	
	static class BlockGroup extends Rectangle {
		ArrayList<Block> blocks;
		static CompareBlockGroups compareBlockgroups=new CompareBlockGroups();
		/*static Comparator<BlockGroup> compareByYX = (BlockGroup bg1, BlockGroup bg2) ->
			bg1.top != bg2.top ? (int)(bg1.top-bg2.top) : (int) (bg1.left-bg2.left);*/
		
		public BlockGroup(Block block) {
			blocks=new ArrayList<Block>();
			addBlock(block);
		}
		
		public void addBlock(Block block) {
			blocks.add(block);
			updateRectangle(block);
		}
		
		public void removeBlock(Block block) {
			blocks.remove(block);
			
			for (Block b:blocks) {
				updateRectangle(b);
			}
		}
		
		public void truncate(int firstBlockIndex) {
			int n=blocks.size();
			for(int i=firstBlockIndex; i<n; i++)
				blocks.remove(firstBlockIndex);
			reUpdateRectangle();
		}
		
		public void reUpdateRectangle() {
			for (Block b:blocks) 
				updateRectangle(b);
		}
		
		public void print(FileWriter fw) throws IOException  {
			fw.write(String.format("BlockGroup left:%d====================>\n",left));
			for(Block block:blocks)
				block.print(fw);
		}
		
		static class CompareBlockGroups implements Comparator<BlockGroup> {
			public int compare(BlockGroup bg1, BlockGroup bg2) {
				if (bg1.bottom < bg2.top)
					return -1;
				else if (bg2.bottom < bg1.top)
					return 1;
				else 
					return bg1.bottom!=bg2.bottom ? bg1.bottom-bg2.bottom : bg1.right-bg2.right;
			}
		}
	}
}
