package webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class WebServer {
	public WebServer() throws IOException, InterruptedException {
		int port = 80;
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		System.out.println("Server started at " + port);
		server.createContext("/", new RootHandler());
		server.createContext("/upload_file", new UploadeHandler());
		server.setExecutor(null);
		server.start();
		
		Thread.sleep(20000);
	}

	public class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
        	String response=
        		"<form action=\"/#upload_file\" method=\"post\">" + 
	        	  "<label for=\"upload\">Upload PDF File:</label>" +
	        	  "<input type=\"file\" name=\"pdf_file\" id=\"pdf_file\"><br><br>" +
	        	  "<input type=\"submit\" value=\"Submit\">"+
	        	"</form>";
        	
        	exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

	}
	
	public class UploadeHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange t) throws IOException {
            for(Entry<String, List<String>> header : t.getRequestHeaders().entrySet()) {
                System.out.println(header.getKey() + ": " + header.getValue().get(0));
            }
            DiskFileItemFactory d = new DiskFileItemFactory();      

            try {
                ServletFileUpload up = new ServletFileUpload(d);
                @SuppressWarnings("unchecked")
				List<FileItem> result = up.parseRequest(new RequestContext() {
                    @Override
                    public String getCharacterEncoding() {
                        return "UTF-8";
                    }

                    @Override
                    public int getContentLength() {
                        return 0; //tested to work with 0 as return
                    }

                    @Override
                    public String getContentType() {
                        return t.getRequestHeaders().getFirst("Content-type");
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return t.getRequestBody();
                    }

                });
                t.getResponseHeaders().add("Content-type", "text/plain");
                t.sendResponseHeaders(200, 0);
                OutputStream os = t.getResponseBody();               
                for(FileItem fi : result) {
                    os.write(fi.getName().getBytes());
                    os.write("\r\n".getBytes());
                    System.out.println("File-Item: " + fi.getFieldName() + " = " + fi.getName());
                }
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
	}
}
