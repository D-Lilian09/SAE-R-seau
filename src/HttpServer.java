public class HttpServer {
    public static void main(String[] args) throws Exception {
        ParamSite[] sites = LecteurConfig.lire("serverWeb.conf");
        for (int i = 0; i < sites.length; i++) {
            System.out.println("Port configuré : " + sites[i].getPort());
            new Ecouteur(sites[i]).start(); // Ecouteur, pas Handler
        }
    }
}