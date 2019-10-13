package ru.shemplo.scanner;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class RunISvsR {
    
    public static void main (String ... args) throws IOException {
        String testString = generateString (10_000_000);
        
        try (
            InputStream is  = new ByteArrayInputStream (testString.getBytes ());
            Reader r = new InputStreamReader (is, StandardCharsets.UTF_8);
            
            InputStream is2 = new ByteArrayInputStream (testString.getBytes ());
            Reader r2 = new InputStreamReader (is2, StandardCharsets.UTF_8);
            Reader br2 = new BufferedReader (r2);
            
            InputStream is3 = new ByteArrayInputStream (testString.getBytes ());
            UTF8Reader ur3 = new UTF8Reader (is3);
                
            InputStream is4 = new ByteArrayInputStream (testString.getBytes ());
            InputStream bis4 = new BufferedInputStream (is4);
            UTF8Reader ur4 = new UTF8Reader (bis4);
        ) {
            int character = -1;
            
            long time0 = System.nanoTime ();
            int letters0 = 0;
            character = -1;
            while ((character = r.read ()) != -1) {
                if (Character.isLetter (character)) {
                    letters0++;
                }
            }
            
            long time1 = System.nanoTime ();
            int letters1 = 0;
            character = -1;
            while ((character = br2.read ()) != -1) {
                if (Character.isLetter (character)) {
                    letters1++;
                }
            }
            
            long time2 = System.nanoTime ();
            int letters2 = 0;
            character = -1;
            while ((character = ur3.read ()) != -1) {
                if (Character.isLetter (character)) {
                    letters2++;
                }
            }
            
            long time3 = System.nanoTime ();
            int letters3 = 0;
            character = -1;
            while ((character = ur4.read ()) != -1) {
                if (Character.isLetter (character)) {
                    letters3++;
                }
            }
            
            long time4 = System.nanoTime ();
            System.out.println (String.format ("Reader: %dms, BufferedReader: %dms, "
                    + "InputStream: %dms, BufferedInputStream: %dms", 
                (time1 - time0) / 1000000, (time2 - time1) / 1000000, (time3 - time2) / 1000000,
                (time4 - time3) / 1000000));
            assert letters0 == letters1;
            assert letters0 == letters2;
            assert letters0 == letters3;
        }
    }
    
    private static String generateString (int length) {
        StringBuilder sb = new StringBuilder ();
        Random r = new Random ();
        
        for (int i = 0; i < length; i++) {
            sb.append ((char) (' ' + r.nextInt (10000)));
        }
        
        return sb.toString ();
    }
    
}
