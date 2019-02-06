package ru.shemplo.genome.rf;

import static java.util.Arrays.*;
import static ru.shemplo.genome.rf.external.PythonRunner.*;
import static ru.shemplo.genome.rf.external.RRunner.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import ru.shemplo.genome.rf.RunCsvConverter.Action;
import ru.shemplo.genome.rf.data.DataParser;
import ru.shemplo.genome.rf.data.SourceDataset;
import ru.shemplo.snowball.stuctures.Trio;
import ru.shemplo.snowball.utils.fun.StreamUtils;

public class RunIntersector {
    
    public static final Path RESULTS_FILE_PATH 
         = Paths.get ("results", "itersections.csv");
    
    public static final double P_VAL = 0.015;
    
    public static void main (String [] args) throws Exception {
        Locale.setDefault (Locale.ENGLISH);
        
        // Reading data about people and genes
        Path path = Paths.get ("GSE3189_series_matrix.txt");
        SourceDataset dataset = null;
        try (
            InputStream is = Files.newInputStream (path);
        ) {
            dataset = new DataParser ().parse (is);
        }

        List <Trio <String, String, Double []>> genes 
           = dataset.getAllGenes ().stream ()
           . map     (g -> Trio.mt (g, g, new Double [4]))
           . collect (Collectors.toList ());
        
        // Erasing previous file with results
        if (Files.exists (RESULTS_FILE_PATH)) {
            Files.delete (RESULTS_FILE_PATH);
        }
        
        // Preparing file for splitter
        RunTopNExtractor.action = Action.NO_ACTION;
        RunTopNExtractor.makeTrainSet (dataset);
        
        // Splitting data set to train and test sets (big test)
        int i = 3, estimators = 60;
        double testSize = 0.75;
        runPython (String.format ("python splitter.py %d %d %f", 
                                  i, estimators, testSize), true);
        System.out.println ("   Small train set done");
        
        Path trainsetFrom = Paths.get ("src/main/python/trainset.csv"),
             trainsetTo   = Paths.get ("src/main/r/trainset.csv");
        if (Files.exists (trainsetTo)) { Files.delete (trainsetTo); }
        Files.copy (trainsetFrom, trainsetTo);
        
        runR ("Rscript gse3189.r", true);
        System.out.println ("   Small MCMC and PVal done");
        Path freqsFrom = Paths.get ("src/main/r/freqs.csv"),
             freqsTo   = Paths.get ("temp/freqs.csv");
        if (Files.exists (freqsTo)) {
            Files.delete (freqsTo);
        }
        Files.copy (freqsFrom, freqsTo);
        
        Path pvalsFrom = Paths.get ("src/main/r/pvals.csv"),
             pvalsTo   = Paths.get ("temp/pvals.csv");
        if (Files.exists (pvalsTo)) {
            Files.delete (pvalsTo);
        }
        Files.copy (pvalsFrom, pvalsTo);
        
        Map <String, Double> pval1Genes = RunTopNExtractor.getSortedByPValue ().stream ()
                                        . filter  (g -> g.T <= P_VAL)
                                        . collect (Collectors.toMap (Trio::getF, Trio::getT)),
                             mcmc1Genes = RunTopNExtractor.getSortedAfterMCMC ().stream ()
                                        . map (g -> g.applyT (v -> v == 0 ? -1 : 1 / v))
                                        . collect (Collectors.toMap (Trio::getF, Trio::getT));
        genes.forEach (g -> {
            g.T [0] = pval1Genes.get (g.F);
            g.T [1] = mcmc1Genes.get (g.F);
        });
        
        // Splitting data set to train and test sets (big train)
        i = 3; estimators = 60;
        testSize = 0.05;
        runPython (String.format ("python splitter.py %d %d %f", 
                                  i, estimators, testSize), true);
        System.out.println ("   Big train set done");
        
        trainsetFrom = Paths.get ("src/main/python/trainset.csv");
        trainsetTo   = Paths.get ("src/main/r/trainset.csv");
        if (Files.exists (trainsetTo)) { Files.delete (trainsetTo); }
        Files.copy (trainsetFrom, trainsetTo);
        
        runR ("Rscript gse3189.r", true);
        System.out.println ("   Big MCMC and PVal done");
        freqsFrom = Paths.get ("src/main/r/freqs.csv");
        freqsTo   = Paths.get ("temp/freqs.csv");
        if (Files.exists (freqsTo)) {
            Files.delete (freqsTo);
        }
        Files.copy (freqsFrom, freqsTo);
        
        pvalsFrom = Paths.get ("src/main/r/pvals.csv");
        pvalsTo   = Paths.get ("temp/pvals.csv");
        if (Files.exists (pvalsTo)) {
            Files.delete (pvalsTo);
        }
        Files.copy (pvalsFrom, pvalsTo);
        
        Map <String, Double> pval2Genes = RunTopNExtractor.getSortedByPValue ().stream ()
                                        . filter  (g -> g.T <= P_VAL)
                                        . collect (Collectors.toMap (Trio::getF, Trio::getT)),
                             mcmc2Genes = RunTopNExtractor.getSortedAfterMCMC ().stream ()
                                        . map (g -> g.applyT (v -> v == 0 ? -1 : 1 / v))
                                        . collect (Collectors.toMap (Trio::getF, Trio::getT));
        genes.forEach (g -> {
            g.T [2] = pval2Genes.get (g.F);
            g.T [3] = mcmc2Genes.get (g.F);
        });
        
        Map <String, String> decodedNames = getDecodedNames ();
        Set <String> vetrices = readGraphGenes ();
        genes = genes.stream ()
              . map     (g -> g.applyS (decodedNames::get))
              . filter  (g -> g.S != null)
              . filter  (g -> vetrices.contains (g.S))
              . collect (Collectors.toList ());
        
        path = Paths.get ("results", "sets.csv");
        try (
            PrintWriter pw = new PrintWriter (path.toFile ());
        ) {
            pw.println ("Gene id;Gene name;Small pval;Small mcmc;Big pval;Big mcmc");
            genes.stream ().forEach (g -> {
                StringJoiner sj = new StringJoiner (";");
                sj.add (g.F); sj.add (g.S);
                for (int j = 0; j < g.T.length; j++) {
                    sj.add ("" + (g.T [j] != null && g.T [j] != -1 ? g.T [j] : ""));
                }
                
                pw.println (sj.toString ());
            });
        }
        
        System.out.println ("   Saved in file `sets.csv`");
    }
    
    public static Map <String, String> getDecodedNames () throws IOException {
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
        
        return decodedNames;
    }
    
    private static Set <String> readGraphGenes () throws IOException {
        Set <String> vertices = new HashSet <> ();
        String baseFile = "/interactions.txt";
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
                vertices.addAll (asList (tokens.get (0), tokens.get (3)));
            }
        }
        
        return vertices;
    }
    
}
