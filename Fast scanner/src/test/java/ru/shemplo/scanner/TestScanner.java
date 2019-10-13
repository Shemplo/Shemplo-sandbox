package ru.shemplo.scanner;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.junit.jupiter.api.Test;

import ru.shemplo.snowball.stuctures.Pair;

public class TestScanner {
    
    private static Pair <String, List <String>> generateStressTest (int wordsBound) {
        List <String> words = new ArrayList <> ();
        StringBuilder sb = new StringBuilder ();
        Random r = new Random ();
        
        int wordsNumber = wordsBound + r.nextInt (100);
        for (int i = 0; i < wordsNumber; i++) {
            for (int j = 0; j < 1 + r.nextInt (5); j++) {
                sb.append (" ");
            }
            
            int wordLength = 5 + r.nextInt (10);
            String word = "";
            for (int j = 0; j < wordLength; j++) {
                word += (char) ('a' + r.nextInt (26 * 2));
            }
            words.add (word);
            sb.append (word);
            
            for (int j = 0; j < 2 + r.nextInt (5); j++) {
                sb.append (" ");
            }
        }
        
        return Pair.mp (sb.toString (), words);
    }
    
    @Test
    public void testToken () throws IOException {
        try (
            InputStream is = new ByteArrayInputStream ("word".getBytes ());
            var scanner = new FastScanner (is, StandardCharsets.UTF_8);
        ) {
            assertTrue (scanner.hasNext ());
            assertTrue (scanner.hasNext ());
            assertEquals ("word", scanner.next ());
            
            assertFalse (scanner.hasNext ());
        }
    }
    
    @Test
    public void test2Tokens () throws IOException {
        try (
            InputStream is = new ByteArrayInputStream ("two words".getBytes ());
            var scanner = new FastScanner (is, StandardCharsets.UTF_8);
        ) {
            assertTrue (scanner.hasNext ());
            assertEquals ("two", scanner.next ());
            
            assertTrue (scanner.hasNext ());
            assertEquals ("words", scanner.next ());
            
            assertFalse (scanner.hasNext ());
        }
    }
    
    @Test
    public void testTokensInLines () throws IOException {
        try (
            InputStream is = new ByteArrayInputStream ("a b\nc  \t d e\nf g h i \n j k ".getBytes ());
            var scanner = new FastScanner (is, StandardCharsets.UTF_8);
        ) {
            assertTrue (scanner.hasNext ());
            assertTrue (scanner.hasNextInLine ());
            assertEquals ("a", scanner.next ());
            
            assertTrue (scanner.hasNext ());
            assertTrue (scanner.hasNextInLine ());
            assertEquals ("b", scanner.next ());
            
            assertTrue (scanner.hasNext ());
            assertFalse (scanner.hasNextInLine ());
            assertEquals ("c", scanner.next ());
            
            assertTrue (scanner.hasNext ());
            assertTrue (scanner.hasNextInLine ());
            assertEquals ("d", scanner.next ());
            
            assertTrue (scanner.hasNext ());
            assertTrue (scanner.hasNextInLine ());
            assertEquals ("e", scanner.next ());
            
            assertTrue (scanner.hasNext ());
            assertFalse (scanner.hasNextInLine ());
            assertEquals ("f", scanner.next ());
            
            assertTrue (scanner.hasNext ());
            assertTrue (scanner.hasNextInLine ());
            assertEquals ("g", scanner.next ());
            
            assertTrue (scanner.hasNext ());
            assertTrue (scanner.hasNextInLine ());
            assertEquals ("h", scanner.next ());
            
            assertTrue (scanner.hasNext ());
            assertTrue (scanner.hasNextInLine ());
            assertEquals ("i", scanner.next ());
            
            assertTrue (scanner.hasNext ());
            assertFalse (scanner.hasNextInLine ());
            assertEquals ("j", scanner.next ());
            
            assertTrue (scanner.hasNext ());
            assertTrue (scanner.hasNextInLine ());
            assertEquals ("k", scanner.next ());
        }
    }
    
    @Test
    public void testUnicodeTokens () throws IOException {
        try (
            InputStream is = new ByteArrayInputStream ("Привет мир".getBytes ());
            var scanner = new FastScanner (is, StandardCharsets.UTF_8);
        ) {
            assertTrue (scanner.hasNext ());
            assertEquals ("Привет", scanner.next ());
            
            assertTrue (scanner.hasNext ());
            assertEquals ("мир", scanner.next ());
            
            assertFalse (scanner.hasNext ());
        }
    }
    
    @Test
    public void testOverBufferStream () throws IOException {
        Pair <String, List <String>> test = generateStressTest (32000);
        
        try (
            InputStream is = new ByteArrayInputStream (test.F.getBytes ());
            var scanner = new FastScanner (is, StandardCharsets.UTF_8);
        ) {
            for (String word : test.S) {
                assertTrue (scanner.hasNext ());
                assertEquals (word, scanner.next ());
            }
            
            assertFalse (scanner.hasNext ());
        }
    }
    
    @Test
    public void testInteger () throws IOException {
        try (
            InputStream is = new ByteArrayInputStream ("436".getBytes ());
            var scanner = new FastScanner (is, StandardCharsets.UTF_8);
        ) {
            assertTrue (scanner.hasNext ());
            assertTrue (scanner.hasNextInt ());
            assertTrue (scanner.hasNextInt ());
            assertEquals (436, scanner.nextInt ());
            
            assertFalse (scanner.hasNext ());
        }
    }
    
    @Test
    public void testIntegers () throws IOException {
        try (
            InputStream is = new ByteArrayInputStream ("436 56432".getBytes ());
            var scanner = new FastScanner (is, StandardCharsets.UTF_8);
        ) {
            assertTrue (scanner.hasNext ());
            assertTrue (scanner.hasNextInt ());
            assertTrue (scanner.hasNextInt ());
            assertEquals (436, scanner.nextInt ());
            
            assertTrue (scanner.hasNextInt ());
            assertEquals (56432, scanner.nextInt ());
            
            assertFalse (scanner.hasNext ());
        }
    }
    
    @Test
    public void testPerformance () throws IOException {
        Pair <String, List <String>> test = generateStressTest (128000);
        
        try (
            InputStream is2 = new ByteArrayInputStream (test.F.getBytes ());
            InputStream is = new ByteArrayInputStream (test.F.getBytes ());
        ) { 
            long time0 = System.nanoTime ();
            var scanner = new FastScanner (is, StandardCharsets.UTF_8);
            int length0 = 0;
            while (scanner.hasNext ()) {
                length0 += scanner.next ().length ();
            }
            
            long time1 = System.nanoTime ();
            StringTokenizer st = new StringTokenizer (test.F);
            int length1 = 0;
            while (st.hasMoreTokens ()) {
                length1 += st.nextToken ().length ();
            }
            
            long time2 = System.nanoTime ();
            Scanner sc = new Scanner (is2);
            int length2 = 0;
            while (sc.hasNext ()) {
                length2 += sc.next ().length ();
            }
            
            long time3 = System.nanoTime ();
            scanner.close ();
            sc.close ();
            
            System.out.println (String.format ("Fast scanner: %dmcs, StringTokenizer: %dmcs, Scanner: %dmcs", 
                (time1 - time0) / 1000, (time2 - time1) / 1000, (time3 - time2) / 1000));
            assertEquals (length1, length0); assertEquals (length1, length2);
            assertTrue ((time1 - time0) < (time3 - time2));
        }
    }
    
}
