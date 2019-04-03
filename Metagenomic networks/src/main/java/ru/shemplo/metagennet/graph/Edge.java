package ru.shemplo.metagennet.graph;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.shemplo.snowball.stuctures.Pair;

@EqualsAndHashCode (callSuper = true)
public class Edge extends Pair <Vertex, Vertex> {

    private static final long serialVersionUID = -33766014367150868L;
    
    @Getter @Setter
    @NonNull private Double weight;
    
    public Edge (Vertex F, Vertex S, double weight) { 
        super (F, S); this.weight = weight;
    }
    
    @Override
    public String toString () {
        return String.format ("%d -> %d", F.getId (), S.getId ());
    }
    
    @Override
    public Edge swap () { return new Edge (S, F, weight); }
    
}