///A Simple Web Server (WebServer.java)

package http.server;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Example program from Chapter 1 Programming Spiders, Bots and Aggregators in
 * Java Copyright 2001 by Jeff Heaton
 * 
 * WebServer is a very simple web-server. Any request is responded with a very
 * simple web-page.
 * 
 * @author Jeff Heaton
 * @version 1.0
 */
public class WebServer {

  PrintWriter out;
  Socket remote;
  HashMap<String, String> headers;
  HashMap<String, String> contentType = new HashMap<>();
  HashMap<String, String> state = new HashMap<>();
  BufferedReader in;
  /**
   * WebServer constructor.
   */
  protected void start() {
    ServerSocket s;

    System.out.println("Webserver starting up on port 3000");
    System.out.println("(press ctrl-c to exit)");
    try {
      // create the main server socket
      s = new ServerSocket(3000);
    } catch (Exception e) {
      System.out.println("Error: " + e);
      return;
    }

    System.out.println("Waiting for connection");
    headers = new HashMap<>();
    try {
      readContentType();
      readState();
    }catch (Exception e){}

    for (; ; ) {
      try {
        // wait for a connection
        remote = s.accept();
        // remote is now the connected socket
        System.out.println("Connection, sending data.");
        in = new BufferedReader(new InputStreamReader(remote.getInputStream()));
        out = new PrintWriter(remote.getOutputStream());


        // read the data sent. We basically ignore it,
        // stop reading once a blank line is hit. This
        // blank line signals the end of the client HTTP
        // headers.

        String request = "";
        String line = in.readLine();
        while (line != null && !line.equals("")) {
          String[] strings = line.split(":");
          if(strings.length >= 2) {
            headers.put(strings[0], strings[1]);
          } else {
            request = line;
          }
          line = in.readLine();
        }

        byte[] body = new byte[0];
        if(headers.containsKey("Content-Length")) {
          int cL = Integer.valueOf(headers.get("Content-Length").split(" ")[1]);
          char[] buffer = new char[cL];

          in.read(buffer, 0, cL);

          Charset cs = Charset.forName("UTF-8");
          CharBuffer cb = CharBuffer.allocate(buffer.length);
          cb.put(buffer);
          cb.flip();
          ByteBuffer bb = cs.encode(cb);
          body = bb.array();
        }

        System.out.println("hashmap");
        for(String ss : headers.keySet()) {
          System.out.println(ss+" " + headers.get(ss));
        }
        System.out.println("request : " + request);

        String[] requests = request.split(" ");

        switch (requests[0]){
          case "GET":
            get(requests[1]);
            break;
          case "HEAD":
            head(requests[1]);
            break;
          case "PUT":
            put(requests[1], body);
            break;
          case "POST":
            post(requests[1], body);
            break;
          case "DELETE":
            delete(requests[1]);
            break;
          default:
        }

      } catch (Exception e) {
        System.out.println(e);
      }
    }
  }

  /**
   * Read the file which contains a table of correspondence of type and MIME.
   * Then we stock them in a hashMap.
   * @throws IOException
   */
  public void readContentType() throws IOException {
    File file = new File("src/http/server/type.txt");
    if (file.exists()) {
      InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
      BufferedReader br = new BufferedReader(reader);
      String rd = br.readLine();
      while(rd != null) {
        String[] s = rd.split("=");
        contentType.put(s[0].split("\"")[1], s[1].split("\"")[1]);
        rd = br.readLine();
      }
    }
  }

  /**
   * Read the file which contains a status code of correspondence of statusCode and statusMessage.
   * Then we stock them in a hashMap.
   * @throws IOException
   */
  public void readState() throws IOException {
    File file = new File("src/http/server/state.txt");
    if (file.exists()) {
      InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
      BufferedReader br = new BufferedReader(reader);
      String rd = br.readLine();
      while(rd != null) {
        String[] s = rd.split(":");
        state.put(s[0], s[1]);
        rd = br.readLine();
      }
    }
  }

