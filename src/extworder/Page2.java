package extworder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.TextPosition;

public class Page2 extends Rectangle {
	public Content2 content;
	public int id;
    public ArrayList<Char2> chars;
    public ArrayList<Block2> blocks;
    public ArrayList<Row2> rows;
    PageBitmap pageBitmap;
    public PageImg pageImg;
	private int xOffset;
	private int yOffset;
	
	final static int _MaxHInterval=20;
	final static int _MaxVInterval=20;
    
	public Page2(Content2 content,int id) {
		this.content=content;
		this.id=id;
		chars=new ArrayList<Char2>();
		blocks=new ArrayList<Block2>();
		rows=new ArrayList<Row2>();
	}
	
	public void writeString(TextPosition text) {
		//if rotated skip it
    	if(text.getX()!=text.getXDirAdj()) {
    		return;
    	}
    	
    	String str;
    	str=text.toString();
    	
    	Char2 ch=new Char2(str, text.getXDirAdj(),text.getYDirAdj()-text.getHeight(),text.getWidthDirAdj(),text.getHeight(),
    	    		text.getFont());
    	
       	chars.add(ch);	
       	
    	updateRectangle(ch);
	}
	
	public void complete(PDPage pdPage, boolean ignoreColoredBlock) throws IOException {
		if(chars.size()==0)
			return;
		
		adjustCoordinates();
		
		pageBitmap=new PageBitmap(this);
		eliminateCharIntersections();
		
		Collections.sort(chars,Char2.compareChars);
		
		for(int hItv=1; hItv<_MaxHInterval; hItv++) {
			for(Char2 ch: chars) {
				if(ch.row!=null)
					continue;
				
				Row2 row=new Row2(ch,hItv,this);
				
				for(int vItv=1; vItv<_MaxVInterval; vItv++) {
					Block2 block=new Block2(row,vItv,this);
			}
		}
	}
	
	private void adjustCoordinates() {
		xOffset=left-1;
		yOffset=upper-1;
		
		for(Char2 ch:chars) {
			ch.left=ch.left-xOffset;
			ch.upper=ch.upper-yOffset;
			ch.right=ch.right-xOffset;
			ch.lower=ch.lower-yOffset;
		}
		
		left=left-xOffset;
		right=right-xOffset;
		upper=upper-yOffset;
		lower=lower-yOffset;
		
		width=right-left;
		height=lower-upper;
	}
	
	private void eliminateCharIntersections() {
		for(int x=left; x<=right;x++)
			for(int y=upper;y<=lower;y++) {
				if(pageBitmap.points[x][y]==null)
					continue;
				
				Char2 ch=pageBitmap.points[x][y].ch;
				if(ch==null)
					continue;
				
				ArrayList<Char2> rights=ch.getRightConnected(this,1,0);
				if(rights.size()==0)
					continue;
				
				for(Char2 ch1:rights) {
					if(ch1!=null && ch1.left<=ch.right) {
						for(int i=ch1.left; i<=ch.right; i++) {
							for(int j=ch1.upper; j<=ch1.lower; j++)
								pageBitmap.points[i][j]=new Char2.Point(i,j,ch1);
						}
						
						for(int i=ch1.left; i<=ch.right; i++) {
							for(int j=ch1.upper;j<=ch1.lower;j++)
								if(pageBitmap.points[i][j]!=null &&
										pageBitmap.points[i][j].ch==ch)
									pageBitmap.points[i][j].ch=ch1;
						}
						
						for(int i=ch1.left; i<=ch.right; i++)
							for(int j=ch.upper; j<=ch.lower; j++)
								if(pageBitmap.points[i][j] != null && pageBitmap.points[i][j].ch!=null && pageBitmap.points[i][j].ch==ch)
									pageBitmap.points[i][j]=null;
							
						ch.right=ch1.left-1;
					}
				}
				
				ArrayList<Char2> lowers=ch.getLowerConnected(this,1);
				if(lowers.size()==0)
					continue;
				
				for(Char2 ch1:lowers) {
					if(ch1!=null && ch1.upper<=ch.lower) {
						for(int i=ch1.upper; i<=ch.lower; i++) {
							for(int j=ch1.left; j<=ch1.right; j++)
								pageBitmap.points[j][i]=new Char2.Point(j,i,ch1);
						}
						
						for(int i=ch1.upper; i<=ch.lower; i++) {
							for(int j=ch1.left;j<=ch1.right;j++)
								if(pageBitmap.points[j][i]!=null &&
										pageBitmap.points[j][i].ch==ch)
									pageBitmap.points[j][i].ch=ch1;
						}
						
						for(int i=ch1.upper; i<=ch.lower; i++)
							for(int j=ch.left; j<=ch.right; j++)
								if(pageBitmap.points[j][i]!=null && pageBitmap.points[j][i].ch!=null && pageBitmap.points[j][i].ch==ch)
									pageBitmap.points[j][i]=null;
						
						ch.lower=ch1.upper-1;
					}
				}
			}
	}
	
	boolean ignored() {
		return content.ignorePage.isIgnored(this);
	}
	
	class PageBitmap {
		Char2.Point[][] points;
		
		public PageBitmap(Page2 page) {
			points=new Char2.Point[page.right+1][page.lower+1];
			
			for (int i=0; i<chars.size();i++) {
				Char2 ch=chars.get(i);
				
				for (int x=ch.left; x<=ch.right; x++)
					for (int y=ch.upper; y<=ch.lower; y++) {
						Char2.Point point=new Char2.Point(x,y,ch);
						points[x][y]=point;
					}
			}
		}
	}
	
	class PageImg {
		public BufferedImage img;
		
		private int left;
		private int right;
		private int upper;
		private int lower;

		public PageImg(BufferedImage img) {
			this.img=img;
			left=img.getMinX();
			upper=img.getMinY();
			right=img.getWidth()-1;
			lower=img.getHeight()-1;
		}
		
		boolean in(int xPage, int yPage) {
			return (xPage-1>=left || yPage-1>=upper || xPage-1<=right || yPage-1<=lower);
		}
		
		boolean normal() {
			return left<=Page2.this.left-1 && right>=Page2.this.right-1 &&
						upper<=Page2.this.upper-1 && lower>=Page2.this.lower-1;
		}
		
		void setPageRGB(int xPage, int yPage, int rgb) {
			img.setRGB(xPage-1,yPage-1,rgb);
		}
		
		int getPageRGB(int xPage, int yPage) {
			return img.getRGB(xPage-1,yPage-1);
		}
	}
}
