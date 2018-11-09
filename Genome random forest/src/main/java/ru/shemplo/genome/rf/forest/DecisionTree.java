package ru.shemplo.genome.rf.forest;

import static ru.shemplo.genome.rf.data.EntityVerdict.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.shemplo.genome.rf.data.EntityVerdict;
import ru.shemplo.genome.rf.data.NormalizedMatrix;
import ru.shemplo.genome.rf.data.SourceDataset;
import ru.shemplo.genome.rf.data.SourceEntity;

public class DecisionTree {
    
    private final NormalizedMatrix matrix;
    private final SourceDataset dataset;
    private Layer root;
    
    public DecisionTree (SourceDataset dataset, NormalizedMatrix matrix) {
        this.dataset = dataset; this.matrix = matrix;
        this.root = Layer.builder ().usedGenes (new HashSet <> ())
                  . enities (matrix.getEntitiesName ())
                  . dataset (dataset).depth (0).build ();
        split (root);
    }
    
    @Getter @Setter @Builder
    public static class Layer {

        @NonNull private List <String> enities;
        @NonNull private SourceDataset dataset;
        private Layer left, right;
        
        private Predicate <Double> splitter;
        private Set <String> usedGenes;
        private int depth;
        
        public boolean isSplittedEnough () {
            AtomicInteger zero = new AtomicInteger ();
            enities.forEach (e -> {
                SourceEntity entity = dataset.getEntityByGeoAccess (e);
                EntityVerdict verdict = entity.getVerdict ();
                zero.addAndGet (NORMAL.equals (verdict) ? 1 : 0);
            });
            
            double zeros = zero.get () * 1.0;
            return (zeros / enities.size () >= 0.95) // 95% of entities are the same
                || depth >= 9;
        }
        
    }
    
    private void split (Layer layer) {
        List <String> entities = layer.getEnities (),
                      genes = matrix.getGenesName ();
        double [][] matrix = this.matrix.getMatrix ();
        Set <String> reserved = layer.getUsedGenes ();
        
        double [] sums = new double [2];
        int [] number = new int [2];
        double leftSum = 0;
        
        int bestGene = -1; double bestDistance = 0;
        for (int i = 0; i < genes.size (); i++) {
            String gene = genes.get (i);
            Arrays.fill (number, 0);
            Arrays.fill (sums, 0d);
            
            if (reserved.contains (gene)) { continue; }
            for (int j = 0; j < entities.size (); j++) {
                SourceEntity entity = dataset.getEntityByGeoAccess (entities.get (j));
                int part = EntityVerdict.NORMAL.equals (entity.getVerdict ()) ? 0 : 1;
                sums [part] += matrix [i][j]; number [part] += 1;
            }
            
            for (int j = 0; j < sums.length; j++) {
                sums [j] /= number [j];
            }
            
            double distance = Math.abs (sums [0] - sums [1]);
            if (distance > bestDistance) {
                leftSum = Math.min (sums [0], sums [1]);
                bestDistance = distance;
                bestGene = i;
            }
        }
        
        if (bestGene == -1 || bestDistance < 1e-4) {
            return; // Difference is too small
        }
        
        String gene = genes.get (bestGene);
        final double trait = this.matrix.denormalizeValue (gene, 
                                    leftSum + bestDistance / 2);

        Predicate <Double> splitter = d -> d >= trait;
        List <String> left = new ArrayList <> (), right = new ArrayList <> ();
        entities.stream ().map (dataset::getEntityByGeoAccess).forEach (e -> {
            if (splitter.test (e.getExpressionByGene (gene))) {
                right.add (e.getGeoAccess ());
            } else { left.add (e.getGeoAccess ()); }
        });
        
        layer.setSplitter (splitter);
        
        Set <String> res = new HashSet <> (reserved);
        res.add (gene);
        
        layer.setLeft (Layer.builder ().usedGenes (res)
                            .depth (layer.getDepth () + 1)
                            .dataset (dataset)
                            .enities (left)
                            .build ());
        if (!layer.getLeft ().isSplittedEnough ()) {
            split (layer.getLeft ());
        }
        
        layer.setRight (Layer.builder ().usedGenes (res)
                             .depth (layer.getDepth () + 1)
                             .dataset (dataset)
                             .enities (right)
                             .build ());
        if (!layer.getRight ().isSplittedEnough ()) {
            split (layer.getRight ());
        }
    }
    
}
