import java.io.Serializable;

public class Node implements Serializable {
    private String ID;
    private int port;
    private String type;

    public Node(String ID, int port, String type) {
        this.ID = ID;
        this.port = port;
        this.type = type;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
