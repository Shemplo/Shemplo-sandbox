package ru.shemplo.metagennet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GraphGenerator {
    
    public static final File GEN_FILE = new File ("runtime/graph.csv");
    
    private static final Random R = new Random ();
    
    public static void main (String ... args) throws Exception {
        final int size = 40 + R.nextInt (11);
        double [][] matrix = new double [size][size];
        
        matrix [size - 1][size - 1] = R.nextDouble () * 10;
        
        for (int i = 0; i < matrix.length - 1; i++) {
            int neighborsN = R.nextInt (Math.min (matrix.length - i - 1, 7));
            matrix [i][i] = R.nextDouble ();
            
            System.out.println ("Generate " + neighborsN + " / " + size + " from " + (i + 1));
            for (int neighbor : generateUniqueExept (neighborsN, size, i + 1)) {
                matrix [i][neighbor] = matrix [neighbor][i] = R.nextDouble () / 10;
            }
        }
        
        GEN_FILE.getParentFile ().mkdirs ();
        try (
            OutputStream os = new FileOutputStream (GEN_FILE);
            Writer w = new OutputStreamWriter (os, StandardCharsets.UTF_8);
            PrintWriter pw = new PrintWriter (w);
        ) {
            for (int i = 0; i < matrix.length; i++) {
                StringJoiner sj = new StringJoiner (";");
                
                for (int j = 0; j < matrix [i].length; j++) {
                    sj.add (String.format (Locale.ENGLISH, "%.7f", matrix [i][j]));
                }
                
                pw.println (sj.toString ());
            }
        }
    }
    
    private static List <Integer> generateUniqueExept (int amount, int bound, int from) {
        if (amount > bound - from) {
            String message = "Impossible to generate more than `bound` unique values";
            throw new IllegalArgumentException (message);
        }
        
        Set <Integer> out = new HashSet <> ();
        while (out.size () < amount) {
            out.add (from + R.nextInt (bound - from));
        }
        
        return new ArrayList <> (out);
    }
    
}
