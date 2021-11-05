package Übung5;

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

            sendMultiple(objectOutputStream);

        } catch (IOException | InterruptedException e) {
            System.out.println("A client error occured.");
            e.printStackTrace();
        }
    }

    public void sendMultiple(ObjectOutputStream objectOutputStream) throws IOException, InterruptedException {
        int count = 0;
        int limit = 3; // sets the amount of the messages to send
        String text = "";

        while (count <= limit) {
            if (count < limit) {
                long datetime = System.currentTimeMillis();
                Timestamp ts = new Timestamp(datetime);

                text = count + " | " + ts;
                objectOutputStream.writeObject(write(text));

                int randomDelay = ThreadLocalRandom.current().nextInt(500, 1000);
                Thread.sleep(randomDelay);

                count++;
            } else {
                objectOutputStream.writeObject(read(1));
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

        System.out.println("1. Client " + pid + "-" + tid + " WRITE: " + txt);

        return message;
    }

    public Message read(int number) {
        TextMessage textMessage = new TextMessage();
        textMessage.setMessage("");

        Message message = new Message();
        message.setType("READ");
        message.setPayload(textMessage);

        System.out.println("1. Client " + pid + "-" + tid + " READ: " + number);

        return message;
    }

    public Socket initialise(int port, String dns) throws IOException {
        Socket cs = new Socket(dns, port);

        return cs;
    }
}