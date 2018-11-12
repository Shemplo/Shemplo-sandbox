package ru.shemplo.genome.rf.forest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;
import ru.shemplo.genome.rf.data.EntityVerdict;
import ru.shemplo.genome.rf.data.NormalizedMatrix;
import ru.shemplo.genome.rf.data.SourceDataset;

@RequiredArgsConstructor
public class RandomForest {
    
    private List <DecisionTree> trees = new ArrayList <> ();
    
    private final NormalizedMatrix matrix;
    private final SplitStrategy strategy;
    private final SourceDataset dataset;
    private final int parts, size;
    
    public RandomForest train () {
        System.out.println ("[] Training trees ...");
        
        for (int i = 0; i < size; i++) {
            if (i % 100 == 0) {
                String format = " - tree %3d of %-3d";
                System.out.println (String.format (format, i + 1, size));
            }
            
            int part = i % parts, height = matrix.getNumberOfGenes () / parts;
            int start = height * part, end = height * (part + 1);
            int width = matrix.getNumberOfEntities ();
            
            NormalizedMatrix tmp = matrix.getSubMatrix (start, end, 0, width);
            trees.add (new DecisionTree (dataset, tmp, strategy));
        }
        
        return this;
    }
    
    public EntityVerdict predict (Map <String, Double> input) {
        int normal = 0;
        for (DecisionTree tree : trees) {
            if (EntityVerdict.NORMAL.equals (tree.predict (input))) {
                normal += 1;
            }
        }
        
        return normal >= trees.size () / 2
             ? EntityVerdict.NORMAL
             : EntityVerdict.MELANOMA;
    }
    
    public Map <String, Double> makeProbabilities () {
        int depth = DecisionTree.getMaxTreeDepth ();
        Map <String, Double> genes = new HashMap <> ();
        AtomicInteger layers = new AtomicInteger ();
        
        for (int i = 0; i < depth; i++) {
            final int layer = i;
            genes.clear ();
            layers.set (0);
            
            trees.forEach (t -> {
                Map <String, Integer> onLayer = t.getGenesOnDepth (layer);
                layers.addAndGet (onLayer.remove (DecisionTree.nameOfMeta));
                onLayer.keySet ().forEach (k -> {
                    genes.putIfAbsent (k, 0.0d);
                    
                    Double value = onLayer.get (k).doubleValue ();
                    genes.merge (k, value, Double::sum);
                });
            });
            
            genes.keySet ().forEach (k -> {
                genes.compute (k, (__, v) -> v / layers.get ());
            });
            
            trees.forEach (t -> t.propagateProbabilities (layer, 1.0 / trees.size (), genes));
        }
        
        genes.clear ();
        trees.forEach (t -> {
            Map <String, Double> inTree = t.getAllProbabilities ();
            inTree.keySet ().forEach (k -> {
                genes.putIfAbsent (k, 0.0d);
                
                Double value = inTree.get (k).doubleValue ();
                genes.merge (k, value, Double::sum);
            });
        });
        
        /*
        AtomicReference <Double> norm = new AtomicReference <> (0.0d);
        genes.values ().forEach (v -> norm.updateAndGet (r -> r + v));
        genes.keySet ().forEach (k -> {
            genes.compute (k, (__, v) -> v * (1 / norm.get ()));
        });
        */
        
        return genes;
    }
    
}
