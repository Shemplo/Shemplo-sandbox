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
import ru.shemplo.snowball.utils.StringManip;
import ru.shemplo.snowball.utils.fun.StreamUtils;

public class RunCsvConverter {
    
    private static final int N = 100;
    
    private static enum Action {
        NO_ACTION, TOP_N_MCMC, TOP_N_PVAL, RANDOM_N
    }
    
    private static Action action = Action.TOP_N_MCMC;
    
    public static void main (String ... args) throws Exception { 
        Path path = Paths.get ("GSE3189_series_matrix.txt");
        SourceDataset dataset = null;
        try (                
            InputStream is = Files.newInputStream (path);
        ) {
            dataset = new DataParser ().parse (is);
        }
        
        final Map <String, String> decodedNames = new HashMap <> ();
        final Map <String, Double> pvalues = new HashMap <> ();
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
                final String id = tokens.get (2);
                decodedNames.put (id, tokens.get (4)); 
                //                ID / Gene.symbol
                tokens = Arrays.asList (line.split (","));
                String value = tokens.get (tokens.size () - 3);
                pvalues.put (id, Double.parseDouble (value));
                //           ID / pval
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
        Map <String, String> reverse = restrictedNames.entrySet ().stream ()
                                     . map     (Pair::fromMapEntry)
                                     . collect (Collectors.toMap (Pair::getS, Pair::getF));
        
        if (action == Action.TOP_N_MCMC) {
            Map <String, String> top100 = new HashMap <> ();
            path = Paths.get ("temp", "freqs.csv");
            try (
                BufferedReader br = Files.newBufferedReader (path);
            ) {
                br.readLine (); // skip headers
                
                String line = null; int counter = 0;
                while ((line = StringManip.fetchNonEmptyLine (br)) != null
                        && counter < N) {
                    final String [] vals = line.split (",");
                    vals [0] = vals [0].replace ("\"", "");
                    
                    top100.put (reverse.get (vals [0]), vals [0]);
                    counter += 1;
                }
            }
            
            restrictedNames.clear ();
            restrictedNames.putAll (top100);
        } else if (action == Action.TOP_N_PVAL) {
            Map <String, String> top100 = pvalues.entrySet ().stream ()
                                        . map     (Pair::fromMapEntry)
                                        . sorted  ((a, b) -> Double.compare (a.S, b.S))
                                        . limit   (N)
                                        . map     (p -> p.applyS (__ -> decodedNames.get (p.F)))
                                        . collect (Collectors.toMap (Pair::getF, Pair::getS));
            restrictedNames.clear ();
            restrictedNames.putAll (top100);
        } else if (action == Action.RANDOM_N) {
            List <String> ids = new ArrayList <> (decodedNames.keySet ());
            Collections.shuffle (ids);
            restrictedNames.clear ();
            
            ids.stream ().limit (N).forEach (id -> {
                restrictedNames.put (id, decodedNames.get (id));
            });
        }
        
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
