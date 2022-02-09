package geskiw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import extworder.Block;
import extworder.Char;
import extworder.Common;
import extworder.Row;

public class CharString {
	ArrayList<Char> chars;
	private String string;
	HashMap<Integer,Char> charmap;
	
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
	
	public CharString(Block block,ArrayList<Pattern> abbrPatterns) {
		chars=new ArrayList<Char>();
		string="";
		
		ArrayList<Block> blocks=new ArrayList<Block>();
		blocks.add(block);
		build(blocks);
		markAbbreviation(abbrPatterns);
	}
	
	public void addChar(Char c) {
		chars.add(c);
	}
	
	public void addAllChars(CharString cs) {
		chars.addAll(cs.chars);
	}
	
	public void build(ArrayList<Block> blocks) {
		Char c=null;
		Char c0=null;
		boolean lastDash=false;
		
		for(Block block:blocks) {
			for(Row row:block.rows) {
				if(c!=null && !lastDash)
					chars.add(c.spaceChar(c.width));	// not the first row
				
				c0=row.chars.get(0);
				for(int i=0;i<row.chars.size();i++) {
					c=row.chars.get(i);
					lastDash=false;
					
					if(i==row.chars.size()-1)
						if(c.str.charAt(c.str.length()-1)=='-') {
							lastDash=true;
							if(c.str.length()>1)
								c=new Char(c,c.str.substring(0,c.str.length()-2),c.width);
							else continue;
						}
					
					if(c0!=c && ! Common.connectedChars(c0,c))
						chars.add(c0.spaceChar(c.left-c0.right-1));
					chars.add(c);
					c0=c;
				}
			}
		}
	}

	
	public String string() {
		if(string!="")
			return string;

		charmap=new HashMap<Integer,Char>();
		int i=0;

		for(Char c:chars) {
			string+=c.str;
			for(int j=0; j<c.str.length();j++) {
				charmap.put(i+j,c);
			}
			i+=c.str.length();
		}
		return string;
	}
	
	public String reString() {
		string="";
		return string();
	}
	
	public void markAbbreviation(ArrayList<Pattern> abbrPatterns) {
		for(int i=0;i<abbrPatterns.size();i++) {
			Matcher m=abbrPatterns.get(i).matcher(string());
			while(m.find()) {
				for(int j=m.start();j<m.end();j++) {
					Char c=charmap.get(j);

					if(c.str.contains(".")) {
						c.str=c.str.replace(".",Consts._AbbrDot);
					}
				}
			}
		}
	}
	
	public CharString unmarkAbbreviation() {
		for(Char c:chars)
			if(c.str.contains(Consts._AbbrDot))
				c.str=c.str.replace(Consts._AbbrDot,".");
		return this;
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
