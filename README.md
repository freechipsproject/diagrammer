Chisel Visualizer Project
=======================

This project can generate GraphViz dot files and from those png files representing Chisel generated Firrtl circuits.
It is also an example of a creating a Firrtl Transformation.  This transformation can be applied through the 
use of annotations as demonstrated in the examples.GCD test.  

It can also be applied directly through the Visualizer objects main method.
Use sbt 'runMain dotvisualizer.Visualizer <lowFirrtlFile>'

## TODO
- Better shapes
- More complete handling of firrtl
- Currently only modules (not instances of modules can be annotated)
- Limit the depth of processing to a particular number
- Stretch goal: Generate image just for components that an annotated element depends on.