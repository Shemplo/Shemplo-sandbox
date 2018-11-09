package ru.shemplo.genome.rf.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import ru.shemplo.snowball.stuctures.Pair;

public class SourceEntity {

    private final List <Pair <String, Double>> genesExpression;
    private final Set <String> genes;
    
    @Getter @Setter private EntityVerdict verdict;
    @Getter @Setter private String geoAccess;
    @Getter private final String name;
    
    public SourceEntity (String name) {
        this.genesExpression = new ArrayList <> ();
        this.genes = new HashSet <> ();
        this.name = name;
    }
    
    public void addGeneExpression (Pair <String, Double> expression) {
        if (expression == null) { return; }
        
        if (genes.contains (expression.F)) { return; } // Gene already written
        genesExpression.add (expression); genes.add (expression.F);
    }
    
    public void addGeneExpression (String gene, Double value) {
        this.addGeneExpression (Pair.mp (gene, value));
    }
    
    public int getNumberOfExpressions () {
        return genesExpression.size ();
    }
    
}
