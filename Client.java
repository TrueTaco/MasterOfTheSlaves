import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.concurrent.ThreadLocalRandom;

public class Client implements Runnable {

    private long pid;
    private long tid;

    private int port;
    private String dns;

    public Client(int port, String dns) {
        this.port = port;
        this.dns = dns;
    }

    public void run() {
        try {
            pid =  ProcessHandle.current().pid();
            tid = Thread.currentThread().getId();

            Socket cs = initialise(port, dns);

            OutputStream outputStream = cs.getOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

            InputStream inputStream = cs.getInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

            System.out.println("Client " + pid + "-" + tid + " is ready");

            sendMultiple(objectOutputStream, objectInputStream);
            while (true) {
                Message message = readStream(objectInputStream);
                String text = ((TextMessage) message.getPayload()).getMessage();
                System.out.println("Client: received " + text);
                readStream(objectInputStream);
            }

        } catch (IOException | InterruptedException e) {
            System.out.println("A client error occured.");
            e.printStackTrace();
        }
    }

    public void sendMultiple(ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream) throws IOException, InterruptedException {
        int count = 0;
        int limit = 2; // sets the amount of the messages to send
        String text = "";

        while (count <= limit) {
            if (count < limit) {
                long datetime = System.currentTimeMillis();
                Timestamp ts = new Timestamp(datetime);

                text = count + " | " + ts;
                objectOutputStream.writeObject(write(text));
                objectOutputStream.flush();

                Message message = readStream(objectInputStream);
                text = ((TextMessage) message.getPayload()).getMessage();
                System.out.println("Client: received " + text);
                readStream(objectInputStream);

                int randomDelay = ThreadLocalRandom.current().nextInt(500, 1000);
                Thread.sleep(randomDelay);

                count++;
            } else {
                objectOutputStream.writeObject(read(1));
                objectOutputStream.flush();
                Message message = readStream(objectInputStream);
                text = ((TextMessage) message.getPayload()).getMessage();
                System.out.println("Client: received " + text);
                readStream(objectInputStream);

                count++;
            }
        }
    }

    public Message write(String txt) {
        TextMessage textMessage = new TextMessage();
        textMessage.setMessage(txt);

        Message message = new Message();
        message.setType("WRITE");
        message.setPayload(textMessage);
        System.out.println(message.getTime());

        System.out.println("Client: sending WRITE");

        return message;
    }

    public Message read(int number) {
        TextMessage textMessage = new TextMessage();
        textMessage.setMessage("2");

        Message message = new Message();
        message.setType("READ");
        message.setPayload(textMessage);

        System.out.println("Client: sending read");

        return message;
    }

    public Message readStream(ObjectInputStream ois) {
        Message ret = null;
        try {
            ret = (Message) ois.readObject();
        } catch (Exception e) {
            System.err.println(e.toString());

        }
        return ret;
    }

    public Socket initialise(int port, String dns) throws IOException {
        Socket cs = new Socket(dns, port);

        return cs;
    }
}