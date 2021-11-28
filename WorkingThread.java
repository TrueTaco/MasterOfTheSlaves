import resources.RSAHelper;

import java.io.IOException;
import java.util.ArrayList;

public class WorkingThread implements Runnable {

    MasterSlave father;
    private long pid;
    private long tid;

    public String publicKey;
    public int startIndex = -1;
    public int endIndex;
    private ArrayList<String> primes;

    public WorkingThread(String publicKey, int startIndex, int endIndex, ArrayList<String> slavePrimes, MasterSlave father) {
        this.publicKey = publicKey;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.primes = slavePrimes;
        this.father = father;
    }

    // Tries to solve RSA with gives ranges for the primes and the publicKey
    public void run() {
        pid = ProcessHandle.current().pid();
        tid = Thread.currentThread().getId();

        RSAHelper helper = new RSAHelper();
        String p;
        String q;
        boolean isValid = false;

        // Test all primes in given range
        for (int i = startIndex; i < endIndex; i++) {
            p = primes.get(i);
            for (int j = 0; j < primes.size(); j++) {
                q = primes.get(j);
                isValid = helper.isValid(p, q, publicKey);
                if (isValid) {
                    System.out.println("WorkingSlave " + pid + "-" + tid + " found: p = " + p + ", q = " + q);
                    try {
                        // Send solution to master
                        father.shareSolution(p, q);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    i = endIndex - 1;
                    break;
                }
            }
        }
        System.out.println("WorkingSlave " + pid + "-" + tid + " is trying to close itself");
        try {
            // Close this thread
            father.closeWorkingThread();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}