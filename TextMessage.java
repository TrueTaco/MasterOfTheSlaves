import java.io.Serializable;

public class TextMessage implements Serializable {
    public String message;

    public TextMessage() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
