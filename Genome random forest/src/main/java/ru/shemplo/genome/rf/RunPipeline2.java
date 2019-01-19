package ru.shemplo.genome.rf;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import ru.shemplo.genome.rf.RunCsvConverter.Action;
import ru.shemplo.genome.rf.data.DataParser;
import ru.shemplo.genome.rf.data.SourceDataset;

public class RunPipeline2 {
    
    public static final Path RESULTS_FILE_PATH = Paths.get ("results", "MCMCvsPVal.csv");
    
    public static final int [] ESTIMATORS = {40};
    
    public static final int RUNS = 1000;
    
    public static void main (String ... args) throws Exception { 
        Locale.setDefault (Locale.ENGLISH);
        
        Path path = Paths.get ("GSE3189_series_matrix.txt");
        SourceDataset dataset = null;
        try (
            InputStream is = Files.newInputStream (path);
        ) {
            dataset = new DataParser ().parse (is);
        }
        
        if (Files.exists (RESULTS_FILE_PATH)) {
            Files.delete (RESULTS_FILE_PATH);
        }
        
        RunTopNExtractor.action = Action.NO_ACTION;
        RunTopNExtractor.makeTrainSet (dataset);
        
        for (int i = 0; i < RUNS; i++) {
            for (int estimators : ESTIMATORS) {
                runPython (String.format ("python splitter.py %d %d", i, estimators));
                System.out.println (String.format ("%3d Set splitted (est: %d)", 
                                    i, estimators));
                
                Path trainsetFrom = Paths.get ("src/main/python/trainset.csv"),
                     trainsetTo   = Paths.get ("src/main/r/trainset.csv");
                if (Files.exists (trainsetTo)) { Files.delete (trainsetTo); }
                Files.copy (trainsetFrom, trainsetTo);
                
                String command = String.format ("cmd /C cd src/main/r && Rscript gse3189.r");
                Process process = Runtime.getRuntime ().exec (command);
                process.waitFor ();
                
                System.out.println (String.format ("%3d MCMC done", i));
                
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
                
                System.out.println (String.format ("%3d MCMC files transfered", i));
                
                // Score of classification with MCMC genes
                
                RunTopNExtractor.action = Action.TOP_N_MCMC;
                RunTopNExtractor.makeTrainSet (dataset);
                
                runPython (String.format ("python gse3189.py %d %d", i, estimators));
                double scoresM = readTreeScore (estimators);
                
                System.out.println (String.format ("%3d MCMC score %f", i, scoresM));
                
                // Score of classification with PVal genes
                
                RunTopNExtractor.action = Action.TOP_N_PVAL;
                RunTopNExtractor.makeTrainSet (dataset);
                
                runPython (String.format ("python gse3189.py %d %d", i, estimators));
                double scoresP = readTreeScore (estimators);
                
                System.out.println (String.format ("%3d PVal score %f", i, scoresP));
                
                // Flushing to file to save progress
                
                appendToFile (i, estimators, scoresM, scoresP);
                
                System.out.println (String.format ("%3d Scores appended to file", i));
            }
            
            System.out.println (String.format ("[] Iteration %d finished", i));
        }
    }
    
    private static void runPython (String command) throws Exception {
        command = String.format ("cmd /C bash -c \""
                + "cd src/main/python; %s\"", command);
        Process process = Runtime.getRuntime ().exec (command);
        
        /*
        try (
            InputStream is = process.getInputStream ();
            Reader r = new InputStreamReader (is);
            BufferedReader br = new BufferedReader (r);
        ) {
            String line = null;
            while ((line = br.readLine ()) != null) {
                System.out.println (line);
            }
        }
        
        try (
            InputStream is = process.getErrorStream ();
            Reader r = new InputStreamReader (is);
            BufferedReader br = new BufferedReader (r);
        ) {
            String line = null;
            while ((line = br.readLine ()) != null) {
                System.out.println (line);
            }
        }
        */
        
        process.waitFor ();
    }
    
    private static double readTreeScore (int estimators) throws Exception {
        String file = String.format ("sklearn-%d.txt", estimators);
        final Path path = Paths.get ("temp", file);
        try (
            BufferedReader br = Files.newBufferedReader (path);
        ) {
            return Double.parseDouble (br.readLine ());
        }
    }
    
    private static void appendToFile (int iteration, int estimators, double mcmc, double pval) throws Exception {
        if (!Files.exists (RESULTS_FILE_PATH)) { Files.createFile (RESULTS_FILE_PATH); }
        final File file = RESULTS_FILE_PATH.toFile ();
        
        try (
            OutputStream os = new FileOutputStream (file, true);
            PrintWriter pw = new PrintWriter (os);
        ) {
            pw.append (String.format ("%d;%f;%f\n", 
                       iteration + 1, mcmc, pval));
        }
    }
    
}
