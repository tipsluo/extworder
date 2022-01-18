package geskiw;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import extworder.Block;
import extworder.Char;
import extworder.Row;

public class CharString {
	ArrayList<Char> chars;
	private String string;
	
	public CharString() {
		chars=new ArrayList<Char>();
		string="";
	}
	
	public CharString(ArrayList<Block> blocks,ArrayList<Pattern> abbrPatterns) {
		chars=new ArrayList<Char>();
		string="";
		build(blocks);
		markAbbreviation(abbrPatterns);
	}
	
	public void addChar(Char c) {
		chars.add(c);
	}
	
	/*static public ArrayList<CharString> splitBlocks(ArrayList<Block> blocks) {
		ArrayList<CharString> ret=new ArrayList<CharString>();
		
		CharString cs=new CharString();
		boolean delimiter=false;
		boolean start=true;
		int y=blocks.get(0).rows.get(0).lower;
		
		for(Block block:blocks) {
			for(Row row:block.rows) {
				for(Char ch:row.chars) {
					if(start && 
							(ch.str.equals(" ") || 
									ch.str.equals("\n") ||
									ch.str.equals("\r"))) {
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
								ch.str.equals("\r") ||
								ch.str.equals("\"") ||
								ch.str.equals(")")) {
							
							if(ch.str.equals("\"") ||
									ch.str.equals(")")
								cs.chars.add(ch);
							
							ret.add(cs);
							cs=new CharString();
							start=true;
						}
						delimiter=false;
						continue;
					}
					
					if (row.upper>=y) {
						if(delimiter) {
							ret.add(cs);
							cs=new CharString();
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
	}*/
	
	public void build(ArrayList<Block> blocks) {
		ArrayList<CharString> ret=new ArrayList<CharString>();
		
		int y=blocks.get(0).rows.get(0).lower;
		String lastStr="";
		
		for(Block block:blocks) {
			for(Row row:block.rows) {
				if (row.upper>=y) {
					if(! lastStr.equals("-"))
						chars.add(new Char(" "));
					else
						chars.remove(chars.size()-1);
					y=row.lower;
				}
				
				for(Char c:row.chars)
					chars.add(c);
			}
		}
	}
	
	public String string() {
		if(string!="")
			return string;
		
		for(Char c:chars)
			string+=c.str;
		return string;
	}
	
	public String reString() {
		string="";
		return string();
	}
	
	public void markAbbreviation(ArrayList<Pattern> abbrPatterns) {
		boolean b=false;
		
		for(int i=0;i<abbrPatterns.size();i++) {
			Matcher m=abbrPatterns.get(i).matcher(string());
			while(m.find()) {
				b=true;
				for(int j=m.start();j<m.end();i++) {
					Char c=chars.get(j);

					if(c.str.equals("."))
						c.str=Consts._AbbrDot;
				}
			}
		}
		
		if(b)
			reString();
	}
	
	public void unmarkAbbreviation() {
		for(Char c:chars)
			if(c.str.equals(Consts._AbbrDot))
				c.str=".";
	}
	
	public ArrayList<CharString> splitSentences() {
		ArrayList<CharString> ret=new ArrayList<CharString>();
		
		CharString cs=new CharString();
		boolean delimiter=false;
		boolean start=true;
		
		for(Char ch:chars) {
			if(start && 
					(ch.str.equals(" ") || 
							ch.str.equals("\n") ||
							ch.str.equals("\r"))) {
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
						ch.str.equals("\r") ||
						ch.str.equals("\"") ||
						ch.str.equals(")")) {
					
					if(ch.str.equals("\"") || ch.str.equals(")"))
						cs.chars.add(ch);
					
					ret.add(cs);
					cs=new CharString();
					start=true;
				} else {
					ret.add(cs);
					cs=new CharString();
					cs.chars.add(ch);
					start=true;
				}
				
				delimiter=false;
				continue;
			}
			
			cs.chars.add(ch);
		}
		
		ret.add(cs);
		return ret;
	}
}
