package ru.shemplo.genome.rf.forest;

import static ru.shemplo.genome.rf.data.EntityVerdict.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
import ru.shemplo.snowball.utils.Algorithms;

public class DecisionTree {
    
    private final NormalizedMatrix matrix;
    private final SplitStrategy strategy;
    private final SourceDataset dataset;
    private Layer root;
    
    public DecisionTree (SourceDataset dataset, NormalizedMatrix matrix, SplitStrategy strategy) {
        this.dataset = dataset; this.matrix = matrix; this.strategy = strategy;
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
        private double probability;
        private String splitGene;
        private int depth;
        
        public boolean isSplittedEnough () {            
            double zeros = countNormalEntities () * 1.0;
            return (zeros / enities.size () >= 0.95) // 95% of entities are the same
                || depth >= getMaxTreeDepth ()
                || enities.size () == 1;
        }
        
        private int countNormalEntities () {
            AtomicInteger zero = new AtomicInteger ();
            enities.forEach (e -> {
                SourceEntity entity = dataset.getEntityByGeoAccess (e);
                EntityVerdict verdict = entity.getVerdict ();
                zero.addAndGet (NORMAL.equals (verdict) ? 1 : 0);
            });
            
            return zero.get ();
        }
        
        public EntityVerdict predict (Map <String, Double> genes) {
            if (getSplitter () != null && getSplitGene () != null) {
                double exp = genes.get (getSplitGene ());
                boolean test = getSplitter ().test (exp);
                
                Layer next = test ? getRight () : getLeft ();
                return next.predict (genes);                
            }
            
            return getClassification ();
        }
        
        private EntityVerdict getClassification () {
            int zeros = countNormalEntities ();
            return zeros >= enities.size () / 2
                 ? EntityVerdict.NORMAL
                 : EntityVerdict.MELANOMA;
        }
        
        public void propagateProbabilities (int depth, double parent, 
                Map <String, Double> probs) {
            if (getDepth () == depth) {
                if (getSplitGene () == null) { return; }
                this.probability = probs.get (getSplitGene ()) * parent;
            } else {
                final double prob = getProbability ();
                Layer [] layers = {getLeft (), getRight ()};
                Arrays.asList (layers).forEach (l -> {
                    if (l == null) { return; }
                    l.propagateProbabilities (depth, prob, probs);
                });
            }
        }
        
    }
    
    private void split (Layer layer) {
        List <String> entities = layer.getEnities (),
                      genes = matrix.getGenesName ();
        Set <String> reserved = layer.getUsedGenes ();
        strategy.reset ();
        
        for (int i = 0; i < genes.size (); i++) {
            if (reserved.contains (genes.get (i))) { continue; }
            strategy.suggestFeature (i, layer, matrix);
        }
        
        if (strategy.getBestFeature () == -1 
                || strategy.isDifferenceSmall ()) {
            return; // Difference is too small
        }
        
        String gene = genes.get (strategy.getBestFeature ());
        final double trait = this.matrix.denormalizeValue (gene, 
                                      strategy.getBestSplit ());

        Predicate <Double> splitter = d -> d >= trait;
        List <String> left = new ArrayList <> (), right = new ArrayList <> ();
        entities.stream ().map (dataset::getEntityByGeoAccess).forEach (e -> {
            if (splitter.test (e.getExpressionByGene (gene))) {
                right.add (e.getGeoAccess ());
            } else { left.add (e.getGeoAccess ()); }
        });
        
        layer.setSplitter (splitter);
        layer.setSplitGene (gene);
        
        Set <String> res = new HashSet <> (reserved);
        res.add (gene);
        
        layer.setLeft (Layer.builder ().usedGenes (res).depth (layer.getDepth () + 1)
                            .dataset (dataset).enities (left).build ());
        if (!layer.getLeft ().isSplittedEnough ()) {
            split (layer.getLeft ());
        }
        
        layer.setRight (Layer.builder ().usedGenes (res).depth (layer.getDepth () + 1)
                             .dataset (dataset).enities (right).build ());
        if (!layer.getRight ().isSplittedEnough ()) {
            split (layer.getRight ());
        }
    }
    
    public EntityVerdict predict (Map <String, Double> genes) {
        return this.root.predict (genes);
    }
    
    public static final String nameOfMeta = "$numberOfLayers";
    public static int getMaxTreeDepth () { return 9; }
    
    public int getHeight () {
        AtomicInteger height = new AtomicInteger ();
        Algorithms.<Layer> runBFS (root, layer -> {
            height.set (Math.max (height.get (), layer.getDepth () + 1));
            return layer.getLeft () != null && layer.getRight () != null;
        }, layer -> Arrays.asList (layer.getLeft (), layer.getRight ()));
        
        return height.get ();
    }
    
    public Map <String, Integer> getGeneScores () {
        Map <String, Integer> scores = new HashMap <> ();
        final int treeHeight = getMaxTreeDepth ();
        
        Algorithms.<Layer> runBFS (root, layer -> {
            final String gene = layer.getSplitGene ();
            if (gene == null) { return false; }
            
            scores.putIfAbsent (gene, 0);
            int score = treeHeight - layer.getDepth ();
            scores.compute (gene, (k, v) -> v + score * score);
            
            return layer.getLeft () != null && layer.getRight () != null;
        }, layer -> Arrays.asList (layer.getLeft (), layer.getRight ()));
        return scores;
    }
    
    public Map <String, Integer> getGenesOnDepth (int depth) {
        Map <String, Integer> counters = new HashMap <> ();
        counters.put (nameOfMeta, 0);
        
        Queue <Layer> queue = new LinkedList <> ();
        queue.add (root);
        
        while (!queue.isEmpty ()) {
            Layer layer = queue.poll ();
            if (layer == null) { continue; }
            
            if (layer.getDepth () != depth) {
                queue.add (layer.getLeft ()); queue.add (layer.getRight ());
            } else {
                String gene = layer.getSplitGene ();
                if (gene == null) { continue; }
                counters.putIfAbsent (gene, 0);
                
                counters.compute (nameOfMeta, (__, v) -> v + 1);
                counters.compute (gene, (__, p) -> p + 1);
            }
        }
        
        return counters;
    }
    
    public void propagateProbabilities (int depth, double parent, 
            Map <String, Double> probs) {
        this.root.propagateProbabilities (depth, parent, probs);
    }
    
    public Map <String, Double> getAllProbabilities () {
        Map <String, Double> probs = new HashMap <> ();
        Queue <Layer> queue = new LinkedList <> ();
        queue.add (root);
        
        while (!queue.isEmpty ()) {            
            Layer layer = queue.poll ();
            if (layer == null) { continue; }
            
            String gene = layer.getSplitGene ();
            if (gene == null) { continue; }
            probs.putIfAbsent (gene, 0.0d);
            
            probs.compute (gene, (__, v) -> v + layer.getProbability ());
            queue.add (layer.getLeft ()); queue.add (layer.getRight ());
        }
        
        return probs;
    }
    
}
