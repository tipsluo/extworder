package extworder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public abstract class Rectangle {
	int left=9999,top=9999,right=0,bottom=0;
	float width,height;

	protected void updateRectangle(Rectangle r) {
		if (left>r.left) left=r.left;
		if (right<r.right) right=r.right;
		if (top>r.top) top=r.top;
		if (bottom<r.bottom) bottom=r.bottom;
	}
}
