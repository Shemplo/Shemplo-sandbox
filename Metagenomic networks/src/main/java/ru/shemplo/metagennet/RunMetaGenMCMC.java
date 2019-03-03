package ru.shemplo.metagennet;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import ru.shemplo.metagennet.mcmc.GraphHolder;
import ru.shemplo.metagennet.mcmc.MCMCSingleRunHolder;
import ru.shemplo.snowball.utils.StringManip;

public class RunMetaGenMCMC {
    
    public static final Random RANDOM = new Random ();
    
    public static void main (String ... args) throws IOException {
        GraphHolder mcmc = new GraphHolder (readMatrix (GraphGenerator.GEN_FILE));
        MCMCSingleRunHolder singleRun = mcmc.makeSingleRun (250);
        singleRun.doAllIterations ();
    }
    
    private static List <List <Double>> readMatrix (File file) throws IOException {
        List <List <Double>> matrix = new ArrayList <> ();
        try (
            InputStream is = new FileInputStream (file);
            Reader r = new InputStreamReader (is);
            BufferedReader br = new BufferedReader (r);
        ) {
            String line = null;
            
            while ((line = StringManip.fetchNonEmptyLine (br)) != null) {
                matrix.add (
                    Arrays.asList (line.split (";")).stream ()
                    . map     (Double::parseDouble)
                    . collect (Collectors.toList ())
                );
            }
        }
        
        return matrix;
    }
    
}
