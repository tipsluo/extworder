package geskiw;

import java.util.ArrayList;

import extworder.Block;
import extworder.Char;
import extworder.Row;

public class CharSentence {
	ArrayList<Char> chars;
	
	public CharSentence() {
		chars=new ArrayList<Char>();
	}
	
	public void addChar(Char c) {
		chars.add(c);
	}
	
	static public ArrayList<CharSentence> splitBlocks(ArrayList<Block> blocks) {
		ArrayList<CharSentence> ret=new ArrayList<CharSentence>();
		
		CharSentence cs=new CharSentence();
		boolean delimiter=false;
		boolean start=true;
		int y=blocks.get(0).rows.get(0).lower;
		
		for(Block block:blocks) {
			for(Row row:block.rows) {
				for(Char ch:row.chars) {
					if(start && 
							(ch.str.equals(" ") || 
									ch.str.equals("\n") ||
									ch.str.equals("\r")) {
						continue;
					}
					
					start=false;
					
					if(ch.str.equals(".") || ch.str.equals("!") || ch.str.equals("?")) {
						cs.chars.add(ch);
						delimiter=true;
						continue;
					}
					
					if(delimiter) {
						if(ch.str.equals(" ") || 
								ch.str.equals("\n") || 
								ch.str.equals("\r")) ||
								ch.str.equals("\"") ||
								ch.str.equals(")") {
							
							if(ch.str.equals("\"") ||
									ch.str.equals(")")
								cs.chars.add(ch);
							
							ret.add(cs);
							cs=new CharSentence();
							start=true;
						}
						delimiter=false;
						continue;
					}
					
					if (row.upper>=y) {
						if(delimiter) {
							ret.add(cs);
							cs=new CharSentence();
							start=true;
							cs.chars.add(ch);
						}
						y=row.lower;
					}
				}
			}
		}
		ret.add(cs);
		return ret;
	}
}
