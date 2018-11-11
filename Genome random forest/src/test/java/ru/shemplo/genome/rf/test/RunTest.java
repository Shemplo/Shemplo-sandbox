package ru.shemplo.genome.rf.test;

import static java.lang.Character.*;
import static java.nio.charset.StandardCharsets.*;
import static ru.shemplo.snowball.utils.fun.StreamUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import ru.shemplo.genome.rf.data.EntityVerdict;
import ru.shemplo.genome.rf.data.NormalizedMatrix;
import ru.shemplo.genome.rf.data.SourceDataset;
import ru.shemplo.genome.rf.data.SourceEntity;
import ru.shemplo.genome.rf.forest.AvDistanceStrategy;
import ru.shemplo.genome.rf.forest.RandomForest;
import ru.shemplo.genome.rf.forest.SplitStrategy;
import ru.shemplo.snowball.stuctures.Pair;

public class RunTest {
    
    public static void main (String ... args) throws Exception {
        List <Pair <List <Double>, Integer>> 
            trainRows = new ArrayList <> ();
        
        try (
            InputStream train = openResource ("/train.csv");
            Reader r = new InputStreamReader (train, UTF_8);
            BufferedReader br = new BufferedReader (r);
        ) {
            br.readLine (); // Line with description of input data
            
            String line = null;
            while ((line = br.readLine ()) != null) {
                if (line.length () == 0) { continue; }
                List <Double> tokens = Arrays.asList (line.split (",")).stream ().skip (1)
                                     . map (RunTest::convertLetterToDigit)
                                     . map (Double::parseDouble)
                                     . collect (Collectors.toList ());
                int size = tokens.size ();
                trainRows.add (Pair.mp (tokens.subList (0, size - 1), 
                                        tokens.get (size - 1).intValue ()));
            }
        }
        
        trainRows = trainRows.stream ().filter (p -> p.S < 2)
                  . collect (Collectors.toList ());
        
        SourceDataset dataset = new SourceDataset ();
        zip (trainRows.stream (), Stream.iterate (0, i -> i + 1), Pair::mp).forEach (row -> {
            SourceEntity entity = new SourceEntity ("u" + row.S);
            
            zip (row.F.F.stream (), Stream.iterate (0, i -> i + 1), Pair::mp).forEach (fs -> {
                entity.addGeneExpression ("g" + fs.S, fs.F);
            });
            
            entity.setVerdict (EntityVerdict.values () [row.F.S * 2]);
            dataset.addEntity (entity);
        });
        
        NormalizedMatrix matrix = dataset.getNormalizedMatrix ();
        SplitStrategy strategy = new AvDistanceStrategy ();
        RandomForest forest = new RandomForest (matrix, strategy, dataset, 10, 301)
                            . train ();
        
        Map <Integer, List <Double>> testRows = new HashMap <> ();
        Map <Integer, Integer> testAnswers = new HashMap <> ();
        try (
            InputStream test = openResource ("/test.csv");
            Reader r = new InputStreamReader (test, UTF_8);
            BufferedReader br = new BufferedReader (r);
        ) {
            br.readLine (); // Line with description of input data
            
            String line = null;
            while ((line = br.readLine ()) != null) {
                if (line.length () == 0) { continue; }
                List <Double> tokens = Arrays.asList (line.split (",")).stream ()
                                     . map (RunTest::convertLetterToDigit)
                                     . map (Double::parseDouble)
                                     . collect (Collectors.toList ());
                int size = tokens.size ();
                testRows.put (tokens.get (0).intValue (), 
                              tokens.subList (1, size));
            }
        }
        
        try (
            InputStream test = openResource ("/answer.csv");
            Reader r = new InputStreamReader (test, UTF_8);
            BufferedReader br = new BufferedReader (r);
        ) {
            br.readLine (); // Line with description of input data
            
            String line = null;
            while ((line = br.readLine ()) != null) {
                if (line.length () == 0) { continue; }
                List <Double> tokens = Arrays.asList (line.split (",")).stream ()
                                     . map (RunTest::convertLetterToDigit)
                                     . map (Double::parseDouble)
                                     . collect (Collectors.toList ());
                testAnswers.put (tokens.get (0).intValue (), tokens.get (1).intValue ());
            }
        }
        
        AtomicInteger correct = new AtomicInteger (), total = new AtomicInteger ();
        zip (testRows.keySet ().stream (), Stream.iterate (0, i -> i + 1), Pair::mp)
          . forEach (row -> {
            SourceEntity entity = new SourceEntity ("testEntity" + row.S);
            int answer = testAnswers.get (row.F);
            if (answer >= 2) { return; }
            total.incrementAndGet ();
            
            entity.setVerdict (EntityVerdict.values () [answer * 2]);
            zip (testRows.get (row.F).stream (), Stream.iterate (0, i -> i + 1), Pair::mp).forEach (fs -> {
                entity.addGeneExpression ("g" + fs.S, fs.F);
            });
            
            EntityVerdict prediction = forest.predict (entity.getGenesExpMap ());
            if (prediction.equals (entity.getVerdict ())) {
                correct.incrementAndGet ();
            }
        });
        
        
        System.out.println ("Corrrect: " + correct.get () 
                         + " / " + total.get ());
        /*
        forest.makeProbabilities ().forEach ((k, v) -> {
            System.out.println (String.format (" - %12s %.8f", k, v));
        });
        */
        Map <String, Double> probs = forest.makeProbabilities ();
        probs.keySet ().stream ()
                       . map (k -> Pair.mp (k, probs.get (k)))
                       . sorted ((pa, pb) -> -Double.compare (pa.S, pb.S))
                       . map (p -> String.format (" - %12s %.12f", p.F, p.S))
                       . forEach (System.out::println);
        
    }
    
    private static String convertLetterToDigit (String token) {
        if (token.length () == 1) {
            char [] symbols = token.toCharArray ();
            int shift = getNumericValue ('A');
            if (Character.isLetter (symbols [0])) {
                int code = getNumericValue (symbols [0]);
                return "" + (code - shift);
            }
        }
        
        return token;
    }
    
    private static final InputStream openResource (String path) {
        return RunTest.class.getResourceAsStream (path);
    }
    
}
