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

    public void run() {
        pid = ProcessHandle.current().pid();
        tid = Thread.currentThread().getId();

        RSAHelper helper = new RSAHelper();
        String p;
        String q;
        boolean isValid = false;

        for (int i = startIndex; i < endIndex; i++) {
            p = primes.get(i);
            for (int j = 0; j < primes.size(); j++) {
                q = primes.get(j);
                isValid = helper.isValid(p, q, publicKey);
                if (isValid) {
                    System.out.println("WorkingSlave " + pid + "-" + tid + " found: p = " + p + ", q = " + q);
                    try {
                        father.shareSolution(p, q);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    i = endIndex - 1;
                    break;
                }
            }
//            if ((i+1) % 10 == 0) System.out.println("Round " + i + " done");
        }
        //System.out.println("WorkingSlave " + pid + "-" + tid + " is trying to kill itself");
        try {
            father.annihilateWorkingThread();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // TODO: Wenn fertig, dann Methode aus father ausrufen, die mich vernichtet
    }
}