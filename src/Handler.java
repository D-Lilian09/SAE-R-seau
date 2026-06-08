import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

public class Handler extends Thread {
    private Socket client;
    private ParamSite ps;
    private String home = System.getProperty("user.home");

    public Handler(Socket client, ParamSite ps) {
        this.client = client;
        this.ps = ps;
    }

    public void run() {
        try {
            traiterRequete();
        } catch (Exception e) {
            System.out.println("Erreur client : " + e.getMessage());
        }
    }

    private void traiterRequete() throws Exception {
        BufferedReader bf = new BufferedReader(new InputStreamReader(client.getInputStream()));

        String premiereLigne = bf.readLine();
        if (premiereLigne == null) { client.close(); return; }

        String fichier = premiereLigne.split(" ")[1];
        if (fichier.equals("/status")) {
            envoyerStatus();
            client.close();
            return;
        }

        if (fichier.equals("/")) fichier = "/" + ps.getDefaultIndex();

        String pasMatch = null;
        String ligne = bf.readLine();
        while (ligne != null && !ligne.equals("")) {
            if (ligne.startsWith("If-None-Match:")) pasMatch = ligne.split(": ")[1].trim();
            ligne = bf.readLine();
        }

        String fichierLocal = home + ps.getDocumentRoot() + File.separator + fichier.substring(1);
        File f = new File(fichierLocal);

        if (f.isDirectory()) envoyerListeFichiers(f, fichier);
        else if (f.exists()) envoyerFichier(f, pasMatch);
        else envoyer404();

        client.close();
    }

    private void envoyerFichier(File f, String ifNoneMatch) throws Exception {
        byte[] contenu = Files.readAllBytes(f.toPath());
        if (f.getName().toLowerCase().endsWith(".html")) {
            String html = new String(contenu);
            html = traiterDyna(html);
            contenu = html.getBytes();
        }
        String etag = calculerETag(contenu);
        OutputStream sortie = client.getOutputStream();
        String cheminAccesLog = home + ps.getAccesLog();
        String entetesBase = "Date: " + new Date().toString() + "\r\nServer: MiniWebJava\r\n";

        if (etag.equals(ifNoneMatch)) {
            sortie.write(("HTTP/1.1 304 Not Modified\r\n" + entetesBase + "ETag: " + etag + "\r\n\r\n").getBytes());
            ecrireLog(cheminAccesLog, "GET " + f.getName() + " -> 304 Not Modified");
        } else {
            String nomFichier = f.getName().toLowerCase();
            if (nomFichier.endsWith(".png") || nomFichier.endsWith(".jpg") || nomFichier.endsWith(".pdf")) {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream gzos = new GZIPOutputStream(baos);
                gzos.write(contenu);
                gzos.close();
                byte[] contenuCompresse = baos.toByteArray();


                sortie.write(("HTTP/1.1 200 OK\r\n" + entetesBase + "ETag: " + etag + "\r\nContent-Encoding: gzip\r\nContent-Length: " + contenuCompresse.length + "\r\n\r\n").getBytes());
                sortie.write(contenuCompresse);
                ecrireLog(cheminAccesLog, "GET " + f.getName() + " -> 200 OK (GZIP)");
            } else {

                sortie.write(("HTTP/1.1 200 OK\r\n" + entetesBase + "ETag: " + etag + "\r\nContent-Length: " + contenu.length + "\r\n\r\n").getBytes());
                sortie.write(contenu);
                ecrireLog(cheminAccesLog, "GET " + f.getName() + " -> 200 OK");
            }
        }
        sortie.flush();
    }

    private void envoyerListeFichiers(File dossier, String chemin) throws Exception {
        String html = "<html><body><h1>Index de " + chemin + "</h1><ul>";
        for (File entry : dossier.listFiles()) {
            String lien = chemin.endsWith("/") ? chemin + entry.getName() : chemin + "/" + entry.getName();
            html += "<li><a href=\"" + lien + "\">" + entry.getName() + "</a></li>";
        }
        html += "</ul></body></html>";
        byte[] contenu = html.getBytes();
        OutputStream sortie = client.getOutputStream();

        String entetesBase = "Date: " + new Date().toString() + "\r\nServer: MiniWebJava\r\n";
        sortie.write(("HTTP/1.1 200 OK\r\n" + entetesBase + "Content-Type: text/html\r\nContent-Length: " + contenu.length + "\r\n\r\n").getBytes());
        sortie.write(contenu);
        sortie.flush();

        ecrireLog(home + ps.getAccesLog(), "GET " + chemin + " (Dossier) -> 200 OK");
    }

