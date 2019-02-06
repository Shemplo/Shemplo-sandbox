package ru.shemplo.genome.rf.external;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class RRunner {
    
    public static void runR (String command, boolean debug) throws IOException, InterruptedException {
        command = String.format ("cmd /C cd src/main/r && %s", command);
        Process process = Runtime.getRuntime ().exec (command);
        
        if (debug) {
            printEverythingFromIs (process.getInputStream ());
            //printEverythingFromIs (process.getErrorStream ());
        }
        
        process.waitFor ();
    }
    
    public static void runPR (String comand) 
            throws IOException, InterruptedException {
        runR (comand, false);
    }
    
    private static BufferedReader brFromIs (InputStream is) throws IOException {
        Reader r = new InputStreamReader (is, StandardCharsets.UTF_8);
        return new BufferedReader (r);
    }
    
    private static void printEverythingFromIs (InputStream is) throws IOException {
        try (BufferedReader br = brFromIs (is)) {
            String line = null;
            while ((line = br.readLine ()) != null) {
                System.out.println (line);
            }
        }
    }
    
}
