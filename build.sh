#!/bin/bash
# Proof-of-concept mill build that can be used with ammonite.
# TODO: make this single file and add maven version

# test success in mill 0.5.1 
mill diagrammer.assembly
find -wholename "*assembly/dest/out.jar" -exec cp {} ./diagrammer.jar \; 
echo "./firrtl-diagrammer is ready to run"
