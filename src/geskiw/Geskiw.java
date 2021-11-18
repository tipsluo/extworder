package geskiw;

import java.io.IOException;

import extworder.Content;
import geskiw.Process.IgnorePage;
import webserver.WebServer;

public class Geskiw {
	
	public Geskiw(String fn) throws IOException, InterruptedException {
		
	}
	
	public static void main(String args[]) throws IOException, InterruptedException  {
		//Process process=new Process("Wiley-Early life stress and HPA axis");
		Process process=new Process("A model for estimating parameters of rotational landslide");
		new WebServer();
		System.out.println("Geskiw Done.");
	}
}
