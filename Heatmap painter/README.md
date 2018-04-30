# Heatmap painter

> **Heatmap** - table that has all cells painted to some gradient color depending on value in such cell

There are several kinds of these maps for different purposes. 
This one is for mapping two sets of sets of data with known attitude coefficients.

Due to numbers can be placed in undefined order, the visual analysis can be complicated.
So this heatmap painter solves such problem by sorting data to diagonal representation.

#### Build:

```
$ mvn package

$ java -cp heatmap.painter*.jar ru.shemplo.heatmap.painter.RunPainter [path to matrix]
```

#### Example:

![example](heatmap.jpg)