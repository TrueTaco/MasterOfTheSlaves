import org.w3c.dom.Text;

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

            Thread.sleep(1000);
            // TODO: Alle public keys hier im Client mal speichern und dann auf basis der länge der primes mitgeben
            String publicKey = "237023640130486964288372516117459992717";
            String chiffre = "b4820013b07bf8513ee59a905039fb631203c8b38ca3d59b475b4e4e092d3979";
            String amountOfPrimes = "10000";

            String[] rsaInformation = {publicKey, chiffre, amountOfPrimes};

            Message message = new Message();
            message.setType("RSA");
            message.setPayload(rsaInformation);
            //System.out.println(message.getTime());
            System.out.println("\nClient sent: RSA Information");

            objectOutputStream.writeObject(message);

            //sendMultiple(objectOutputStream, objectInputStream);

            while(true) {
                message = readStream(objectInputStream);
                if(message.getType().equals("RSA-RESPONSE")) {
                    TextMessage textMessage = (TextMessage) message.getPayload();
                    System.out.println("Client received: " + textMessage.getMessage());
                }
            }

        } catch (IOException | InterruptedException e) {
            System.out.println("A client error occured.");
            e.printStackTrace();
        }
    }

    public void sendMultiple(ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream) throws IOException, InterruptedException {
        int count = 0;
        int limit = 1; // sets the amount of the messages to send
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
                System.out.println("Client received: " + text);
                int randomDelay = ThreadLocalRandom.current().nextInt(500, 1000);
                Thread.sleep(randomDelay);

                count++;
            } else {
                objectOutputStream.writeObject(read(1));
                objectOutputStream.flush();
                Message message = readStream(objectInputStream);
                text = ((TextMessage) message.getPayload()).getMessage();
                System.out.println("Client received: " + text);
                readStream(objectInputStream);

                count++;
            }
        }
    }

    public Message sendMessage(String type, String payload) {
        TextMessage textMessage = new TextMessage();
        textMessage.setMessage(payload);

        Message message = new Message();
        message.setType(type);
        message.setPayload(textMessage);

        System.out.println("\nClient sent: (" + type + ") " + payload);

        return message;
    }

    public Message write(String txt) {
        Message message = sendMessage("WRITE", txt);
        return message;
    }

    public Message read(int number) {
        Message message = sendMessage( "READ", String.valueOf(number));
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