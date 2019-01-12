package ru.shemplo.genome.rf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.StringManip;

public class RunCsvComposer {
    
    private static final Map <String, List <Double>> 
        values = new HashMap <> ();
    
    private static final String [][] files = {
        {"freqs.csv",       ";"},
        {"sklearn-60.txt",  " "},
        {"sklearn-90.txt",  " "},
        {"sklearn-150.txt", " "},
        {"sklearn-250.txt", " "},
    };
    
    public static void main (String [] args) throws Exception { mainR (args); }
    
    public static double mainR (String [] args) throws Exception {
        for (String [] file : files) {
            readFile (file [0], file [1]);
        }
        
        double [] maxes = new double [files.length];
        values.values ().forEach (lst -> {
            for (int i = 0; i < lst.size (); i++) {
                maxes [i] = Math.max (maxes [i], lst.get (i));
            }
        });
        values.values ().forEach (lst -> {
            for (int i = 0; i < lst.size (); i++) {
                lst.set (i, lst.get (i) / (maxes [i] != 0 ? maxes [i] : 1.0));
            }
        });
        
        String destination = args.length > 0 ? String.format ("-run-%s-%s", args [1], args [0]) : "";
        Path path = Paths.get ("results", String.format ("table%s.csv", destination));
        try (
            PrintWriter pw = new PrintWriter (path.toFile ());
        ) {
            pw.println ("id;gene;mcmc;rf60;rf90;rf150;rf250");
            
            AtomicInteger counter = new AtomicInteger ();
            values.entrySet ().stream ()
            . map     (Pair::fromMapEntry)
            . forEach (p -> {
                StringJoiner sj = new StringJoiner (";");
                sj.add ("" + counter.incrementAndGet ());
                sj.add (p.F);
                
                p.S.stream ()
                . map      (Objects::toString)
                . forEach  (sj::add);
                pw.println (sj.toString ());
            });
        }
        
        AtomicReference <Double> distance = new AtomicReference <> (0D);
        values.values ().forEach (lst -> {
            double abs  = Math.abs (lst.get (0) - lst.get (1));
            distance.getAndAccumulate (abs * abs, Double::sum);
        });
        
        values.clear ();
        return distance.get ();
    }
    
    private static void readFile (String fileName, String separator) throws IOException {
        Path path = Paths.get ("temp", fileName);
        try (
            BufferedReader br = Files.newBufferedReader (path);
        ) {
            br.readLine (); // skip headers
            
            String line = null; int maxSize = 0;
            while ((line = StringManip.fetchNonEmptyLine (br)) != null) {
                final String [] vals = line.split (separator);
                vals [0] = vals [0].replace ("\"", "");
                
                values.putIfAbsent (vals [0], new ArrayList <> ());
                values.get (vals [0]).add (Double.parseDouble (vals [1]));
                maxSize = Math.max (maxSize, values.get (vals [0]).size ());
            }
            
            final int finalSize = maxSize;
            values.values ().forEach (list -> {
                while (list.size () != finalSize) {
                    list.add (0.0);
                }
            });
        }
    }
    
}
