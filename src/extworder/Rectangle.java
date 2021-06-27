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
}
