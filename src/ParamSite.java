public class ParamSite {
    private int port;
    private String documentRoot;
    private String defaultIndex;
    private String accesLog;
    private String errorLog;

    public ParamSite(int p, String dr, String di, String a, String e) {
        this.port = p;
        this.documentRoot = dr;
        this.defaultIndex = di;
        this.accesLog = a;
        this.errorLog = e;
    }

    public int getPort() {
        return port;
    }

    public String getDocumentRoot() {
        return documentRoot;
    }

    public String getDefaultIndex() {
        return defaultIndex;
    }

    public String getAccesLog() {
        return accesLog;
    }

    public String getErrorLog() {
        return errorLog;
    }
}

