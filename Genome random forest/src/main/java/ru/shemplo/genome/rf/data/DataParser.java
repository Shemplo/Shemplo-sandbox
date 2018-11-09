package ru.shemplo.genome.rf.data;

import static ru.shemplo.genome.rf.data.EntityVerdict.*;
import static ru.shemplo.snowball.utils.StringManip.*;
import static ru.shemplo.snowball.utils.fun.StreamUtils.*;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.nio.charset.StandardCharsets;

import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.fun.PoolOfHandlers;

public class DataParser {
    
    private final PoolOfHandlers <List <String>> 
        pool = PoolOfHandlers.<List <String>> build ()
             . addConsumer ((lst, dset) -> {
                 if ("!Series_title".equals (lst.get (0))) {
                     SourceDataset ds = (SourceDataset) dset;
                     StringJoiner sj = new StringJoiner (" ");
                     lst.stream ().skip (1)
                        .map (t -> t.replace ("\"", ""))
                        .forEach (sj::add);
                     ds.setTitle (sj.toString ());
                 }
             })
             . addConsumer ((lst, dset) -> {
                 if ("!Sample_title".equals (lst.get (0))) {
                     SourceDataset ds = (SourceDataset) dset;
                     for (int i = 1; i < lst.size (); i++) {
                         String name = lst.get (i).replace ("\"", "");
                         ds.addEntity (new SourceEntity (name));
                     }
                 }
             })
             . addConsumer ((lst, dset) -> {
                 if ("!Sample_geo_accession".equals (lst.get (0))) {
                     SourceDataset ds = (SourceDataset) dset;
                     for (int i = 1; i < lst.size (); i++) {
                         String access = lst.get (i).replace ("\"", "");
                         ds.updateEntity (i - 1, e -> e.setGeoAccess (access));
                     }
                 }
             })
             . addConsumer ((lst, dset) -> {
                 if ("!Sample_characteristics_ch1".equals (lst.get (0))) {
                     SourceDataset ds = (SourceDataset) dset;
                     for (int i = 1; i < lst.size (); i++) {
                         EntityVerdict verdict = string2Verdict (lst.get (i));
                         ds.updateEntity (i - 1, e -> e.setVerdict (verdict));
                     }
                 }
             })
             . done ();
    
    public SourceDataset parse (InputStream is) throws IOException {
        System.out.println ("[] Parsing input file ...");
        
        SourceDataset dataset = new SourceDataset ();
        
        try (
            Reader r = new InputStreamReader (is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader (r);
        ) {
            String line = null;
            while ((line = br.readLine ()) != null) {
                if (line.trim ().length () == 0) { continue; }
                List <String> tokens = splitOnTokens (line);
                if ("\"ID_REF\"".equals (tokens.get (0))) {
                    System.out.println ("[] Parsing expression matrix ...");
                    
                    List <String> order = tokens.stream ().skip (1)
                                                .map (t -> t.replace ("\"", ""))
                                                .collect (Collectors.toList ());
                    while ((line = br.readLine ()) != null) {
                        if (line.trim ().length () == 0) { continue; }
                        
                        tokens = splitOnTokens (line);
                        if (line.charAt (0) != '"') {
                            System.out.println ("[] Matrix parsed");
                            break; 
                        }
                        
                        String gene = tokens.get (0).replace ("\"", "");
                        zip (order .stream ().map (dataset::getEntityByGeoAccess),
                             tokens.stream ().skip (1).map (Double::parseDouble), 
                             Pair::mp)
                          . forEach (p -> p.F.addGeneExpression (gene, p.S));
                    }
                }
                
                pool.handle (tokens, dataset);
            }
        }
        
        return dataset;
    }
    
}
