Chisel Visualizer Project
=======================

This project can generate GraphViz dot files and from those png files representing Chisel generated Firrtl circuits.
It is also an example of a creating a Firrtl Transformation.  This transformation can be applied through the 
use of annotations as demonstrated in the examples.GCD test.  

It can also be applied directly through the Visualizer objects main method.
Use sbt 'runMain dotvisualizer.Visualizer <lowFirrtlFile>'

## Using
### Install
Installing this software should be pretty much the following.
```bash
git clone https://github.com/chick/visualizer
cd visualizer
sbt publishLocal
```

### Add dependency to your project
You must add visualizer to your dependencies.  Here is an example build.sbt file that 
has this dependency added.  This assumes you are using chisel3 and chisel-iotesters
as you would typically be doing if you started with [chisel-template](https://github.com/ucb-bar/chisel-template).

```sbtshell
name := "my-chisel-experiments"

version := "1.0"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

// Provide a managed dependency on X if -DXVersion="" is supplied on the command line.
val defaultVersions = Map(
  "chisel3" -> "3.1-SNAPSHOT",
  "chisel-iotesters" -> "1.1-SNAPSHOT",
  "chisel-dot-visualizer" -> "0.1-SNAPSHOT"
  )

libraryDependencies ++= (Seq("chisel3","chisel-iotesters","chisel-dot-visualizer").map {
  dep: String => "edu.berkeley.cs" %% dep % sys.props.getOrElse(dep + "Version", defaultVersions(dep)) })

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.5",
  "org.scalacheck" %% "scalacheck" % "1.12.4")
```

### Adding the visualizer annotation.
It's probably easiest to look at the test examples in this repo to see what you need to do to generate a graph.
This basically amounts to adding `with VisualizerAnnotator` to the module you want to annotate and then
Adding the following annotation command anywhere in your module.
`visualize(this, depth = 1)`
The depth argument tells it how deep to go in graphing submodules.
If you use zero you get an uninteresting graph with just the module IOs.
You might also want to try adding `setDotProgram("fdp")` into your module, which changes the graphviz renderer.
Sometimes `fdp` seems to do a better job with the diagram.
Other programs in that family have not been tested by me but might work as well.

## TODO
- Better shapes
- More complete handling of firrtl
- Currently only modules (not instances of modules can be annotated)
- Stretch goal: Generate image just for components that an annotated element depends on.
- Do this right and use D3 to make prettier animated graphs.

## How Visualizer Works
Class VisualizerTransform (at the end of the Visualizer.scala program) creates the original graph of the program - it calls on RemoveUselessGenAndT Pass and TopLevelModPass to remove the Gen and T boxes in between elements and to create a high level module relationship overview, respectively. It uses DOT language to create these graphs and writes to files using Java PrintWriter.

The TopLevelModPass currently uses the firrtl.analyses.InstanceGraph to collect instances associated with each module. It is a work in progress though.

Generated files go to the test_run_dir directory.
