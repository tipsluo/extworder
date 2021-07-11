package extworder;

import java.util.ArrayList;

public abstract class Rectangle {
	int left,top,right,bottom;
	//float width,height;
	
	Rectangle() {
		left=9999;
		top=9999;
		right=0;
		bottom=0;
	}
	
	Rectangle(int left,int top,int right, int bottom) {
		this.left=left;
		this.right=right;
		this.top=top;
		this.bottom=bottom;
	}

	protected void updateRectangle(Rectangle r) {
		if (left>r.left) left=r.left;
		if (right<r.right) right=r.right;
		if (top>r.top) top=r.top;
		if (bottom<r.bottom) bottom=r.bottom;
	}
	
	protected boolean contains(Rectangle r) {
		return r.left>=left && r.right<=right && r.top>=top && r.bottom<=bottom;
	}
	
	protected float distance(Rectangle r) {
		if( top>=r.top && top<=r.bottom || 
			r.top >= top && r.top<=bottom ) {
			
			float d1=Math.abs(left-r.right);
			float d2=Math.abs(right-r.left);
			
			return Math.min(d1,d2);
		}
		
		if( left>=r.left && left<=r.right || 
			r.left >= left && r.right<=right ) {
			
			float d1=Math.abs(top-r.bottom);
			float d2=Math.abs(bottom-r.top);
			
			return Math.min(d1,d2);
		}	
		
		float d1=(float) Math.sqrt(Math.pow(top-r.bottom,2) + Math.pow(left-r.right,2));
		float d2=(float) Math.sqrt(Math.pow(top-r.bottom,2) + Math.pow(right-r.left,2));
		float d3=(float) Math.sqrt(Math.pow(r.top-bottom,2) + Math.pow(r.left-right,2));
		float d4=(float) Math.sqrt(Math.pow(r.top-bottom,2) + Math.pow(r.right-left,2));
		
		return Math.min(Math.min(d1,d2),Math.min(d3,d4));
	}
	
	protected boolean isHIntersected(Rectangle r1) {
		return (left >= r1.left && left <= r1.right) ||
			   (r1.left >= left && r1.left <= right);
	}
	
	protected boolean isVIntersected(Rectangle r1) {
		return (top >= r1.top && top <= r1.bottom) ||
			   (r1.top >= top && r1.top <= bottom);
	}
	
	protected boolean isHContaining(Rectangle r1) {
		return (left <= r1.left && right >= r1.right);
	}
	
	protected boolean isVContaining(Rectangle r1) {
		return (top <= r1.top && bottom >= r1.bottom) ;
	}
	
	protected <T extends Rectangle> ArrayList<T> getAllAbove(ArrayList<T> ts) {		
		ArrayList<T> cs=new ArrayList<T>();
		
		if (top<1) return cs;
		
		for(T t:ts) {
			if(isHIntersected(t) && top>t.bottom)
				cs.add(t);
		}
		
		return cs;
	} 

	protected <T extends Rectangle> ArrayList<T> getAllBelow(ArrayList<T> ts) {		
		ArrayList<T> cs=new ArrayList<T>();
		
		if (top<1) return cs;
		
		for(T t:ts) {
			if(isHIntersected(t) && bottom<t.top)
				cs.add(t);
		}
		
		return cs;
	}
	
	/*protected <T extends Rectangle> ArrayList<T> getRightAbove(ArrayList<T> ts) {		
		ArrayList<T> cs=new ArrayList<T>();
		
		//ArrayList<T> allAbove=getAllAbove(ts);
		
		int i=left;
		for(; i<=right; i++) {
			int highest=9999;
			int lowest=0000;
			T lowestT=null;
			
			for(int j=0;j<ts.size();j++) {
				T t=ts.get(j);
				
				if(t.bottom>=top)
					continue;
				
				if(t.top<highest)
					continue;
				
				if(t.left<=i && t.right>=i) {
					if(t.left<left || t.right>right) {
						highest=t.bottom+1;
						continue;
					}
					if(t.bottom>lowest) {
						lowest=t.bottom;
						lowestT=t;
					}
				}
			}
			if(lowestT!=null) {
				cs.add(lowestT);
				i=lowestT.right+1;
			} else
				i++;
		}
		return cs;
	}
	
	protected <T extends Rectangle> ArrayList<T> getRightBelow(ArrayList<T> ts) {		
		ArrayList<T> cs=new ArrayList<T>();
		
		//ArrayList<T> allBelow=getAllBelow(ts);
		
		int i=left;
		for(; i<=right; i++) {
			int highest=9999;
			int lowest=0000;
			T highestT=null;
			
			for(int j=0;j<ts.size();j++) {
				T t=ts.get(j);
				
				if(t.top<=bottom)
					continue;
				
				if(t.bottom>lowest)
					continue;
				
				if(t.left<=i && t.right>=i) {
					if(t.left<left || t.right>right) {
						lowest=t.top-1;
						continue;
					}
					if(t.top<highest) {
						highest=t.top;
						highestT=t;
					}
				}
			}
			if(highestT!=null) {
				cs.add(highestT);
				i=highestT.right+1;
			} else
				i++;
		}
		return cs;
	}*/
}
