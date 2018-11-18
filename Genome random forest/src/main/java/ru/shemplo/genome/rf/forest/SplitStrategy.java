package ru.shemplo.genome.rf.forest;

import java.util.List;

import ru.shemplo.genome.rf.data.NormalizedMatrix;
import ru.shemplo.genome.rf.data.SourceEntity;
import ru.shemplo.genome.rf.forest.DecisionTree.Layer;

public interface SplitStrategy {
    
    public void suggestFeature (int featureIndex, 
           Layer layer, NormalizedMatrix matrix);
    
    double [] getDifference (List <SourceEntity> entities, 
               NormalizedMatrix matrix, int featureIndex);
    
    public boolean isDifferenceSmall ();
    
    public double getBestSplit ();
    
    public int getBestFeature ();
    
    public void reset ();
    
}
