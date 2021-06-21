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
	Page page;
	
	public Block(Page page, int x, int y) {
		this.page=page;
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
		
		if ( page.bitmap.points[x][y].ch != null )
			expand(page.bitmap.points[x][y].ch.row);
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
	
	public void print(FileWriter fw) throws IOException {
		fw.write("==============================\n");
		
		Content content=page.content;
		
		if (rowHeight>=content.titleCharHeight)
			fw.write("type: title");
		else if (rowHeight>content.textCharHeight)
			fw.write(String.format("type: subtitle level%d",content.charHeightIndexes.get(rowHeight)));
		else if (rowHeight==content.textCharHeight)
			fw.write(String.format("type: text"));
		else 
			fw.write(String.format("type: notes"));
					
		fw.write(String.format("\ntypeindex=%d left=%d right=%d top=%d bottom=%d width=%f height %f\n====>\n\n",
				content.charHeightIndexes.get(rowHeight),left,right,top,bottom,width,height));
		
		int y=rows.get(0).bottom;
		for(Row row:rows) {
			if (row.top>y) {
				fw.write("\n");
				y=row.bottom;
			} else {
				fw.write(" ");
			}
			row.print(fw);
		}
		
		fw.write("\n==============================\n\n");
	}
}
