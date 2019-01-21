package ru.shemplo.genome.rf.external;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PythonRunner {
    
    public static void runPython (String command, boolean debug) throws IOException, InterruptedException {
        command = String.format ("cmd /C bash -c \"cd src/main/python; %s\"", command);
        Process process = Runtime.getRuntime ().exec (command);
        
        if (debug) {
            printEverythingFromIs (process.getInputStream ());
            printEverythingFromIs (process.getErrorStream ());
        }
        
        process.waitFor ();
    }
    
    public static void runPython (String comand) 
            throws IOException, InterruptedException {
        runPython (comand, false);
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
