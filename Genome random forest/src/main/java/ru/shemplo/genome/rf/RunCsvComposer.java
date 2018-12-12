package ru.shemplo.genome.rf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.StringManip;

public class RunCsvComposer {
    
    private static final Map <String, List <Double>> 
        values = new HashMap <> ();
    
    public static void main (String [] args) throws Exception {
        readFile ("sklearn-60.txt", " ");
        readFile ("sklearn-90.txt", " ");
        readFile ("sklearn-150.txt", " ");
        readFile ("sklearn-250.txt", " ");
        readFile ("freqs.csv", ",");
        
        Path path = Paths.get ("results", "table.csv");
        try (
            PrintWriter pw = new PrintWriter (path.toFile ());
        ) {
            pw.println ("id,gene,rf60,rf90,rf150,rf250,mcmc");
            
            AtomicInteger counter = new AtomicInteger ();
            values.entrySet ().stream ()
            . map     (Pair::fromMapEntry)
            . forEach (p -> {
                StringJoiner sj = new StringJoiner (",");
                sj.add ("" + counter.incrementAndGet ());
                sj.add (p.F);
                
                p.S.stream ()
                . map     (Objects::toString)
                . forEach (sj::add);
                pw.println (sj.toString ());
            });
        }
    }
    
    private static void readFile (String fileName, String separator) throws IOException {
        Path path = Paths.get ("temp", fileName);
        try (
            BufferedReader br = Files.newBufferedReader (path);
        ) {
            br.readLine (); // skip headers
            
            String line = null;
            while ((line = StringManip.fetchNonEmptyLine (br)) != null) {
                final String [] vals = line.split (separator);
                vals [0] = vals [0].replace ("\"", "");
                
                values.putIfAbsent (vals [0], new ArrayList <> ());
                values.get (vals [0]).add (Double.parseDouble (vals [1]));
            }
        }
    }
    
}
