import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.concurrent.ThreadLocalRandom;

public class Client implements Runnable {

    private long pid;
    private long tid;

    private int port;
    private String dns;
    private int amountOfPrimes;

    public Client(int port, String dns, int amountOfPrimes) {
        this.port = port;
        this.dns = dns;
        this.amountOfPrimes = amountOfPrimes;
    }

    public void run() {
        try {
            pid = ProcessHandle.current().pid();
            tid = Thread.currentThread().getId();

            Socket cs = initialise(port, dns);

            OutputStream outputStream = cs.getOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

            InputStream inputStream = cs.getInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

            System.out.println("Client " + pid + "-" + tid + " is ready");

            Thread.sleep(1000);

            Message message = createRSA(amountOfPrimes);

            if(message != null) objectOutputStream.writeObject(message);

            //sendMultiple(objectOutputStream, objectInputStream);

            // Print out all received messages
            while (true) {
                message = readStream(objectInputStream);

                TextMessage textMessage = (TextMessage) message.getPayload();
                System.out.println("Client received: " + textMessage.getMessage());

            }

        } catch (IOException | InterruptedException e) {
            System.out.println("A client error occured.");
            e.printStackTrace();
        }
    }

    // Creates message with RSA computing information
    public Message createRSA(int amountOfPrimes){
        boolean allowRequest = true;
        String publicKey = "";
        String chiffre = "";

        switch (amountOfPrimes){
            case 100:
                publicKey = "298874689697528581074572362022003292763";
                chiffre = "b4820013b07bf8513ee59a905039fb631203c8b38ca3d59b475b4e4e092d3979";
                break;
            case 1000:
                publicKey = "249488851623337787855631201847950907117";
                chiffre = "55708f0326a16870b299f913984922c7b5b37725ce0f6670d963adc0dc3451c8";
                break;
            case 10000:
                publicKey = "237023640130486964288372516117459992717";
                chiffre = "a9fc180908ad5f60556fa42b3f76e30f48bcddfad906f312b6ca429f25cebbd0";
                break;
            case 100000:
                publicKey = "174351747363332207690026372465051206619";
                chiffre = "80f7b3b84e8354b36386c6833fe5c113445ce74cd30a21236a5c70f5fdca7208";
                break;
            default:
                System.out.println("\nAmount of primes is not set correctly");
                allowRequest = false;
                break;
        }

        if(!allowRequest) return null;

        String[] rsaInformation = {publicKey, chiffre, String.valueOf(amountOfPrimes)};

        Message message = new Message();
        message.setType("RSA");
        message.setPayload(rsaInformation);
        System.out.println("\nClient sent: RSA Information for " + amountOfPrimes + " primes");
        return message;
    }

    // Sends multiple prefabricated messages to the connected slave
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

    // Returns message with text message as payload depending on the input type and payload
    public Message createMessage(String type, String payload) {
        TextMessage textMessage = new TextMessage();
        textMessage.setMessage(payload);

        Message message = new Message();
        message.setType(type);
        message.setPayload(textMessage);

        return message;
    }

    public Message write(String txt) {
        Message message = createMessage("WRITE", txt);
        return message;
    }

    public Message read(int number) {
        Message message = createMessage("READ", String.valueOf(number));
        return message;
    }

    // Reads given objectInputStream and returns the read message
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