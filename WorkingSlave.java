import resources.RSAHelper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class WorkingSlave implements Runnable {

    MasterSlave father;
    private long pid;
    private long tid;

    public String publicKey;
    public int startIndex = -1;
    public int endIndex;
    private ArrayList<String> primes;

    public WorkingSlave(String publicKey, int startIndex, int endIndex, ArrayList<String> slavePrimes) {
        this.publicKey = publicKey;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.primes = slavePrimes;
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
                    return;
                }
            }
//            if ((i+1) % 10 == 0) System.out.println("Round " + i + " done");
        }
        System.out.println("WorkingSlave " + pid + "-" + tid + " was not able to meet the required standards and therefore was eliminated");

        // TODO: Wenn fertig, dann Methode aus father ausrufen, die mich vernichtet
    }
}