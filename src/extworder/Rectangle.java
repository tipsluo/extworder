package extworder;

import java.util.ArrayList;
import java.util.Comparator;

public class Rectangle implements Comparable<Rectangle> {
	int left,upper,right,lower;
	float width, height;
	long hashValue=0;
	
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
	
    Rectangle(Rectangle rect) {
        this.left=rect.left;
        this.right=rect.right;
        this.upper=rect.upper;
        this.lower=rect.lower;
        width=rect.width;
        height=rect.height;
    }
    
    boolean samePosition(Rectangle rect) {
        return left==rect.left && right==rect.right && upper==rect.upper && lower==rect.lower;
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
	
	protected int distance(Rectangle r) {
		boolean v=vIntersected(r);
		boolean h=hIntersected(r);
		
		if(v && h)
			return -1;
		
		if(v) {
			
			int d1=Math.abs(left-r.right);
			int d2=Math.abs(right-r.left);
			
			return Math.min(d1,d2);
		}
		
		if(h) {
			
			int d1=Math.abs(upper-r.lower);
			int d2=Math.abs(lower-r.upper);
			
			return Math.min(d1,d2);
		}	
		
		float d1=(float) Math.sqrt(Math.pow(upper-r.lower,2) + Math.pow(left-r.right,2));
		float d2=(float) Math.sqrt(Math.pow(upper-r.lower,2) + Math.pow(right-r.left,2));
		float d3=(float) Math.sqrt(Math.pow(r.upper-lower,2) + Math.pow(r.left-right,2));
		float d4=(float) Math.sqrt(Math.pow(r.upper-lower,2) + Math.pow(r.right-left,2));
		
		return Math.min(Math.round(Math.min(d1,d2)),Math.round(Math.min(d3,d4)));
	}
	
	/*protected int alignment(Rectangle parentRect, int centerAlignAdj, Content content) {
		int leftIndent=left-parentRect.left;
		int rightIndent=parentRect.right-right;
		
		if(Math.abs(leftIndent)<=Common._AlignAdjustment && Math.abs(rightIndent)<=Common._AlignAdjustment)
			return Common._FULLALIGNED;
		if(Math.abs(leftIndent)<=Common._AlignAdjustment)
			return Common._LEFTALIGNED;
		else if(Math.abs(leftIndent-content.firstRowIndent)<=Common._AlignAdjustment)
			return Common._INDENTLEFTALIGNED;
		else if(Math.abs(rightIndent)<=Common._AlignAdjustment)
			return Common._RIGHTALIGNED;
		else if(Math.abs(leftIndent-rightIndent) <= centerAlignAdj)
			return Common._CENTERALIGNED;
		else
			return Common._UNKNOWNALIGNED;
	}*/
	
	protected int alignment(Rectangle parentRect, int centerAlignAdj, Content2 content) {
		int leftIndent=left-parentRect.left;
		int rightIndent=parentRect.right-right;
		
		if(Math.abs(leftIndent)<=Common._AlignAdjustment && Math.abs(rightIndent)<=Common._AlignAdjustment)
			return Common._FULLALIGNED;
		if(Math.abs(leftIndent)<=Common._AlignAdjustment)
			return Common._LEFTALIGNED;
		else if(Math.abs(leftIndent-content.firstRowIndent)<=Common._AlignAdjustment)
			return Common._INDENTLEFTALIGNED;
		else if(Math.abs(rightIndent)<=Common._AlignAdjustment)
			return Common._RIGHTALIGNED;
		else if(Math.abs(leftIndent-rightIndent) <= centerAlignAdj)
			return Common._CENTERALIGNED;
		else
			return Common._UNKNOWNALIGNED;
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
	
	protected boolean centralAligned(Rectangle parent) {
		return centralAligned(parent,
				Math.round(parent.width*Common._ColumnWidthAdjustment));
	}
	
	protected boolean centralAligned(Rectangle parent, int adj) {
		int leftIndent=left-parent.left;
		int rightIndent=parent.right-right;
		
		return Math.abs(leftIndent-rightIndent) < adj;
			
	}
	
	protected boolean hIntersected(Rectangle r1) {
		return (left >= r1.left && left <= r1.right) ||
			   (r1.left >= left && r1.left <= right);
	}
	
	/*protected float hOverlap(Rectangle r1) {
		int l=Math.max(left,r1.left);
		int r=Math.min(right,r1.right);
		
		if(l>r)
			return -1;
		
		int minLen=Math.min(right-left,r1.right-r1.left);
		
		if(minLen==0)
			return -1;
		
		return (float)(r-l) / (float)minLen;
	}*/
	
	protected boolean vIntersected(Rectangle r1) {
		return (upper >= r1.upper && upper <= r1.lower) ||
			   (r1.upper >= upper && r1.upper <= lower);
	}
	
	/*protected float vOverlap(Rectangle r1) {
		int l=Math.max(upper,r1.upper);
		int r=Math.min(lower,r1.lower);
		
		if(l>r)
			return -1;
		
		int minLen=Math.min(lower-upper,r1.lower-r1.upper);
		
		if(minLen==0)
			return -1;
		
		return (float)(r-l) / (float)minLen;
	}*/
	
	public boolean hContains(Rectangle r1) {
		return (left <= r1.left && right >= r1.right);
	}
	
	public boolean vContains(Rectangle r1) {
		return (upper <= r1.upper && lower >= r1.lower) ;
	}
	
	protected boolean onLeftSide(Rectangle r) {
		return right<r.left;
	}
	
	protected boolean onRightSide(Rectangle r) {
		return left>r.right;
	}
	
	boolean isFull(Content2 content, Column2 column) {
		if(column==null)
			return width >= content.lowContentWidth &&
				width <= content.highContentWidth;
			
		return width >= content.lowColumnWidth &&
				width <= content.highColumnWidth;
	}
	
	boolean isFull(Content content, Rectangle rect) {
		return width >= rect.width * (1-Common._ColumnWidthAdjustment) &&
				width <= rect.width * (1+Common._ColumnWidthAdjustment);
	}
	
	boolean isLongEnough(Rectangle rect, Row2 next) {
		if(next.string().length()==0)
			return false;
			
		float diff=rect.right-right;
		
		int i=1;
		Char2 ch0=next.chars.get(0);
		for(;i<next.chars.size();i++) {
			Char2 ch=next.chars.get(i);
			if(ch.left-ch0.right > next.interval)
				break;
			
			ch0=ch;
		}
		
		if(i<next.chars.size())
			if(diff <= ch0.right-next.left)
				return true;
		
		return false;
	}
	
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
	
	@SuppressWarnings("unchecked")
	protected <T extends Rectangle> ArrayList<T> traceAllAbove(ArrayList<T> ts, int maxGap) {
		ArrayList<T> rects=new ArrayList<T>();
		rects.add((T)this);
		
		for(;;) {
			ArrayList<T> rects1=new ArrayList<T>();
			
			for(T rect:rects) {
				if(!rects1.contains(rect))
					rects1.add(rect);
				
				ArrayList<T> als=rect.getAllAbove(ts);
				
				for(T al:als)
					if(rect.distance(al)<=maxGap)
						if(!rects1.contains(al))
							rects1.add(al);
			}
			
			if(rects1.size()==rects.size()) {
				rects=rects1;
				break;
			} else {
				rects=rects1;
			}
		}
		
		return rects;
	}
	
	@SuppressWarnings("unchecked")
	protected <T extends Rectangle> ArrayList<T> traceAllBelow(ArrayList<T> ts, int maxGap) {
		ArrayList<T> rects=new ArrayList<T>();
		rects.add((T)this);
		
		for(;;) {
			ArrayList<T> rects1=new ArrayList<T>();
			
			for(T rect:rects) {
				if(!rects1.contains(rect))
					rects1.add(rect);
				
				ArrayList<T> bls=rect.getAllBelow(ts);
				
				for(T bl:bls)
					if(rect.distance(bl)<=maxGap)
						if(!rects1.contains(bl))
							rects1.add(bl);
			}
			
			if(rects1.size()==rects.size()) {
				rects=rects1;
				break;
			} else {
				rects=rects1;
			}
		}
		
		return rects;
	}
	
	static class ComparePerimeter<T extends Rectangle> implements Comparator<T> {
		public int compare(T b1, T b2) {
			return Common.compareValue(
					b2.right-b2.left+b2.lower-b2.upper,
					b1.right-b1.left+b1.lower-b1.upper);
		}
	}
    
    /*@Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        
        Rectangle other = (Rectangle) obj;

        return compareTo(other)==0;
    }*/

    @Override
    public int compareTo(Rectangle rect) {
    	long h1=hashValue();
    	long h2=rect.hashValue();
    	
        if(h1>h2)
        	return 1;
        else if(h1<h2)
        	return -1;
        else
        	return 0;
    }
    
    protected long hashValue() {
    	if(hashValue==0)
    		hashValue=(((long)left)<<48) + (((long)upper)<<32) + (((long)right)<<16) + (long)(lower);
    	
    	return hashValue;
    }
    
   /* @Override 
    public int hashCode() {
        return (((left * Math.round(width))) << 16 ) + upper * Math.round(height);
    }*/
}
