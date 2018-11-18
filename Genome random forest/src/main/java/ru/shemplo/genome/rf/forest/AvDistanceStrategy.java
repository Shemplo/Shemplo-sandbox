package ru.shemplo.genome.rf.forest;

import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ru.shemplo.genome.rf.data.EntityVerdict;
import ru.shemplo.genome.rf.data.NormalizedMatrix;
import ru.shemplo.genome.rf.data.SourceDataset;
import ru.shemplo.genome.rf.data.SourceEntity;
import ru.shemplo.genome.rf.forest.DecisionTree.Layer;

public class AvDistanceStrategy implements SplitStrategy {

    protected final Random random = new Random ();
    protected double bestDistance = 0;
    protected double minAverage = 0;
    protected int bestIndex = -1;
    
    @Override
    public void suggestFeature (int featureIndex, 
            Layer layer, NormalizedMatrix matrix) {
        List <String> entities = layer.getEnities ();
        SourceDataset dataset = layer.getDataset ();
        
        List <SourceEntity> list = new ArrayList <> ();
        for (int j = 0; j < entities.size (); j++) {
            list.add (dataset.getEntityByGeoAccess (entities.get (j)));
        }
        
        double [] distances = getDifference (list, matrix, featureIndex);
        if (distances [0] > bestDistance || (distances [0] == bestDistance 
                                             && random.nextBoolean ())) {
            minAverage = min (distances [1], distances [2]);
            bestDistance = distances [0];
            bestIndex  = featureIndex;
        }
    }
    
    public double [] getDifference (List <SourceEntity> entities, 
                       NormalizedMatrix matrix, int featureIndex) {
        double [][] mx = matrix.getMatrix ();
        double [] sums = {0.0d, 0.0d};
        int [] number = {0, 0};
        
        for (int j = 0; j < entities.size (); j++) {
            SourceEntity entity = entities.get (j);
            int part = EntityVerdict.NORMAL.equals (entity.getVerdict ()) ? 0 : 1;
            sums [part] += mx [featureIndex][j]; number [part] += 1;
        }
        
        if (number [0] == 0 || number [1] == 0) { return new double [] {0, 0, 0}; }
        
        for (int j = 0; j < sums.length; j++) { sums [j] /= number [j]; }
        return new double [] {Math.abs (sums [0] - sums [1]), sums [0], sums [1]};
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
