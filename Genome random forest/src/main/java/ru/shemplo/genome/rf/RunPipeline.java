package ru.shemplo.genome.rf;

import static ru.shemplo.genome.rf.RunCsvConverter.*;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.shemplo.genome.rf.RunCsvConverter.Action;

public class RunPipeline {
    
    public static final int [] ESTIMATORS = {60, 90, 150, 250};
    
    public static final int RUNS = 100;
    
    public static void main (String ... args) throws Exception {
        List <Double> results = new ArrayList <> ();
        Locale.setDefault (Locale.ENGLISH);
        
        for (int i = 0; i < RUNS * 2; i++) {
            RunCsvConverter.action = Action.values () [1 + i % 2];
            RunCsvConverter.N      = 20;
            RunCsvConverter.main ();
            
            System.out.println (String.format ("Run (act: %s, n: %d) is prepared", 
                                      RunCsvConverter.action, RunCsvConverter.N));
            
            for (int estimators : ESTIMATORS) {                
                String command = String.format ("cmd /C bash -c \""
                        + "cd src/main/python; "
                        + "python gse3189.py %d %d\"", 
                        i, estimators);
                Process process = Runtime.getRuntime ().exec (command);
                process.waitFor ();
                
                System.out.println (String.format ("%3d Random forest built (est: %d)", 
                                    i, estimators));
                
                Path impsFrom = Paths.get (String.format ("src/main/python/sklearn-%d.txt", 
                                           estimators)),
                     impsTo   = Paths.get (String.format ("temp/sklearn-%d.txt", 
                                           estimators));
                if (Files.exists (impsTo)) {
                    Files.delete (impsTo);
                }
                Files.copy (impsFrom, impsTo);
            }
            
            Path trainsetFrom = Paths.get ("src/main/python/trainset.csv"),
                 trainsetTo   = Paths.get ("src/main/r/trainset.csv");
            if (Files.exists (trainsetTo)) {
                Files.delete (trainsetTo);
            }
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
            
            double result = RunCsvComposer.mainR (new String [] {"" + i, action.name ()});
            System.out.println (String.format ("%3d Composition of results done", i));
            results.add (result);
        }
        
        Path path = Paths.get ("results/MCMCvsPVAL.csv");
        try (
            PrintWriter pw = new PrintWriter (path.toFile ());
        ) {
            pw.println ("iteration;by MCMC;by P-value");
            
            for (int i = 0; i < RUNS; i++) {
                pw.println (String.format ("%d;%f;%f", i, results.get (i * 2), 
                                                    results.get (i * 2 + 1)));
            }
        }
        
        System.out.println (String.format ("Comparing table written"));
    }
    
}
