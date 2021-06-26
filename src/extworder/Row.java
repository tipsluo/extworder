package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

import extworder.Char.CharFont;

public class Row extends Rectangle {
	//float charHeight;
	//String charFont;
	Char.CharFont charfont;
	ArrayList<Char> chars;
	Block block;
	Page page;
	
	static Comparator<Row> compareByYX = (Row r1, Row r2) ->
	r1.left != r2.left ? (int)(r1.left-r2.left) : (int) (r1.top-r2.top);
	
	public Row(Page page, int x, int y) {
		this.page=page;
		build(x,y);
		width=right-left;
		height=bottom-top;
	}
	
	public void build(int x,int y) {
		chars=new ArrayList<Char>();
		
		if ( page.bitmap.points[x][y].ch != null )
			expand(page.bitmap.points[x][y].ch);
		
		Collections.sort(chars,Char.compareByYX);

		/*charHeight=mostCharHeight();
		charFont=mostCharFont();*/
		charfont=mostCharFont();
	}

	private void expand(Char ch) {
		if (ch==null) return;
				
		if (ch.row==null) {
			chars.add(ch); 
			ch.row=this;
			
			updateRectangle(ch);
			
			ch.getLeftConnected(page).forEach(this::expand);
			ch.getRightConnected(page).forEach(this::expand);
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
				if (page.bitmap.points[i][j]!=null) break;
			}
			
			if (j<0 || page.bitmap.points[i][j]==null) continue;
			
			if (row==page.bitmap.points[i][j].ch.row) continue;
			
			row=page.bitmap.points[i][j].ch.row;
			
			if(!checkSameBlock(row)) continue;
			
			if( j1 > row.height*Common._CharVGapRatio ) continue;
			
			rows.add(row);
		}
		
		return rows;
	}

	public ArrayList<Row> getBelowConnected() {		
		ArrayList<Row> rows=new ArrayList<Row>();
		
		if (bottom >= page.height) return rows;
		
		Row row=null;
		
		for (int i=left; i<=right; i++) {
			int j=1;
			int j1=1;
			for (;j1<height*Common._CharVGapRatio;j1++) {
				j=bottom+j1;
				if (j>=page.height) break;
				if (page.bitmap.points[i][j]!=null) break;
			}
			
			if(j>page.height || page.bitmap.points[i][j]==null) continue;
			if (row==page.bitmap.points[i][j].ch.row) continue;
			
			row=page.bitmap.points[i][j].ch.row;
			
			if(!checkSameBlock(row)) continue;
			
			if( j1 > row.height*Common._CharVGapRatio ) continue;
			
			rows.add(row);
		}
		
		return rows;
	}
	
	private boolean checkSameBlock(Row row) {
		//return charHeight==row.charHeight && charFont==row.charFont;
		return charfont.equals(row.charfont);
	}
	
	public CharFont mostCharFont() {
		TreeMap<CharFont,Integer> charFonts=new TreeMap<>();
		
		for (Char ch: chars) {
			CharFont cf=new Char.CharFont(ch.fontname,ch.height);
			int n=charFonts.compute(cf, (k,v) -> (v == null ? 0 : v) + 1);
        	charFonts.put(cf,n);
		}
		
		CharFont cf=charFonts.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return cf;
		/*Float maxHeight = charHeights.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return maxHeight;*/
	}
	
	/*public float mostCharHeight() {
		TreeMap<Float,Integer> charHeights=new TreeMap<>();
		
		for (Char ch: chars) {
			int n=charHeights.compute(ch.height, (k,v) -> (v == null ? 0 : v) + 1);
        	charHeights.put(ch.height,n);
		}
		
		Float maxHeight = charHeights.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return maxHeight;
	}
	
	public String mostCharFont() {
		TreeMap<String,Integer> charFonts=new TreeMap<>();
		
		for (Char ch: chars) {
			int n=charFonts.compute(ch.fontname, (k,v) -> (v == null ? 0 : v) + 1);
        	charFonts.put(ch.fontname,n);
		}
		
		String maxFont = charFonts.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return maxFont;
	}*/
	
	public void print(FileWriter fw) throws IOException {
		int x0=chars.get(0).right;
		for(Char ch:chars) {
			
			if(ch.left > x0 + ch.width * Common._HSpaceMin) {
				fw.write(" ");
			}
			fw.write(ch.str);
			x0=ch.right;
		}
	}
}
