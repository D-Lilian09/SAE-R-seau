import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.File;

public class LecteurConfig {
    public static ParamSite[] lire(String cheminConfig) throws Exception {
        File configFile = new File(cheminConfig);
        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = dBuilder.parse(configFile);
        doc.getDocumentElement().normalize();

        NodeList siteNodes = doc.getElementsByTagName("site");
        ParamSite[] sites = new ParamSite[siteNodes.getLength()];

        for (int i = 0; i < siteNodes.getLength(); i++) {
            Element e = (Element) siteNodes.item(i);
            String contenu = e.getTextContent();

            int port = 80;
            String documentRoot = ".";
            String defaultIndex = "index.html";
            String accesLog = "logs/access.log";
            String errorLog = "logs/error.log";

            for (String line : contenu.split("\n")) {
                line = line.trim();
                if (line.startsWith("port")) port = Integer.parseInt(line.split(" ")[1]);
                else if (line.startsWith("DocumentRoot")) documentRoot = line.split(" ")[1];
                else if (line.startsWith("DefaultIndex")) defaultIndex = line.split(" ")[1];
                else if (line.startsWith("Acceslog")) accesLog = line.split(" ")[1];
                else if (line.startsWith("Errorlog")) errorLog = line.split(" ")[1];
            }
            sites[i] = new ParamSite(port, documentRoot, defaultIndex, accesLog, errorLog);
        }
        return sites;
    }
}