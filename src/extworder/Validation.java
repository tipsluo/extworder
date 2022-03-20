package extworder;

import java.util.ArrayList;

import extworder.Block.BlockFormat;
import extworder.Block.SubtitleBlockFilter;

public class Validation {
	public Validation() {
		// TODO Auto-generated constructor stub
	}

	static boolean verifyRow(Row row) {
		if(row.chars.size()<2)
			return true;
		for(Char ch1:row.chars)
			for(Char ch2:row.chars) {
				if(ch1==ch2 || ch1.vIntersected(ch2))
					continue;
				if(row.charfont.height!=ch1.height || row.charfont.height != ch2.height)
					continue;
				if(ch1.hIntersected(ch2))
					return false;
			}
			
		return true;
	}
	
	static boolean completeParagraphBlock(Block block) {
		int d=block.rows.get(0).left-block.left;
		if(d>0 && d < block.format.charfont.height * Common._FirstLineIndentRatio)
			return true;
		return false;
	}
}
