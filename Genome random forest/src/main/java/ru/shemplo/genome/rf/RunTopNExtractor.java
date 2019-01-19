package ru.shemplo.genome.rf;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import ru.shemplo.genome.rf.RunCsvConverter.Action;
import ru.shemplo.genome.rf.data.EntityVerdict;
import ru.shemplo.genome.rf.data.SourceDataset;
import ru.shemplo.genome.rf.data.SourceEntity;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.stuctures.Trio;
import ru.shemplo.snowball.utils.StringManip;

public class RunTopNExtractor {
    
    public static Action action = Action.NO_ACTION;
    public static int    N      = 20;
    
    public static void makeTrainSet (SourceDataset dataset) throws Exception {
        final List <Pair <String, Integer>> people = new ArrayList <> ();
        
        for (int i = 0; i < dataset.getSize (); i++) {
            SourceEntity entity = dataset.getEntityByIndex (i);
            if (entity.getVerdict () == EntityVerdict.NORMAL) {
                continue;
            }
            
            people.add (Pair.mp (entity.getGeoAccess (), 
                    entity.getVerdict ().ordinal ()));
        }
        
        switch (action) {
            case TOP_N_MCMC: makeMCMCTrainSet (dataset, people); break;
            case TOP_N_PVAL: makePValTrainSet (dataset, people); break;
            case NO_ACTION : writeMatrixToFile (dataset, people, new ArrayList <> ()); break;
            
            default: break;
        }
    }
    
    private static void makeMCMCTrainSet (SourceDataset dataset, 
            List <Pair <String, Integer>> people) throws Exception {
        List <Trio <String, String, Double>> genes = new ArrayList <> ();
        //filterPeople (people);
        
        Path path = Paths.get ("temp", "freqs.csv");
        try (
            BufferedReader br = Files.newBufferedReader (path);
        ) {
            br.readLine (); // Header line
            
            String line = null;
            while ((line = StringManip.fetchNonEmptyLine (br)) != null) {
                final String tokens [] = line.split (";");
                
                final double value = Double.parseDouble (tokens [2]);
                genes.add (Trio.mt (tokens [0], tokens [1], value));
            }
        }
        
        genes.sort ((a, b) -> -Double.compare (a.T, b.T));
        writeMatrixToFile (dataset, people, genes);
    }
    
    private static void makePValTrainSet (SourceDataset dataset, 
            List <Pair <String, Integer>> people) throws Exception {
        List <Trio <String, String, Double>> genes = new ArrayList <> ();
        //filterPeople (people);
        
        Path path = Paths.get ("temp", "pvals.csv");
        try (
            BufferedReader br = Files.newBufferedReader (path);
        ) {
            br.readLine (); // Header line
            
            String line = null;
            while ((line = StringManip.fetchNonEmptyLine (br)) != null) {
                final String tokens [] = line.split (";");
                
                final double value = Double.parseDouble (tokens [2]);
                genes.add (Trio.mt (tokens [0], tokens [1], value));
            }
        }
        
        genes.sort ((a, b) -> Double.compare (a.T, b.T));
        writeMatrixToFile (dataset, people, genes);
    }
    
    @SuppressWarnings ("unused")
    private static void filterPeople (List <Pair <String, Integer>> people) 
            throws Exception {
        Set <String> keep = new HashSet <> ();
        
        Path path = Paths.get ("temp", "trainset.csv");
        try (
            BufferedReader br = Files.newBufferedReader (path);
        ) {
            br.readLine (); // Header line
            
            String line = null;
            while ((line = StringManip.fetchNonEmptyLine (br)) != null) {
                final String tokens [] = line.split (";");
                keep.add (tokens [0]);
            }
        }
        
        List <Pair <String, Integer>> tmp = people.stream ()
                                          . filter (p -> keep.contains (p.F))
                                          . collect (Collectors.toList ());
        people.clear (); people.addAll (tmp);
    }
    
    private static void writeMatrixToFile (SourceDataset dataset, List <Pair <String, Integer>> people, 
            List <Trio <String, String, Double>> genes) throws Exception {
        Path path = Paths.get ("temp", "dataset.csv");
        try (
            PrintWriter pw = new PrintWriter (path.toFile ());
        ) {
            StringJoiner sj = new StringJoiner (";").add ("object");
            genes.stream ().limit (N)
            . map     (t -> t.S)
            . forEach (sj::add);
            sj.add ("verdict");
            
            pw.println (sj.toString ());
            
            Collections.shuffle (people, new Random (N));
            for (Pair <String, Integer> man : people) {
                sj = new StringJoiner (";").add (man.F);
                
                SourceEntity entity = dataset.getEntityByGeoAccess (man.F);
                genes.stream ().limit (N)
                . map     (t -> t.F)
                . map     (entity::getExpressionByGene)
                . map     (Object::toString)
                . forEach (sj::add);
                sj.add ("" + man.S);
                
                pw.println (sj.toString ());
            }
        }
    }
    
}
