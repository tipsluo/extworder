package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

import extworder.Char.CharFont;

public class Row extends Rectangle {
	Char.CharFont charfont;
	ArrayList<Char> chars;
	Block block;
	Page page;
	float width,height;
	
	static Comparator<Row> compareRows = (Row r1, Row r2) ->
		r1.upper != r2.upper ? Common.compareValue(r1.upper,r2.upper) : Common.compareValue(r1.left,r2.left);
		//r1.upper != r2.upper ? (int)(r1.upper-r2.upper) : (int) (r1.left-r2.left);
	
	public Row(Page page, int x, int y) {
		this.page=page;
		build(x,y);
		width=right-left;
		height=lower-upper;
	}
	
	public void build(int x,int y) {
		chars=new ArrayList<Char>();
		
		if ( page.pageBitmap.points[x][y].ch != null )
			expand(page.pageBitmap.points[x][y].ch);
		
		Collections.sort(chars,Char.compareChars);

		charfont=getCharFont();
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
	
	void merge(Row row) {
		for (Char ch:row.chars) {
			ch.row=this;
			updateRectangle(ch);
		}
		
		chars.addAll(row.chars);
		
		Collections.sort(chars,Char.compareChars);

		charfont=getCharFont();
		
		if (row.block!=null)
			row.block.rows.remove(row);
		page.rows.remove(row);
	}
	
	public ArrayList<Row> getAboveConnected() {		
		ArrayList<Row> rows=new ArrayList<Row>();
		
		if (upper<1) return rows;
		
		Row row=null;
		
		for (int i=left; i<=right; i++) {
			int j=1;
			int j1=1;
			float maxDist=charfont.height*Common._CharVGapRatio;
			for (;j1<maxDist;j1++) {
				j=upper-j1;
				if (j<0) break;
				if (page.pageBitmap.points[i][j]!=null) break;
			}
			
			if (j<0 || page.pageBitmap.points[i][j]==null) continue;
			
			if (row==page.pageBitmap.points[i][j].ch.row) continue;
			
			row=page.pageBitmap.points[i][j].ch.row;
			
			if(!checkSameBlock(row)) continue;
			
			if( j1 > row.charfont.height*Common._CharVGapRatio ) continue;
			
			rows.add(row);
		}
		
		return rows;
	}

	public ArrayList<Row> getBelowConnected() {		
		ArrayList<Row> rows=new ArrayList<Row>();
		
		if (lower >= page.lower) return rows;
		
		Row row=null;
		
		for (int i=left; i<=right; i++) {
			int j=1;
			int j1=1;
			float maxDist=charfont.height*Common._CharVGapRatio;
			for (;j1<maxDist;j1++) {
				j=lower+j1;
				if (j>=page.lower) break;
				if (page.pageBitmap.points[i][j]!=null) break;
			}
			
			if(j>page.lower || page.pageBitmap.points[i][j]==null) continue;
			if (row==page.pageBitmap.points[i][j].ch.row) continue;
			
			row=page.pageBitmap.points[i][j].ch.row;
			
			if(!checkSameBlock(row))
				continue;
			
			if( j1 > row.charfont.height*Common._CharVGapRatio ) continue;
			
			rows.add(row);
		}
		
		return rows;
	}
	
	private boolean checkSameBlock(Row row) {
		return charfont.equals(row.charfont);
	}
	
	private CharFont getCharFont() {
		TreeMap<CharFont,Integer> charFonts=new TreeMap<>();
		
		for (Char ch: chars) {
			CharFont cf=new Char.CharFont(ch.fontname,ch.height);
			
			if(cf.equals(page.content.textCharfont))
				return cf;
			
			int n=charFonts.compute(cf, (k,v) -> (v == null ? 0 : v) + 1);
        	charFonts.put(cf,n);
		}
		
		CharFont cf=charFonts.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return cf;
	}
	
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
	
	String string() {
		String str="";
		
		int x0=chars.get(0).right;
		for(Char ch:chars) {
			if(ch.left > x0 + ch.height * Common._HSpaceMin) {
				str+=" ";
			}
			str+=ch.str;
			x0=ch.right;
		}
		
		return str;
	}
}
