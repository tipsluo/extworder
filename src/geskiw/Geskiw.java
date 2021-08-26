package geskiw;

import java.io.IOException;

public class Geskiw {
	
	public Geskiw(String fn) {
		
	}
	
	public static void main(String args[]) throws IOException  {
		Process process=new Process("Broader perspective on ecosystem");
		
		System.out.println(process.result);
		System.out.println("Geskiw Done.");
	}
}
