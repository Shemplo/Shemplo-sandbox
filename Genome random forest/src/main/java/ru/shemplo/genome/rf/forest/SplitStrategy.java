package ru.shemplo.genome.rf.forest;

import ru.shemplo.genome.rf.data.NormalizedMatrix;
import ru.shemplo.genome.rf.forest.DecisionTree.Layer;

public interface SplitStrategy {
    
    public void suggestFeature (int featureIndex, 
           Layer layer, NormalizedMatrix matrix);
    
    public boolean isDifferenceSmall ();
    
    public double getBestSplit ();
    
    public int getBestFeature ();
    
    public void reset ();
    
}
