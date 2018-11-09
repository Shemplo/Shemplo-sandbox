package ru.shemplo.genome.rf.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.Setter;

public class SourceDataset {

    private final Map <String, SourceEntity> entitiesMap = new HashMap <> ();
    private final List <SourceEntity> entitiesList = new ArrayList <> ();
    
    @Getter @Setter private String title;
    
    public void addEntity (SourceEntity entity) {
        this.entitiesList.add (entity);
    }
    
    public void updateEntity (int index, Consumer <SourceEntity> consumer) {
        if (index >= 0 && index < entitiesList.size ()) {
            SourceEntity entity = getEntityByIndex (index);
            String backGeoAccess = entity.getGeoAccess ();
            consumer.accept (entity);
            
            if (!Objects.equals (backGeoAccess, entity.getGeoAccess ())) {
                if (backGeoAccess != null) entitiesMap.remove (backGeoAccess);
                entitiesMap.put (entity.getGeoAccess (), entity);
            }
        }
    }
    
    public SourceEntity getEntityByIndex (int index) {
        if (index >= 0 && index < entitiesList.size ()) {
            return entitiesList.get (index);
        }
        
        return null;
    }
    
    public SourceEntity getEntityByGeoAccess (String label) {
        return entitiesMap.get (label);
    }
    
    public int getSize () {
        return entitiesList.size ();
    }
    
}
