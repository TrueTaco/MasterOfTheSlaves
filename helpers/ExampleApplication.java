package helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class ExampleApplication {

    public static void main(String[] args) {
        RSAHelper helper = new RSAHelper();
        ArrayList<String> primes = new ArrayList<>();

        String publicKey = "298874689697528581074572362022003292763"; // the given public key
        String chiffre = "b4820013b07bf8513ee59a905039fb631203c8b38ca3d59b475b4e4e092d3979";
        String p;
        String q;

        primes = readFromFile("primes100.txt");
        boolean isValid = false;

        for (int i = 0; i < primes.size(); i++) {
            p = primes.get(i);
            for (int j = 0; j < primes.size(); j++) {
                q = primes.get(j);
                isValid = helper.isValid(p, q, publicKey);
                if (isValid) {
                    System.out.println("Found it: p = " + p + ", q = " + q);
                }
            }
            if ((i+1) % 10 == 0) System.out.println("Round " + i + " done");
        }
        /*
        p = "17594063653378370033"; // the prime 'p' to check, we can brute force and iterate throught the values
        q = "15251864654563933379"; // the prime 'q' to check, we can brute force and iterate throught the values
        boolean isValid = helper.isValid(p, q, publicKey);
        System.out.println("P/Q fit to the public key: " + helper.isValid(p, q, publicKey));
        if (isValid) {
            System.out.println("Decrypted text is: " + helper.decrypt(p, q, chiffre));
        }*/
    }

    public static ArrayList readFromFile(String filename) {
        ArrayList<String> content = new ArrayList<>();

        try {
            File file = new File(filename);
            Scanner myReader = new Scanner(file);

            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                content.add(data);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occured while reading the file");
            e.printStackTrace();
        }
        return content;
    }

}
