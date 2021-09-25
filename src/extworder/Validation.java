package extworder;

public class Validation {
	public Validation() {
		// TODO Auto-generated constructor stub
	}

	static boolean verifyRow(Row row) {
		if(row.chars.size()<2)
			return true;
		for(Char ch1:row.chars)
			for(Char ch2:row.chars) {
				if(ch1==ch2 || ch1.isVIntersected(ch2))
					continue;
				if(row.charfont.height!=ch1.height || row.charfont.height != ch2.height)
					continue;
				if(ch1.isHIntersected(ch2))
					return false;
			}
			
		return true;
	}
}
