package extworder;

import java.util.ArrayList;

import extworder.Block.BlockFormat;

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
	
	/*static Block verifyBodyBlock(Block block) {
		int maxGap=Math.round(Common._MaxInterBodyBlockGapRatio * block.format.charfont.height);

		Block virtualBlock=new Block();
				
		virtualBlock.updateRectangle(block);
		
		ArrayList<Block> ubs=block.traceAllAbove(block.page.blocks,maxGap);
		for(Block ub:ubs) {
			if(ub.type==Common._PageHeaderBlock || ub.type==Common._PageFooterBlock ||
					ub.column!=block.column)
				break;
			virtualBlock.updateRectangle(ub);
		}
		
		ArrayList<Block> lbs=block.traceAllBelow(block.page.blocks,maxGap);
		for(Block lb:lbs) {
			if(lb.type==Common._PageHeaderBlock || lb.type==Common._PageFooterBlock ||
					lb.column!=block.column)
				break;
			virtualBlock.updateRectangle(lb);
		}
		
		if( Math.abs(block.column.width-virtualBlock.width) > block.column.width * Common._ColumnWidthAdjustment)
			return null;
		
		int c=block.page.columns.indexOf(block.column);
		
		if( c == block.page.columns.size()-1)
			return virtualBlock;
		
		if(block.page.columns.get(c+1).blocks.size()==0)
			return virtualBlock;
		
		lbs=block.getAllBelow(block.column.blocks);
		if(lbs.size()>0)
			return virtualBlock;

		
		if(virtualBlock.lower < block.column.lower - maxGap)
			return null;
		
		return virtualBlock;
	}*/
}