  /**
   * Read a file by using the method read.
   * So we don't need to separate the case of a textual file or the other files.
   * @param filepath The path of the file. In our application, the file we want to read is stocked at ./resource
   * @throws IOException
   */
  public void readFile(String filepath) throws IOException {

    BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(filepath));
    byte[] buffer = new byte[256];
    int nbRead;
    while((nbRead = fileIn.read(buffer)) != -1) {
      remote.getOutputStream().write(buffer, 0, nbRead);
    }
    fileIn.close();
    remote.getOutputStream().flush();
  }

  public void readImg(String filepath) throws IOException {

    BufferedImage img = ImageIO.read(new File(filepath));
    ByteArrayOutputStream pngBaos = new ByteArrayOutputStream();
    ImageIO.write(img,"jpg", pngBaos);
    pngBaos.flush();
    byte[] imgByte = pngBaos.toByteArray();
    pngBaos.close();
    remote.getOutputStream().write(imgByte);
  }

  /**
   * Output the Header of the response by using the content-type and status code given
   * @param type the content-type which we want to response to the client
   * @param stat the status code of the response
   */
  public void responseHeadHandle(String type, int stat) {
    out.println("HTTP/1.0 " + stat + " " + state.get(Integer.toString(stat)));
    out.println("Content-Type: " + type);
    out.println("Server: BOT");
    out.println("");
    out.flush();
  }

  /**
   * Handle the dynamic resource by using our program.
   * Give two numbers to add and return their sum.
   * @param data1 one number to add(String)
   * @param data2 another number to add(String)
   * @return the sum of the two number(String)
   * @throws IOException
   */
  public String handleDynamic(String data1, String data2) throws IOException {
    Process process = Runtime.getRuntime().exec("java -classpath classes programme.AddNumber "+data1+" "+data2);

    byte[] results = new byte[255];
    int i = 0;
    int c = 0;
    while((c = process.getInputStream().read()) != -1){
      results[i] = (byte) c;
      ++i;
    }

    return new String(results);
  }

  /**
   * Implementation of the methode get.
   * The user give the URL path and the we search the file correspond in the server.
   * Then we read the file and response to the client by the file we read.
   * The difference of the url and filePath in our system is we stock the file in a repository ./resource
   * But the client doesn't need to because it will be too complicate.
   * @param path The url path which the client entered. It does not contain the hostname and the port of the server
   * @throws IOException
   */
  public void get(String path) throws IOException {
      if(path.contains("add?")) {

        String query = path.split("\\?")[1];
        String[] data = query.split("&");
        String data1 = data[0].split("=")[1];
        String data2 = data[1].split("=")[1];

        try {
          String result = handleDynamic(data1, data2);
          responseHeadHandle("text/html", 200);
          out.println("<h1>"+data1+" + "+data2+" = "+result + "</h1>");
          out.flush();

        } catch (Exception e) {
          e.printStackTrace();
          responseHeadHandle("text/html",500);
        }

        remote.close();
      } else {
        if (path.equals("/") || path.equals("/index.html")) {
          responseHeadHandle("text/html", 200);
          readFile("resource/index.html");
          remote.close();
        } else {
          File file = new File("resource" + path);
          if(file.exists()) {
            String type = path.split("\\.")[1];
            String pathLocation = "resource" + path;
            responseHeadHandle(contentType.get("." + type), 200);
            readFile(pathLocation);
            remote.close();
          } else {
            responseHeadHandle("text/html", 404);
            out.println("<h1>The file doesn't exist ! </h1>");
            out.flush();
            remote.close();
          }
        }
      }

  }

  /**
   * Implementation of the methode head.
   * The user give the URL path and the we search the file correspond in the server.
   * Then we read the file and response to the client with just the head but not the content of the file.
   * The difference of the url and filePath in our system is we stock the file in a repository ./resource
   * But the client doesn't need to because it will be too complicate.
   * @param path The url path which the client entered. It does not contain the hostname and the port of the server
   * @throws IOException
   */
  public void head(String path) throws IOException {
    File file = new File("resource" + path);
    if (file.exists()) {
      if (path.equals("/")) {
        responseHeadHandle("text/html", 200);
      } else {
        String type = path.split("\\.")[1];
        responseHeadHandle(contentType.get("." + type), 200);
      }
      remote.close();
    } else {
      responseHeadHandle("text/html", 404);
      remote.close();
    }
  }

  /**
   * The implementation of the methode put
   * The client update or create a file to the server by using the path given by the client.
   * The file on the server will be replaced totally by the file the user send.
   * @param path The url path which the client entered. It does not contain the hostname and the port of the server
   * @param body The content of the file which we want to put on the server.
   * @throws IOException
   */
  public void put(String path, byte[] body) throws IOException {
    String localLocation = "."+path;
    File f = new File(localLocation);
    try {
      if(!f.exists()){
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(body);
        fos.close();
        responseHeadHandle("text/html",201);
        out.println("<h1>You have created a new file</h1>");
        out.flush();
      }else if(!f.canWrite()){
        responseHeadHandle("text/html",403);
        out.println("<h1>you don't have permission</h1>");
        out.flush();
      } else {
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(body);
        fos.close();
        responseHeadHandle("text/html",200);
        out.println("<h1>you have changed the file</h1>");
        out.flush();
      }
    }catch (Exception e){
      e.printStackTrace();
      responseHeadHandle("text/html",500);
      out.println("<h1>Error</h1>");
      out.flush();
    }
    remote.close();
  }

  /**
   * The implementation of the methode delete
   * The client delete a file on the server by using the path given by the client.
   * The Location of the file is strictly defined by the client.
   * So the path Url and the path of the server is exactly the same.
   * @param path The url path which the client entered. It does not contain the hostname and the port of the server
   * @throws IOException
   */
  public void delete(String path) throws IOException {
    String localLocation = "."+path;
    File f = new File(localLocation);
    try {
      if(!f.exists()){
        responseHeadHandle("text/html",404);
        out.println("<h1>the file doesn't exist</h1>");
        out.flush();
      }else if(!f.canWrite()){
        responseHeadHandle("text/html",403);
        out.println("<h1>you don't have permission</h1>");
        out.flush();
      } else {
        if(f.delete()){
          responseHeadHandle("text/html",200);
          out.println("<h1>The file has been deleted successfully ! </h1>");
          out.flush();
        } else {
          responseHeadHandle("text/html",404);
        }
      }
    }catch (Exception e){
      e.printStackTrace();
      responseHeadHandle("text/html",500);
    }
    remote.close();
  }

  /*
  public void post(String path, byte[] body) throws IOException {
    String localLocation = "resource"+path;
    File f = new File(localLocation);
    try {
      int i = 1;
      while (f.exists()) {
        localLocation = "resource"+path.split("\\.")[0]+Integer.toString(i)+"."+path.split("\\.")[1];
        f = new File(localLocation);
        ++i;
      }

      f.createNewFile();
      FileOutputStream fos = new FileOutputStream(f);
      fos.write(body);
      fos.close();
      responseHeadHandle("text/html",201);
      readFile(localLocation);

    }catch (Exception e){
      e.printStackTrace();
      responseHeadHandle("text/html",500);
    }
    remote.close();
  }
  */


  /**
   * The implementation of the methode post
   * The client update or create a file to the server by using the path given by the client.
   * The file which the server send will be append to the original file if it exists..
   * @param path The url path which the client entered. It does not contain the hostname and the port of the server
   * @param body The content of the file which we want to post on the server.
   * @throws IOException
   */
  public void post(String path, byte[] body) throws IOException {
    String localLocation = "resource"+path;
    File f = new File(localLocation);
    try {
      if(f.exists()) {
        FileOutputStream fos = new FileOutputStream(f,true);
        fos.write(body);
        fos.close();
        responseHeadHandle("text/html",200);
        readFile(localLocation);
      } else {
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(body);
        fos.close();
        responseHeadHandle("text/html",201);
        readFile(localLocation);
      }
    }catch (Exception e){
      e.printStackTrace();
      responseHeadHandle("text/html",500);
    }
    remote.close();
  }

  /**
   * Start the application.
   * 
   * @param args
   *            Command line parameters are not used.
   */
  public static void main(String args[]) {
    WebServer ws = new WebServer();
    ws.start();
  }
}
