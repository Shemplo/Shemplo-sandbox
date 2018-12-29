package ru.shemplo.genome.rf;

import ru.shemplo.genome.rf.RunCsvConverter.Action;

public class RunPipeline {
 
    public static void main (String ... args) throws Exception {
        RunCsvConverter.action = Action.TOP_N_MCMC;
        RunCsvConverter.main ();
    }
    
}
