package geskiw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import extworder.Common.StatGroup;

//public class Word implements Comparable<Word>{
/*	public ArrayList<String> forms;
	
	public Word(String str) {
		forms=new ArrayList<String>();
		
		String irreSigular=Process.irregulars.get(str);
		
		if(irreSigular != null) {
			if(str.equals(irreSigular)) {
				forms.add(irreSigular);
				forms.add(str);
			} else
				forms.add(str);
			
			return;
		}
		
		String orig=origin(str);
		
		if(str.equals(orig))
			forms.add(str);
		else {
			forms.add(orig);
			forms.add(str);
		}
		
		//sort();
	}
	
	public boolean combine(Word word) {
		boolean ret=false;
		
		for(String str1:word.forms) {
			boolean b=false;
		
			for(String str2:forms)
				if(str1.equals(str2)) {
					b=true;
					break;
				}
			
			if(!b) {
				forms.add(str1);
				ret=true;
			}
		}
		
		return ret;
	}

	public String origin(String str) {
		int l=str.length();
		
		if(l<4)
			return str;
		
		String s=str.substring(l-3);
		
		if(s.equals("ies"))
			return str.substring(0,l-3)+"y";
		else if(s.equals("aes") || s.equals("oes") || s.equals("ues") || s.equals("ses") || s.equals("xes"))
			return str.substring(0,l-2);
		else if(s.equals("'es"))
			return str.substring(0,l-2);
		else if(s.equals("ves"))
			return str.substring(0,l-2)+"f";
		
		
		s=str.substring(l-2);
		
		if(s.equals("ss"))
			return str;
		
		s=str.substring(l-1);
		
		if(s.equals("s"))
			return str.substring(0,l-1);

		return str;
	}

	@Override
	public int compareTo(Word word) {
		return forms.get(0).compareTo(word.forms.get(0));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        
        Word other = (Word) obj;

        return hashCode()==other.hashCode();
	}
	
	protected boolean same(Word word) {
		return forms.get(0).equals(word.forms.get(0));
	}
	
	@Override
	public int hashCode() {
		//return 0;
		return forms.get(0).hashCode();
		//return wordCode();
	}
	
	public String string() {
		String ret="";
		for(String s:forms)
			ret+=s+" ";
		return ret;
	}
	
	static class Words {
		ArrayList<Word> list;
		
		public Words(ArrayList<String> source) {
			list=new ArrayList<Word>();
			
			for(String s:source)
				list.add(new Word(s));
			
			uniq();
			
			Collections.sort(list);
		}
		
		public void uniq() {
			int i=0;
			for(;i<list.size();i++) {
				Word word1=list.get(i);
				for(int j=i+1;j<list.size();j++) {
					Word word2=list.get(j);
					if(word1.same(word2)) {
						word1.combine(word2);
						list.remove(j);
						j--;
					}
				}
			}
		}
	}

	static class WordStatGroup extends StatGroup<Word> {
		public WordStatGroup() {
			super();
		}
		
		public WordStatGroup(ArrayList<String> source) {
			super();
			
			for(String str:source) {
				add(new Word(str));
			}
		}
		
		@Override
		public void add(Word word) {
			for(Map.Entry<Word,Integer> entry:records.entrySet()) {
				Word w=entry.getKey();
				Integer i=entry.getValue();
				
				if(word.same(w)) {
					w.combine(word);
					records.put(w,i+1);
					return;
				}
			}

			records.put(word,1);
		}
	}*/
//}
