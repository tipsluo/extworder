package extworder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

import extworder.Char.CharFont;
import extworder.Common.CharfontFilter;

public class Block extends Rectangle {
	CharFont charfont;
	ArrayList<Row> rows;
	Page page;
    
	static CompareBlocks compareBlocks=new CompareBlocks();
	
	public Block(Page page, int x, int y) {
		
		this.page=page;
		build(x,y);
		
		width=right-left;
		height=bottom-top;
		
		Collections.sort(rows,Row.compareRows);

		charfont=mostCharFont();
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
	
	void merge(Block block) {
		for (Row row:block.rows) {
			row.block=this;
			updateRectangle(row);
		}
		
		rows.addAll(block.rows);
		
		Collections.sort(rows,Row.compareRows);
		
		page.blocks.remove(block);
	}
	
	public CharFont mostCharFont() {
		TreeMap<CharFont,Integer> charFonts=new TreeMap<>();
		
		for (Row row: rows) {
			int n=charFonts.compute(row.charfont, (k,v) -> (v == null ? 0 : v) + 1);
        	charFonts.put(row.charfont,n);
		}
		
		CharFont cf=charFonts.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		return cf;
	}
	
	public void print(FileWriter fw) throws IOException {
		fw.write("==============================\n");
		
		Content content=page.content;
		
		if (this==page.content.titleBlock)
			fw.write("type: title");
		else if (charfont.compareTo(content.textCharfont)>0)
			fw.write(String.format("type: subtitle level%d",content.charfontIndexes.get(charfont)));
		else if (charfont.equals(content.textCharfont))
			fw.write(String.format("type: text"));
		else 
			fw.write(String.format("type: notes"));
					
		fw.write(String.format("\ntypeindex=%d left=%d right=%d top=%d bottom=%d width=%f height %f\n====>\n\n",
				content.charfontIndexes.get(charfont),left,right,top,bottom,width,height));
		
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
	
	/*String text() {
		if(! charfont.equals(page.content.textCharfont))
			return "";
		
		String str="";
		
		int y=rows.get(0).bottom;
		for(Row row:rows) {
			if (row.top>y) {
				str+="\n";
				y=row.bottom;
			} else {
				str+=" ";
			}
			str+=row.string();
		}
		
		return str+"\n";
	}*/
	
	/*String text() {
		if(! this.charfont.equals(page.content.textCharfont))
			return "";
		return string();
	}*/
	
	String string() {
		String str="";
		
		int y=rows.get(0).bottom;
		for(Row row:rows) {
			if (row.top>y) {
				str+="\n";
				y=row.bottom;
			} else {
				str+=" ";
			}
			str+=row.string();
		}
		
		return str+"\n";
	}
	
	static class CompareBlocks implements Comparator<Block> {
		public int compare(Block b1, Block b2) {
			if (b1.bottom < b2.top)
				return -1;
			else if (b2.bottom < b1.top)
				return 1;
			else 
				return b1.top!=b2.top ? b1.top-b2.top : b1.left-b2.left;
		}
	}
}
