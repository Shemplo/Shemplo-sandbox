package ru.shemplo.genome.rf.forest;

import static java.lang.Math.*;

import java.util.List;

import ru.shemplo.genome.rf.data.EntityVerdict;
import ru.shemplo.genome.rf.data.NormalizedMatrix;
import ru.shemplo.genome.rf.data.SourceDataset;
import ru.shemplo.genome.rf.data.SourceEntity;
import ru.shemplo.genome.rf.forest.DecisionTree.Layer;

public class AvDeviationStrategy extends AvDistanceStrategy {
    
    protected double bestDeviation = Double.MAX_VALUE;
    
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
        double distance = abs (sums [0] - sums [1]),
               tmp = min (sums [0], sums [1]);
        sums [0] = sums [1] = 0.0d;
        
        for (int j = 0; j < entities.size (); j++) {
            SourceEntity entity = dataset.getEntityByGeoAccess (entities.get (j));
            int part = EntityVerdict.NORMAL.equals (entity.getVerdict ()) ? 0 : 1;
            double delta = sums [part] - mx [featureIndex] [j];
            sums [part] = max (sums [part], delta * delta);
        }
        
        double devivation = Math.max (sums [0], sums [1]);
        if (devivation < bestDeviation 
                || (devivation == bestDeviation 
                    && random.nextBoolean ())) {
            bestDeviation = devivation;
            bestIndex  = featureIndex;
            bestDistance = distance;
            minAverage = tmp;
        }
    }
    
    @Override
    public void reset () {
        super.reset ();
        
        bestDeviation = Double.MAX_VALUE;
    }
    
    @Override
    public boolean isDifferenceSmall () {
        return bestDistance > 0.5;
    }
    
}
