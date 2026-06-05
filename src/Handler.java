import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.MessageDigest;

public class Handler extends Thread {
    private Socket client;
    private ParamSite ps;

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
        if (fichier.equals("/")) fichier = "/" + ps.getDefaultIndex();

        String pasMatch = null;
        String ligne = bf.readLine();
        while (ligne != null && !ligne.equals("")) {
            if (ligne.startsWith("If-None-Match:")) pasMatch = ligne.split(": ")[1].trim();
            ligne = bf.readLine();
        }

        String fichierLocal = ps.getDocumentRoot() + File.separator + fichier.substring(1);
        File f = new File(fichierLocal);

        if (f.isDirectory()) envoyerListeFichiers(f, fichier);
        else if (f.exists()) envoyerFichier(f, pasMatch);
        else envoyer404();

        client.close();
    }

    private void envoyerFichier(File f, String ifNoneMatch) throws Exception {
        byte[] contenu = Files.readAllBytes(f.toPath());
        String etag = calculerETag(contenu);
        OutputStream sortie = client.getOutputStream();

        if (etag.equals(ifNoneMatch)) {
            sortie.write(("HTTP/1.1 304 Not Modified\r\nETag: " + etag + "\r\n\r\n").getBytes());
        } else {
            sortie.write(("HTTP/1.1 200 OK\r\nETag: " + etag + "\r\nContent-Length: " + contenu.length + "\r\n\r\n").getBytes());
            sortie.write(contenu);
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
        sortie.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: " + contenu.length + "\r\n\r\n").getBytes());
        sortie.write(contenu);
        sortie.flush();
    }

    private void envoyer404() throws Exception {
        String msg = "Erreur 404 : Fichier introuvable";
        byte[] contenu = msg.getBytes();
        OutputStream sortie = client.getOutputStream();
        sortie.write(("HTTP/1.1 404 Not Found\r\nContent-Length: " + contenu.length + "\r\n\r\n").getBytes());
        sortie.write(contenu);
        sortie.flush();
    }

    private String calculerETag(byte[] contenu) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(contenu);
        String result = "";
        for (byte b : hash) result += String.format("%02x", b);
        return "\"" + result + "\"";
    }
}