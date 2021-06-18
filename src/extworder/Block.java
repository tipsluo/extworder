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

public class Block extends Rectangle {
	float rowHeight;
	ArrayList<Row> rows;
	
	public static void getAllBlocks() {
		for(int x=0; x<=Content.content.width;x++)
			for(int y=0;y<=Content.content.height;y++) {
				Point p=Content.content.bitmap.points[x][y];
				if ( p == null ) continue;
				
				Char ch=p.ch;
				if(ch==null) continue;
				
				if(ch.row.block==null) {
					Content.content.blocks.add(new Block(x,y));
				}
			}
		
		Comparator<Block> compareByYX = (Block b1, Block b2) ->
			b1.top != b2.top ? (int)(b1.top-b2.top) : (int) (b1.left-b2.left);
		Collections.sort(Content.content.blocks,compareByYX);
	}
	
	public Block(int x, int y) {
		build(x,y);
		
		width=right-left;
		height=bottom-top;
		
		Comparator<Row> compareByYX = (Row row1, Row row2) ->
											row1.top != row2.top ? (int)(row1.top-row2.top) : (int) (row1.left-row2.left);
		Collections.sort(rows,compareByYX);
		
		rowHeight=mostRowHeight();
	}
	
	public void build(int x, int y) {
		rows=new ArrayList<Row>();
		
		if ( Content.content.bitmap.points[x][y].ch != null )
			expand(Content.content.bitmap.points[x][y].ch.row);
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
	
	public float mostRowHeight() {
		TreeMap<Float,Integer> rowHeights=new TreeMap<>();
		
		for (Row row: rows) {
			int n=rowHeights.compute(row.charHeight, (k,v) -> (v == null ? 0 : v) + 1);
        	rowHeights.put(row.charHeight,n);
		}
		
		Float maxHeight = rowHeights.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return maxHeight;
	}
	
	public void write(FileWriter fw) throws IOException {
		fw.write("==============================\n");
		
		if (rowHeight>=Content.content.titleCharHeight)
			fw.write("type: title");
		else if (rowHeight>Content.content.textCharHeight)
			fw.write(String.format("type: subtitle level%d",Content.content.charHeightIndexes.get(rowHeight)));
		else if (rowHeight==Content.content.textCharHeight)
			fw.write(String.format("type: text"));
		else 
			fw.write(String.format("type: notes"));
					
		fw.write(String.format("\ntypeindex=%d left=%d right=%d top=%d bottom=%d width=%f height %f\n====>\n\n",
				Content.content.charHeightIndexes.get(rowHeight),left,right,top,bottom,width,height));
		
		int y=rows.get(0).bottom;
		for(Row row:rows) {
			if (row.top>y) {
				fw.write("\n");
				y=row.bottom;
			} else {
				fw.write(" ");
			}
			row.write(fw);
		}
		
		fw.write("\n==============================\n\n");
	}
}
