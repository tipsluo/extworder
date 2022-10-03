package extworder;

import java.util.ArrayList;
import java.util.Comparator;

import org.apache.pdfbox.pdmodel.font.PDFont;

import extworder.Common.SortedList;

public class Char2 extends Rectangle {
	String str;
	PDFont font;
	Row2 row;
	SortedList<Row2> rowCandidates;
	//SortedList<Block2> blockCandidates;
	
	static Comparator<Char2> compareChars = (Char2 ch1, Char2 ch2) ->
		ch1.left!=ch2.left ? (int)(ch1.left-ch2.left) : (int)(ch1.upper-ch2.upper);
	
	public Char2() {
		super();
	}
	
	public Char2(Char2 ch, String str , float wid,float hei) {
		super();
		this.str=String.copyValueOf(str.toCharArray());
		height=hei;
		width=wid;
		font=ch.font;
		left=ch.left;
		right=ch.right;
		upper=ch.upper;
		lower=ch.lower;
		row=ch.row;
	}
		
	public Char2(String str,float x, float y, float width, float height, PDFont font) {
		super();
		
		this.str=String.copyValueOf(str.toCharArray());
		this.height=(float)(Math.round(height*10)/10f);
		this.width=(float)(Math.round(width*10)/10f);
		this.font=font;
		left=Math.round(x);
		right=(int)(Math.round(x+this.width));
		upper=Math.round(y);
		lower=(int)(Math.round(y+this.height));
		
		rowCandidates=new SortedList<Row2>();
		//blockCandidates=new SortedList<Block2>();
	}
	
	public ArrayList<Char2> getLeftConnected(Page2 page, float hInterval, int vAdj) {		
		ArrayList<Char2> chars=new ArrayList<Char2>();
		
		if (left<1) return chars;
		
		Char2 ch=null;
		
		int u=upper-vAdj;
		if(u<1)
			u=1;
		
		int l=lower+vAdj;
		if(l>page.lower)
			l=page.lower;
		
		for (int j=u; j<=l; j++) {
			int i=left;
			for(int i1=0; i1 <= hInterval; i1++) {
				i=left-i1;
				if (i<0) break;
				if (page.pageBitmap.points[i][j]!=null) {
					if(page.pageBitmap.points[i][j].ch==this)
						continue;
					break;
				}
			}
					
			if (i<0 || 
				 page.pageBitmap.points[i][j]==null || page.pageBitmap.points[i][j].ch==null) 
				continue;
			if (ch==page.pageBitmap.points[i][j].ch) continue;
			
			ch=page.pageBitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}
	
	public ArrayList<Char2> getRightConnected(Page2 page, float hInterval, int vAdj) {		
		ArrayList<Char2> chars=new ArrayList<Char2>();
		
		if (right >= page.right) return chars;
		
		Char2 ch=null;
		
		int u=upper-vAdj;
		if(u<1)
			u=1;
		
		int l=lower+vAdj;
		if(l>page.lower)
			l=page.lower;
		
		for (int j=u; j<=l; j++) {
			int i=-1;
			for(int i1=0; i1 <= hInterval ; i1++) {
				i=right+i1;
				if (i>page.right) break;
				if (page.pageBitmap.points[i][j]!=null) {
					if(page.pageBitmap.points[i][j].ch==this)
						continue;
					break;
				}
			}
			
			if(i==-1)
				continue;
			
			if (i>=page.right || 
					page.pageBitmap.points[i][j]==null || page.pageBitmap.points[i][j].ch==null) 
				continue;
			if (page.pageBitmap.points[i][j].ch!=null && ch==page.pageBitmap.points[i][j].ch) continue;
			
			ch=page.pageBitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}
	
	public ArrayList<Char2> getUpperConnected(Page2 page, int vGap) {		
		ArrayList<Char2> chars=new ArrayList<Char2>();
		
		if (upper <= page.upper) return chars;
		
		Char2 ch=null;
		
		for (int i=left; i<=right; i++) {
			int j=-1;
			for(int j1=1; j1<=vGap; j1++) {
				j=upper-j1;
				if (j<page.upper) break;
				if (page.pageBitmap.points[i][j]!=null) break;
			}
			
			if(j==-1)
				continue;
			
			if (j<=page.upper || 
					page.pageBitmap.points[i][j]==null || page.pageBitmap.points[i][j].ch==null) 
				continue;
			if (ch==page.pageBitmap.points[i][j].ch) continue;
			
			ch=page.pageBitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}
	
	public ArrayList<Char2> getLowerConnected(Page2 page, int vGap) {		
		ArrayList<Char2> chars=new ArrayList<Char2>();
		
		if (lower >= page.lower) return chars;
		
		Char2 ch=null;
		
		for (int i=left; i<=right; i++) {
			int j=-1;
			for(int j1=1; j1<=vGap; j1++) {
				j=lower+j1;
				if (j>page.lower) break;
				if (page.pageBitmap.points[i][j]!=null) break;
			}
			
			if(j==-1)
				continue;
			
			if (j>=page.lower || 
					page.pageBitmap.points[i][j]==null || page.pageBitmap.points[i][j].ch==null) 
				continue;
			if (ch==page.pageBitmap.points[i][j].ch) continue;
			
			ch=page.pageBitmap.points[i][j].ch;
			chars.add(ch);
		}
		
		return chars;
	}
	
	void registerBlock(Block2 block) {
		//blockCandidates.addSortUniq(block);
	}
	
	void registerRow(Row2 row) {
		rowCandidates.addSortUniq(row);
	}
	
	static public class Point {
		int x,y;
		Char2 ch;
		
		public Point(int x, int y, Char2 ch) {
			this.x=x;
			this.y=y;
			this.ch=ch;
		}
	}
	
	static public class VirtualChar extends Char2 {
		public VirtualChar(String str, float height, PDFont font) {
			super(str,-1,-1,-1,height,font);
		}
	}
}
