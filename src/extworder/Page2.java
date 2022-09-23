package extworder;

import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.TextPosition;

import extworder.Block2.BlockFilter;
import extworder.Common.SortedList;

public class Page2 extends Rectangle {
	public Content2 content;
	public int id;
    public ArrayList<Char2> chars;
    public ArrayList<Block2> blocks;
    public ArrayList<Row2> rows;
    public SortedList<Block2> blockCandidates;
    public SortedList<Row2> rowCandidates;
    PageBitmap pageBitmap;
    public PageImg pageImg;
	private int xOffset;
	private int yOffset;
	private List<BlockSet> blocksetCandidates;
	
	final static int _MaxCharInRowInterval=10;
	final static float _MaxBlockIntervalRatio=2.0f;
    
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
	}
	
	public void analyze() {
		generateAllRowCandidates();
		generateAllBlockCandidates();
		generateBlocksetCandidates();
		
		long lowest=999999999;
		BlockSet blockset=null;
		for(BlockSet bs: blocksetCandidates) 
			if(bs.value<lowest) {
				lowest=bs.value;
				blockset=bs;
			}
		
		blocks=blockset.list;
	}
	
	private void generateAllRowCandidates() {
		rowCandidates=new SortedList<Row2>(); 
		for(Char2 ch:chars) {
			for(int i=0; i<_MaxCharInRowInterval; i++) {
				if(ch.rowCandidates.list.size()>0) {
					boolean done=false;
					for(Row2 row:ch.rowCandidates.list) 
						if(row.interval==i) {
							done=true;
							break;
						}
					if(done)
						continue;
				}
				
				new Row2(ch,this,i);
			}
		}
	}
	
	private void generateAllBlockCandidates() {
		blockCandidates=new SortedList<Block2>();
		
		for(int i=0; i<rowCandidates.list.size(); i++) {
			Row2 row=rowCandidates.list.get(i);
			
			blockCandidates.list.add(new Block2(row));
		}
		
		blockCandidates=mateBlocks(blockCandidates);
	}

	private SortedList<Block2> mateBlocks(SortedList<Block2> input) {
		SortedList<Block2> output=new SortedList<Block2>();
		
		output.list.addAll(input.list);
		
		for(int i=0; i<input.list.size(); i++) {
			Block2 block1=input.list.get(i);
			
			for(int j=i+1; j<input.list.size(); j++) {
				Block2 block2=input.list.get(j);
				
				if(block1.format.charfont.height!=block2.format.charfont.height || !block1.hIntersected(block2))
					continue;
				
				int dis=block1.distance(block2);
				float maxInterval=Math.max(block1.format.charfont.height, block2.format.charfont.height) * _MaxBlockIntervalRatio;
				
				if(dis==-1) {
					if(block1.rows.size()>1 && block2.rows.size()>1 && block1.interval!=block2.interval)
						continue;
				}
					
				if(dis>maxInterval || (block1.rows.size()>1 && dis!=block1.interval) || (block2.rows.size()>1 && dis!=block2.interval))
						continue;
				
				List<Row2> newRows=Block2.checkRows(block1,block2);
				if(newRows.size()>0)
					output.addSortUniq(new Block2(dis,block1,block2));
			}
		}
		
		if(input.list.size()==output.list.size())
			return output;
		
		return mateBlocks(output);
	}
	
	private void generateBlocksetCandidates() {
		blocksetCandidates=new ArrayList<BlockSet>();

		Char2 seed=chars.get(0);
		for(int i=0; i<seed.blockCandidates.list.size(); i++) {
			BlockSet blockset=new BlockSet(this);
			
			blockset.list.add(seed.blockCandidates.list.get(i));
			buildBlockset(blockset,1);
			
			blockset.register();
		}
	}
	
	private void buildBlockset(BlockSet blockset, int charIndex) {
		if(charIndex>chars.size())
			return;
		
		Char2 ch=chars.get(charIndex);
		
		for(int i=0; i<ch.blockCandidates.list.size(); i++) {
			Block2 b=ch.blockCandidates.list.get(i);
			if(blockset.search(b)<0)
				blockset.addSortUniq(b);
			buildBlockset(blockset,charIndex+1);
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
	
	public void print(FileWriter fw) throws IOException {
		fw.write("==============================\n");

		
		fw.write(String.format("Page %d\n Left %d Right %d Top %d Bottom %d\n",
				id,left,right,upper,lower));
		
		if(pageImg!=null)
			fw.write(String.format("BufferImage Width %d Height %d\n",
					pageImg.img.getWidth(),pageImg.img.getHeight()));
		
		fw.write("\n\nColumn meta:\n");
		/*for(Column column:columns) {
			column.printMeta(fw);
		}*/
		
		fw.write("\n\nBlocks:\n----------------------\n");
		for(Block2 block:blocks) {
			block.print(fw);
		}
	}
	
	String string(BlockFilter ...blockFilters) {
		String str="";
		
		for(Block2 block:blocks) {
			boolean unmatched=false;
			
			for(BlockFilter filter : blockFilters)
				if(! filter.filter(block)) {
					unmatched=true;
					break;
				}
			
			if(unmatched)
				continue;
		
			str+=block.string()+"\n";
		}
		
		return str;
	}
	
	class BlockSet extends SortedList<Block2>{
		long value;
		Page2 page;
		
		BlockSet(Page2 page) {
			super();
			value=0;
			this.page=page;
		}
		
		long getValue() {
			long v=0;
			for(Block2 block:list)
				v+=block.value;
			return v;
		}
		
		void register() {
			getValue();
			page.blocksetCandidates.add(this);
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
