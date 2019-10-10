package ru.shemplo.scanner;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class TestScanner {
    
    @Test
    public void testToken () throws IOException {
        try (
            InputStream is = new ByteArrayInputStream ("word".getBytes ());
        ) {
            var scanner = new FastScanner (is);
            
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
        ) {
            var scanner = new FastScanner (is);
            
            assertTrue (scanner.hasNext ());
            assertEquals ("two", scanner.next ());
            
            assertTrue (scanner.hasNext ());
            assertEquals ("words", scanner.next ());
            
            assertFalse (scanner.hasNext ());
        }
    }
    
    @Test
    public void testUnicodeTokens () throws IOException {
        try (
            InputStream is = new ByteArrayInputStream ("Привет мир".getBytes ());
        ) {
            var scanner = new FastScanner (is);
            
            assertTrue (scanner.hasNext ());
            assertEquals ("Привет", scanner.next ());
            
            assertTrue (scanner.hasNext ());
            assertEquals ("мир", scanner.next ());
            
            assertFalse (scanner.hasNext ());
        }
    }
    
    @Test
    public void testOverBufferStream () throws IOException {
        List <String> words = new ArrayList <> ();
        StringBuilder sb = new StringBuilder ();
        Random r = new Random ();
        
        int wordsNumber = 32000 + r.nextInt (100);
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
        
        try (
            InputStream is = new ByteArrayInputStream (sb.toString ().getBytes ());
        ) {
            var scanner = new FastScanner (is);
            
            for (String word : words) {
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
        ) {
            var scanner = new FastScanner (is);
            
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
        ) {
            var scanner = new FastScanner (is);
            
            assertTrue (scanner.hasNext ());
            assertTrue (scanner.hasNextInt ());
            assertTrue (scanner.hasNextInt ());
            assertEquals (436, scanner.nextInt ());
            
            assertTrue (scanner.hasNextInt ());
            assertEquals (56432, scanner.nextInt ());
            
            assertFalse (scanner.hasNext ());
        }
    }
    
}
