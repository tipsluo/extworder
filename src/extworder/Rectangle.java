package extworder;

public abstract class Rectangle {
	int left=9999,top=9999,right=0,bottom=0;
	float width,height;

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
}
