package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

public class Row extends Rectangle {
	float charHeight;
	ArrayList<Char> chars;
	Block block;
	
	public static void getAllRows() {
		for(int x=0; x<=Content.content.width;x++)
			for(int y=0;y<=Content.content.height;y++) {
				Point p=Content.content.bitmap.points[x][y];
				if ( p == null ) continue;
				
				Char ch=p.ch;
				if(ch==null) continue;
				
				if(ch.row==null) {
					Content.content.rows.add(new Row(x,y));
				}
			}
		
		Comparator<Row> compareByYX = (Row r1, Row r2) ->
			r1.left != r2.left ? (int)(r1.left-r2.left) : (int) (r1.top-r2.top);
		Collections.sort(Content.content.rows,compareByYX);
	}
	
	public Row(int x, int y) {
		build(x,y);
		width=right-left+1;
		height=bottom-top+1;
	}
	
	public void build(int x,int y) {
		chars=new ArrayList<Char>();
		if ( Content.content.bitmap.points[x][y].ch != null )
			expand(Content.content.bitmap.points[x][y].ch);
		
		Comparator<Char> compareByYX = (Char ch1, Char ch2) ->
			ch1.y != ch2.y ? (int)(ch1.y-ch2.y) : (int) (ch1.x-ch2.x);
		Collections.sort(chars,compareByYX);

		charHeight=mostCharHeight();
	}

	private void expand(Char ch) {
		if (ch==null) return;
				
		if (ch.row==null) {
			chars.add(ch); 
			ch.row=this;
			
			updateRectangle(ch);
			
			ch.getLeftConnected().forEach(this::expand);
			ch.getRightConnected().forEach(this::expand);
		}	
	}
	
	public ArrayList<Row> getAboveConnected() {		
		ArrayList<Row> rows=new ArrayList<Row>();
		
		if (top<1) return rows;
		
		Row row=null;
		
		for (int i=left; i<=right; i++) {
			int j=1;
			int j1=1;
			for (;j1<height*Common._CharVGapRatio;j1++) {
				j=top-j1;
				if (j<0) break;
				if (Content.content.bitmap.points[i][j]!=null) break;
			}
			
			if (j<0 || Content.content.bitmap.points[i][j]==null) continue;
			
			if (row==Content.content.bitmap.points[i][j].ch.row) continue;
			
			row=Content.content.bitmap.points[i][j].ch.row;
			
			if(!checkSameBlock(row)) continue;
			
			if( j1 > row.height*Common._CharVGapRatio ) continue;
			
			rows.add(row);
		}
		
		return rows;
	}

	public ArrayList<Row> getBelowConnected() {		
		ArrayList<Row> rows=new ArrayList<Row>();
		
		if (bottom >= Content.content.bottom) return rows;
		
		Row row=null;
		
		for (int i=left; i<=right; i++) {
			int j=1;
			int j1=1;
			for (;j1<height*Common._CharVGapRatio;j1++) {
				j=bottom+j1;
				if (j>=Content.content.bottom) break;
				if (Content.content.bitmap.points[i][j]!=null) break;
			}
			
			if(j>Content.content.bottom || Content.content.bitmap.points[i][j]==null) continue;
			if (row==Content.content.bitmap.points[i][j].ch.row) continue;
			
			row=Content.content.bitmap.points[i][j].ch.row;
			
			if(!checkSameBlock(row)) continue;
			
			if( j1 > row.height*Common._CharVGapRatio ) continue;
			
			rows.add(row);
		}
		
		return rows;
	}
	
	private boolean checkSameBlock(Row row) {
		return charHeight==row.charHeight;
	}
	
	public float mostCharHeight() {
		TreeMap<Float,Integer> charHeights=new TreeMap<>();
		
		for (Char ch: chars) {
			int n=charHeights.compute(ch.height, (k,v) -> (v == null ? 0 : v) + 1);
        	charHeights.put(ch.height,n);
		}
		
		Float maxHeight = charHeights.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return maxHeight;
	}
	
	public void write(FileWriter fw) throws IOException {
		//int y0=chars.get(0).bottom;
		int x0=chars.get(0).right;
		for(Char ch:chars) {
			/*if (ch.top>y0) {
				fw.write("\n");
			}*/
			
			if(ch.left > x0+Common._HSpaceMin) {
				fw.write(" ");
			}
			fw.write(ch.str);
			
			//y0=ch.bottom;
			x0=ch.right;
		}
	}
}
