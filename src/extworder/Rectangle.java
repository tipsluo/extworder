package extworder;

import java.util.ArrayList;

public abstract class Rectangle {
	int left,upper,right,lower;
	
	Rectangle() {
		left=9999;
		upper=9999;
		right=0;
		lower=0;
	}
	
	Rectangle(int left,int upper,int right, int lower) {
		this.left=left;
		this.right=right;
		this.upper=upper;
		this.lower=lower;
	}

	protected void updateRectangle(Rectangle r) {
		if (left>r.left) left=r.left;
		if (right<r.right) right=r.right;
		if (upper>r.upper) upper=r.upper;
		if (lower<r.lower) lower=r.lower;
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
	
	protected boolean isHIntersected(Rectangle r1) {
		return (left >= r1.left && left <= r1.right) ||
			   (r1.left >= left && r1.left <= right);
	}
	
	protected boolean isVIntersected(Rectangle r1) {
		return (upper >= r1.upper && upper <= r1.lower) ||
			   (r1.upper >= upper && r1.upper <= lower);
	}
	
	protected boolean isHContaining(Rectangle r1) {
		return (left <= r1.left && right >= r1.right);
	}
	
	protected boolean isVContaining(Rectangle r1) {
		return (upper <= r1.upper && lower >= r1.lower) ;
	}
	
	protected <T extends Rectangle> ArrayList<T> getAllAbove(ArrayList<T> ts) {		
		ArrayList<T> cs=new ArrayList<T>();
		
		if (upper<1) return cs;
		
		for(T t:ts) {
			if(isHIntersected(t) && upper>t.lower)
				cs.add(t);
		}
		
		return cs;
	} 

	protected <T extends Rectangle> ArrayList<T> getAllBelow(ArrayList<T> ts) {		
		ArrayList<T> cs=new ArrayList<T>();
		
		if (upper<1) return cs;
		
		for(T t:ts) {
			if(isHIntersected(t) && lower<t.upper)
				cs.add(t);
		}
		
		return cs;
	}
}
