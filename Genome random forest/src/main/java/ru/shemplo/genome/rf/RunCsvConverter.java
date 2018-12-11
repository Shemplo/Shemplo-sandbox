package ru.shemplo.genome.rf;

import static java.util.Arrays.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.shemplo.genome.rf.data.DataParser;
import ru.shemplo.genome.rf.data.EntityVerdict;
import ru.shemplo.genome.rf.data.SourceDataset;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.fun.StreamUtils;

public class RunCsvConverter {
    
    public static void main (String ... args) throws Exception { 
        Path path = Paths.get ("GSE3189_series_matrix.txt");
        SourceDataset dataset = null;
        try (                
            InputStream is = Files.newInputStream (path);
        ) {
            dataset = new DataParser ().parse (is);
        }
        
        final Map <String, String> decodedNames = new HashMap <> ();
        String baseFile = "/de.csv";
        try (
            InputStream is = RunRandomForest.class.getResourceAsStream (baseFile);
            Reader r = new InputStreamReader (is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader (r);
        ) {
            br.readLine (); // Skip titles line
            
            String line = null;
            while ((line = br.readLine ()) != null) {
                if (line.length () == 0) { continue; }
                
                List <String> tokens = Arrays.asList (line.split ("\",\""));
                decodedNames.put (tokens.get (2), tokens.get (4)); 
                //                           ID / Gene.symbol
            }
        }
        
        Set <String> mentionedNames = new HashSet <> ();
        baseFile = "/interactions.txt";
        try (
            InputStream is = RunRandomForest.class.getResourceAsStream (baseFile);
            Reader r = new InputStreamReader (is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader (r);
        ) {
            String line = null;
            while ((line = br.readLine ()) != null) {
                if (line.length () == 0) { continue; }
                
                List <String> tokens = StreamUtils
                                     . whilst (StringTokenizer::hasMoreTokens, 
                                               StringTokenizer::nextToken, 
                                               new StringTokenizer (line))
                                     . collect (Collectors.toList ());
                if (tokens.get (0).equals (tokens.get (3))) { continue; }
                mentionedNames.addAll (asList (tokens.get (0), tokens.get (3)));
            }
        }
        
        Map <String, String> restrictedNames 
            = decodedNames.entrySet ().stream ().map (Pair::fromMapEntry)
            . filter  (p -> mentionedNames.contains (p.S))
            . collect (Collectors.toMap (p -> p.F, p -> p.S));
        Map <String, Integer> positions 
            = StreamUtils.zip (restrictedNames.keySet ().stream (), 
                               Stream.iterate (0, i -> i + 1), Pair::mp)
            . collect (Collectors.toMap (Pair::getF, Pair::getS));
        
        baseFile = "results/train.csv";
        try (
            PrintWriter pw = new PrintWriter (baseFile, StandardCharsets.UTF_8.name ());
        ) {
            StringJoiner sj = new StringJoiner (",");
            sj.add ("id");
            positions.entrySet ().stream ()
            . map     (Pair::fromMapEntry)
            . sorted  ((a, b) -> Integer.compare (positions.get (a.F), 
                                                  positions.get (b.F)))
            . map     (Pair::getF)
            . map     (restrictedNames::get)
            . forEach (sj::add);
            sj.add ("verdict");
            pw.println (sj.toString ());
            
            List <Pair <List <Pair <String, Double>>, EntityVerdict>> 
                selection = Stream.iterate (0, i -> i + 1)
                          . limit   (dataset.getSize ())
                          . map     (dataset::getEntityByIndex)
                          . map     (e -> Pair.mp (e.getGenesExpMap (), e.getVerdict ()))
                          . map     (p -> Pair.mp (p.F.entrySet ().stream ()
                                                      . map     (Pair::fromMapEntry)
                                                      . filter  (pp -> restrictedNames.containsKey (pp.F))
                                                      . sorted  ((a, b) -> Integer.compare (positions.get (a.F), 
                                                                                            positions.get (b.F)))
                                                      . map     (pp -> pp.applyF (restrictedNames::get))
                                                      . collect (Collectors.toList ()), 
                                                      p.S))
                          . filter  (p -> !EntityVerdict.NORMAL.equals (p.S))
                          . collect (Collectors.toList ());
            for (int i = 0; i < selection.size (); i++) {
                sj = new StringJoiner (",");
                sj.add ("" + (i + 1));
                selection.get (i).F.stream ()
                . map     (Pair::getS)
                . map     (Object::toString)
                . forEach (sj::add);
                sj.add ("" + selection.get (i).S.ordinal ());
                pw.println (sj.toString ());
            }
        }
    }
    
}
