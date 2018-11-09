package ru.shemplo.genome.rf.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import ru.shemplo.snowball.stuctures.Pair;

public class SourceEntity {

    private final List <Pair <String, Double>> genesExpList;
    private final Map <String, Double> genesExpMap;
    
    @Getter @Setter private EntityVerdict verdict;
    @Getter @Setter private String geoAccess;
    @Getter private final String name;
    
    public SourceEntity (String name) {
        this.genesExpList = new ArrayList <> ();
        this.genesExpMap = new HashMap <> ();
        this.name = name;
    }
    
    public void addGeneExpression (Pair <String, Double> expression) {
        if (expression == null) { return; }
        
        if (genesExpMap.containsKey (expression.F)) { return; } // Gene already written
        genesExpMap.put (expression.F, expression.S);
        genesExpList.add (expression); 
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
        return genesExpList.size ();
    }
    
    public Set <String> getGenes () {
        return genesExpMap.keySet ();
    }
    
}
