import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HttpServer {
    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(Integer.parseInt(args[0]));
        System.out.println("server ouvert au port " + args[0]);
        String msgRecu;
        String url= "";
        while (true) {
            Socket socket = server.accept();

            OutputStream os = socket.getOutputStream();

            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            msgRecu = br.readLine();
            String[] msg = msgRecu.split(" ");
            if (msg[0].equals("GET")) {
                url=msg[1];
                System.out.println(msg[0] +" "+url);

            }

            Path path = Paths.get("web/" +url);
            File fichier = new File (path.toString());


            if (fichier.exists()) {
                System.out.println("trouvé, envoi...");
                byte[] envoi = Files.readAllBytes(fichier.toPath());
                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                os.write(("Content length: "+envoi.length+"\r\n").getBytes());
                os.write("Connection: keep-Alive\r\n".getBytes());

                os.write(("Content-Type: "+ Files.probeContentType(fichier.toPath())+ "\r\n").getBytes());
                os.write("\r\n".getBytes());
                os.flush();

                os.write(envoi);
                os.flush();

            } else  {
                System.out.println("fichier non trouvable");
                os.write("HTTP/1.1 404 Not Found\r\n".getBytes());
                os.write("Content-Type: text/html\r\n".getBytes());
                os.write("Connection: close\r\n".getBytes());
                os.write("\r\n".getBytes());
                os.write("404 Not Found".getBytes());
                os.flush();
            }
            br.close();
            os.close();
            socket.close();

        }



    }

}