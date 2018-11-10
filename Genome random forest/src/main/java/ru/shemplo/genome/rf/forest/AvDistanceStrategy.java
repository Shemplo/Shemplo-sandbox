package ru.shemplo.genome.rf.forest;

import static java.lang.Math.*;

import java.util.List;

import ru.shemplo.genome.rf.data.EntityVerdict;
import ru.shemplo.genome.rf.data.NormalizedMatrix;
import ru.shemplo.genome.rf.data.SourceDataset;
import ru.shemplo.genome.rf.data.SourceEntity;
import ru.shemplo.genome.rf.forest.DecisionTree.Layer;

public class AvDistanceStrategy implements SplitStrategy {

    private double bestDistance = 0;
    private double minAverage = 0;
    private int bestIndex = -1;
    
    @Override
    public void suggestFeature (int featureIndex, 
            Layer layer, NormalizedMatrix matrix) {
        List <String> entities = layer.getEnities ();
        SourceDataset dataset = layer.getDataset ();
        double [][] mx = matrix.getMatrix ();
        
        double [] sums = {0.0d, 0.0d};
        int [] number = {0, 0};
        
        for (int j = 0; j < entities.size (); j++) {
            SourceEntity entity = dataset.getEntityByGeoAccess (entities.get (j));
            int part = EntityVerdict.NORMAL.equals (entity.getVerdict ()) ? 0 : 1;
            sums [part] += mx [featureIndex][j]; number [part] += 1;
        }
        
        for (int j = 0; j < sums.length; j++) { sums [j] /= number [j]; }
        double distance = Math.abs (sums [0] - sums [1]);
        
        if (distance > bestDistance) {
            minAverage = min (sums [0], sums [1]);
            bestIndex  = featureIndex;
            bestDistance = distance;
        }
    }

    @Override
    public double getBestSplit () {
        return minAverage + bestDistance / 2;
    }
    
    @Override
    public int getBestFeature () {
        return bestIndex;
    }

    @Override
    public void reset () {
        bestDistance = 0;
        bestIndex = -1;
        minAverage = 0;
    }

    @Override
    public boolean isDifferenceSmall () {
        return bestDistance < 1e-4;
    }

    
}
