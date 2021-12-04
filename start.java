import java.util.ArrayList;

public class start {
    public static ArrayList<Thread> threads = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {

        // INFO: Einbindung von BouncyCastle funktioniert, ohne extra Arbeit
        // INFO: Projekt wird zurzeit mit JDK 11 kompiliert, da Raspberry Pi OpenSDK 11 verwendet
        // INFO: Das funktioniert allerdings trotzdem irgendwie nicht
        // TODO: JDK Version ggf. wieder überall auf 17 stellen (Projekteinstellungen + Java Compiler)
        // TODO: Einfach in Requirements schreiben, dass wir JDK 17 verwenden
        // TODO: Ggf. JDK 17 auf Raspberry Pi installieren

        String type = args[0];
        int masterPort;
        int slavePort;

        switch (type) {
            case "Master":
                masterPort = Integer.parseInt(args[1]);

                startMaster(masterPort);
                break;
            case "Slave":
                masterPort = Integer.parseInt(args[1]);
                String masterDNS = args[2];
                int amountOfSlaves = args.length - 3;

                for (int i = 3; i < (3 + amountOfSlaves); i++) {
                    slavePort = Integer.parseInt(args[i]);
                    startSlave(masterPort, masterDNS, slavePort);
                    Thread.sleep(300);
                }
                break;
            case "Client":
                slavePort = Integer.parseInt(args[1]);
                String slaveDNS = args[2];
                int amountOfPrimes = Integer.parseInt(args[3]);

                startClient(slavePort, slaveDNS, amountOfPrimes);
                break;
        }

        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).join();
            System.out.println("Thread no. " + (i + 1) + " has been joined");
        }
    }

    public static void startMaster(int masterPort) {
        System.out.println("Starting master at :" + masterPort);
        MasterSlave runnableMaster = new MasterSlave("Master", masterPort);
        createThread(runnableMaster);
    }

    public static void startSlave(int masterPort, String masterDNS, int slavePort) {
        System.out.println("Starting slave at :" + slavePort + " connected to " + masterDNS + ":" + masterPort);
        MasterSlave runnableSlave = new MasterSlave("Slave", slavePort, masterPort, masterDNS);
        createThread(runnableSlave);
    }

    public static void startClient(int slavePort, String slaveDNS, int amountOfPrimes) {
        System.out.println("Starting client connected to " + slaveDNS + ":" + slavePort + " with " + amountOfPrimes + " primes to check");
        Client runnableClient = new Client(slavePort, slaveDNS, amountOfPrimes);
        createThread(runnableClient);
    }

    public static void createThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
        threads.add(thread);
    }
}
