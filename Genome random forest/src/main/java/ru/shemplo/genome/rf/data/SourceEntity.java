package ru.shemplo.genome.rf.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import ru.shemplo.snowball.stuctures.Pair;

public class SourceEntity {

    @Getter private final Map <String, Double> genesExpMap;
    private List <Pair <String, Double>> genesExpList;
    
    @Getter @Setter private EntityVerdict verdict;
    @Getter @Setter private String geoAccess;
    @Getter private final String name;
    
    public SourceEntity (String name) {
        this.genesExpList = new ArrayList <> ();
        this.genesExpMap = new HashMap <> ();
        this.geoAccess = this.name = name;
    }
    
    public void addGeneExpression (Pair <String, Double> expression) {
        if (expression == null) { return; }
        
        if (genesExpMap.containsKey (expression.F)) { return; } // Gene already written
        genesExpMap.put (expression.F, expression.S);
        genesExpList.add (expression); 
    }
    
    public void restrictGenes (Map <String, String> decoded) {
        new HashSet <> (genesExpMap.keySet ()).stream ()
          . filter (g -> !decoded.containsKey (g))
          . forEach (genesExpMap::remove);
        
        genesExpList = genesExpList.stream ()
                . filter (p -> genesExpMap.containsKey (p.F))
                . map (p -> Pair.mp (p.F + " (" + decoded.get (p.F) + ")", p.S))
                . collect (Collectors.toList ());
        
        decoded.keySet ().stream ()
               .map (k -> Pair.mp (k, genesExpMap.get (k)))
               .peek    (p -> genesExpMap.remove (p.F))
               .map     (p -> Pair.mp (p.F + " (" + decoded.get (p.F) + ")", p.S))
               .forEach (p -> genesExpMap.put (p.F, p.S));
    }
    
    public void addGeneExpression (String gene, Double value) {
        this.addGeneExpression (Pair.mp (gene, value));
    }
    
    public Pair <String, Double> getExpressionByIndex (int index) {
        return genesExpList.get (index);
    }
    
    public double getExpressionByGene (String gene) {
        return genesExpMap.get (gene);
    }
    
    public int getNumberOfExpressions () {
        return genesExpMap.size ();
    }
    
    public Set <String> getGenes () {
        return genesExpMap.keySet ();
    }
    
}