    private void envoyerStatus() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        long memoireTotale = runtime.totalMemory() / (1024 * 1024);
        long memoireLibre = runtime.freeMemory() / (1024 * 1024);
        int processeurs = runtime.availableProcessors();

        // Espace disque
        File disque = new File(".");
        long disqueTotal = disque.getTotalSpace() / (1024 * 1024 * 1024);
        long disqueLibre = disque.getFreeSpace() / (1024 * 1024 * 1024);

        String html = "<html><body>";
        html += "<h1>Etat du serveur</h1>";
        html += "<p>Processeurs disponibles : " + processeurs + "</p>";
        html += "<p>Memoire totale allouee : " + memoireTotale + " Mo</p>";
        html += "<p>Memoire libre restante : " + memoireLibre + " Mo</p>";
        html += "<p>Espace disque total : " + disqueTotal + " Go</p>";
        html += "<p>Espace disque libre : " + disqueLibre + " Go</p>";
        html += "</body></html>";

        byte[] contenu = html.getBytes();
        OutputStream sortie = client.getOutputStream();
        String entetesBase = "Date: " + new Date().toString() + "\r\nServer: MiniWebJava\r\n";
        sortie.write(("HTTP/1.1 200 OK\r\n" + entetesBase + "Content-Type: text/html\r\nContent-Length: " + contenu.length + "\r\n\r\n").getBytes());
        sortie.write(contenu);
        sortie.flush();

        ecrireLog(home + ps.getAccesLog(), "GET /status -> 200 OK");
    }

    private void envoyer404() throws Exception {
        String msg = "Erreur 404 : Fichier introuvable";
        byte[] contenu = msg.getBytes();
        OutputStream sortie = client.getOutputStream();

        String entetesBase = "Date: " + new Date().toString() + "\r\nServer: MiniWebJava\r\n";
        sortie.write(("HTTP/1.1 404 Not Found\r\n" + entetesBase + "Content-Length: " + contenu.length + "\r\n\r\n").getBytes());
        sortie.write(contenu);
        sortie.flush();

        ecrireLog(home + ps.getErrorLog(), "404 Not Found");
    }

    private String calculerETag(byte[] contenu) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(contenu);
        String result = "";
        for (byte b : hash) result += String.format("%02x", b);
        return "\"" + result + "\"";
    }

    private void ecrireLog(String cheminFichier, String message) {
        try {
            File fichierLog = new File(cheminFichier);
            if (fichierLog.getParentFile() != null) {
                fichierLog.getParentFile().mkdirs();
            }

            FileWriter fw = new FileWriter(fichierLog, true);
            fw.write("[" + new Date().toString() + "] " + message + "\n");
            fw.close();
        } catch (Exception e) {
            System.out.println("Erreur écriture log : " + e.getMessage());
        }
    }

    private String traiterDyna(String html) {
        String resultat = html;
        int debut = resultat.indexOf("<code interpreteur=");

        while (debut != -1) {
            int debutInterp = resultat.indexOf("=", debut) + 1;
            int finInterp = resultat.indexOf(">", debutInterp);
            String interpreteur = resultat.substring(debutInterp, finInterp)
                    .replace("«", "").replace("»", "").replace("\"", "").trim();

            int debutCode = finInterp + 1;
            int finCode = resultat.indexOf("</code>", debutCode);
            String code = resultat.substring(debutCode, finCode).trim();

            String sortie = "";
            try {
                ProcessBuilder pb = new ProcessBuilder(interpreteur);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.getOutputStream().write(code.getBytes());
                process.getOutputStream().close();
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String ligne = br.readLine();
                while (ligne != null) {
                    sortie += ligne + " ";
                    ligne = br.readLine();
                }
                process.waitFor();
            } catch (Exception e) {
                sortie = "[Erreur : " + e.getMessage() + "]";
            }

            resultat = resultat.substring(0, debut) + sortie + resultat.substring(finCode + 7);
            debut = resultat.indexOf("<code interpreteur=");
        }
        return resultat;
    }
}