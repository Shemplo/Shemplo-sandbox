package ru.shemplo.genome.rf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ru.shemplo.genome.rf.RunCsvConverter.Action;

public class RunPipeline {
    
    public static final int [] ESTIMATORS = {60, 90, 150, 250};
    
    public static void main (String ... args) throws Exception {
        for (int i = 0; i < 2; i++) {
            RunCsvConverter.action = Action.values () [(i % 2 + 1) * 2 - 1];
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
            
            RunCsvComposer.main (new String [] {"" + i, RunCsvConverter.action.name ()});
            System.out.println (String.format ("%3d Composition of results done", i));
        }
    }
    
}
