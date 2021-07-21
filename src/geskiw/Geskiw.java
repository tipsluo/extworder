package geskiw;

import java.io.FileWriter;
import java.io.IOException;

import extworder.Common;
import extworder.Content;
import extworder.Extworder;

public class Geskiw {

	public Geskiw() {
		
	}

	public static void main(String args[]) throws IOException  {
		//test1("A model for estimating parameters of rotational landslide");
		//test1("Peace-Development and Peace Through");
		test1("Broader perspective on ecosystem");
		//test1("Taylor&Francis-Purification technology for renewable production of fuel from methan");
		//test1("ILL article-Impact of the KWL reading strategy");
		
		System.out.println("Geskiw Done.");
	}
	
	static void test1(String fn) throws IOException {
		Content content = new Content(fn);
		
		System.out.println(String.format("Title:\n%s\nAbstract:\n%s\n----------------------\n",
				content.title(),content.abstractStr));
	}
}
