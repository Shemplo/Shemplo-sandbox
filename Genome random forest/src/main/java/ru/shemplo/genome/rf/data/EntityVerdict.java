package ru.shemplo.genome.rf.data;


public enum EntityVerdict {
 
    NORMAL, NEVUS, MELANOMA;
    
    public static EntityVerdict string2Verdict (String input) {
        switch (input.trim ().replace ("\"", "").toLowerCase ()) {
            case "melanoma": return MELANOMA;
            case "normal"  : return NORMAL;
            case "nevus"   : return NEVUS;
            
            default: return null;
        }
    }
    
}
