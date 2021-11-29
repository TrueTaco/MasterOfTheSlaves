import java.util.ArrayList;

public class start {
    public static void main(String[] args) throws InterruptedException {

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
                }
                break;
            case "Client":
                slavePort = Integer.parseInt(args[1]);
                String slaveDNS = args[2];
                int amountOfPrimes = Integer.parseInt(args[3]);

                startClient(slavePort, slaveDNS, amountOfPrimes);
                break;
        }
    }

    public static void startMaster(int masterPort) {
        System.out.println("Starting master at " + masterPort);
    }

    public static void startSlave(int masterPort, String masterDNS, int slavePort) {
        System.out.println("Starting slave at " + slavePort + " connected to " + masterDNS + ":" + masterPort);
    }

    public static void startClient(int slavePort, String slaveDNS, int amountOfPrimes) {
        System.out.println("Starting client connected to " + slaveDNS + ":" + slavePort + " with " + amountOfPrimes + " primes to check");
    }
}
