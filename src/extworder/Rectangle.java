package extworder;

import java.util.ArrayList;
import java.util.Comparator;

import extworder.Page.Column;

public abstract class Rectangle {
	int left,upper,right,lower;
	int width, height;
	
	Rectangle() {
		resetRectangle();
	}
	
	Rectangle(int left,int upper,int right, int lower) {
		this.left=left;
		this.right=right;
		this.upper=upper;
		this.lower=lower;
		width=right-left;
		height=lower-upper;
	}
	
	protected void resetRectangle() {
		left=9999;
		upper=9999;
		right=0;
		lower=0;
		width=height=-1;
	}

	protected void updateRectangle(Rectangle r) {
		if (left>r.left) left=r.left;
		if (right<r.right) right=r.right;
		if (upper>r.upper) upper=r.upper;
		if (lower<r.lower) lower=r.lower;
		
		width=right-left;
		height=lower-upper;
	}
	
	protected void updateRectangle(int x,int y) {
		if (left>x) left=x;
		if (right<x) right=x;
		if (upper>y) upper=y;
		if (lower<y) lower=y;
		
		width=right-left;
		height=lower-upper;
	}
	
	protected boolean contains(Rectangle r) {
		return r.left>=left && r.right<=right && r.upper>=upper && r.lower<=lower;
	}
	
	protected float distance(Rectangle r) {
		if( upper>=r.upper && upper<=r.lower || 
			r.upper >= upper && r.upper<=lower ) {
			
			float d1=Math.abs(left-r.right);
			float d2=Math.abs(right-r.left);
			
			return Math.min(d1,d2);
		}
		
		if( left>=r.left && left<=r.right || 
			r.left >= left && r.right<=right ) {
			
			float d1=Math.abs(upper-r.lower);
			float d2=Math.abs(lower-r.upper);
			
			return Math.min(d1,d2);
		}	
		
		float d1=(float) Math.sqrt(Math.pow(upper-r.lower,2) + Math.pow(left-r.right,2));
		float d2=(float) Math.sqrt(Math.pow(upper-r.lower,2) + Math.pow(right-r.left,2));
		float d3=(float) Math.sqrt(Math.pow(r.upper-lower,2) + Math.pow(r.left-right,2));
		float d4=(float) Math.sqrt(Math.pow(r.upper-lower,2) + Math.pow(r.right-left,2));
		
		return Math.min(Math.min(d1,d2),Math.min(d3,d4));
	}
	
	protected int alignment(Rectangle parentRect, int adj) {
		int leftIndent=left-parentRect.left;
		int rightIndent=parentRect.right-right;
		
		if(Math.abs(leftIndent-rightIndent) < adj)
			if(leftIndent==0)
				return Common._CENTERALIGNED;
			else
				return Common._CENTERALIGNEDWIINDENT;
		else if(leftIndent==0)
			return Common._LEFTALIGNED;
		else if(rightIndent==0)
			return Common._RIGHTALIGNED;
		else
			return Common._NOALIGNED;
	}
	
	protected boolean rightAligned(Rectangle parent) {
		return right<=parent.right && 
				right>=parent.right-Math.round(parent.width*Common._ColumnWidthAdjustment);
	}
	
	protected boolean rightAligned(Rectangle parent, int rightAdj) {
		return right<=parent.right && right>=rightAdj;
	}
	
	protected boolean leftAligned(Rectangle parent) {
		return left>=parent.left && 
				left<=parent.left+Math.round(parent.width*Common._ColumnWidthAdjustment);
	}
	
	protected boolean leftAligned(Rectangle parent, int leftAdj) {
		return left<=parent.left && left<leftAdj;
	}
	
	protected boolean hIntersected(Rectangle r1) {
		return (left >= r1.left && left <= r1.right) ||
			   (r1.left >= left && r1.left <= right);
	}
	
	protected boolean vIntersected(Rectangle r1) {
		return (upper >= r1.upper && upper <= r1.lower) ||
			   (r1.upper >= upper && r1.upper <= lower);
	}
	
	protected boolean hContains(Rectangle r1) {
		return (left <= r1.left && right >= r1.right);
	}
	
	protected boolean vContains(Rectangle r1) {
		return (upper <= r1.upper && lower >= r1.lower) ;
	}
	
	boolean isFull(Content content, Column column) {
		//int width=right-left;
		
		if(column==null)
			return width >= content.lowContentWidth &&
				width <= content.highContentWidth;
			
		return width >= content.lowColumnWidth &&
				width <= content.highColumnWidth;
	}
	
	boolean isFull(Content content, Rectangle rect) {
		//int width=right-left;
		//int rWidth=rect.right-rect.left;
		
		return width >= rect.width * (1-Common._ColumnWidthAdjustment) &&
				width <= rect.width * (1+Common._ColumnWidthAdjustment);
	}
	
	/*boolean isTrivial1(Content content, Column column) {
		int blockWidth=right-left+1;
		
		if(column!=null)
			return blockWidth >= content.lowColumnWidth &&
				blockWidth <= content.highColumnWidth;
			
		return blockWidth >= content.lowContentWidth &&
				blockWidth <= content.highContentWidth;
	}*/
	
	protected <T extends Rectangle> ArrayList<T> getAllAbove(ArrayList<T> ts) {		
		ArrayList<T> cs=new ArrayList<T>();
		
		if (upper<1) return cs;
		
		for(T t:ts) {
			if(hIntersected(t) && upper>t.lower)
				cs.add(t);
		}
		
		return cs;
	} 

	protected <T extends Rectangle> ArrayList<T> getAllBelow(ArrayList<T> ts) {		
		ArrayList<T> cs=new ArrayList<T>();
		
		if (upper<1) return cs;
		
		for(T t:ts) {
			if(hIntersected(t) && lower<t.upper)
				cs.add(t);
		}
		
		return cs;
	}
	
	static class ComparePerimeter<T extends Rectangle> implements Comparator<T> {
		public int compare(T b1, T b2) {
			return Common.compareValue(
					b2.right-b2.left+b2.lower-b2.upper,
					b1.right-b1.left+b1.lower-b1.upper);
		}
	}
	
	class VStretch extends Stretch {
		VStretch(Char ch) {
			super(ch.upper,ch.lower);
		}
		
		VStretch(Stretch stretch) {
			super(stretch.start,stretch.end);
		}
	}
	
	class Stretch {
		int start,end;
		
		Stretch(int start, int end) {
			this.start=start;
			this.end=end;
		}

	    public boolean equals(Object object) {
	        if (object != null && object instanceof Stretch) {
				return start== ((Stretch) object).start && end==((Stretch) object).end;
			}
			return false;
	    }
	    
	    public boolean contains(Stretch stretch) {
	    	return start<=stretch.start && end>=stretch.end;
	    }
		
		boolean isIntersected(Stretch stretch) {
			return (start>=stretch.start && start<=stretch.end) ||
					(stretch.start>=start && stretch.start<=end);
		}
		
		Stretch intersection(Stretch stretch) {
			int s=Math.max(start,stretch.start);
			int e=Math.min(end,stretch.end);
			
			if(start<=end)
				return new Stretch(s,e);
			else
				return null;
		}
		
		int length() {
			return end-start+1;
		}
		
		Stretch add(Stretch stretch) {
			int s=Math.min(start,stretch.start);
			int e=Math.max(end,stretch.end);
			return new Stretch(s,e);
		}
		
		Stretch copy() {
			return new Stretch(start,end);
		}
	}
}
