import java.net.*;

public class Ecouteur extends Thread {
    private ParamSite ps;

    public Ecouteur(ParamSite ps) {
        this.ps = ps;
    }

    public void run() {
        try {
            ServerSocket ss = new ServerSocket(ps.getPort());
            System.out.println("Serveur ouvert sur le port : " + ps.getPort());
            while (true) {
                Socket client = ss.accept();
                new Handler(client, ps).start();
            }
        } catch (Exception e) {
            System.out.println("Erreur port " + ps.getPort() + " : " + e.getMessage());
        }
    }
}