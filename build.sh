#!/bin/bash
# Proof-of-concept mill build that can be used with ammonite.
# TODO: make this single file and add maven version

# test success in mill 0.5.1 
mill "diagrammer[2.12.7].assembly"
cp out/diagrammer/2.12.7/assembly/dest/out.jar diagrammer.jar
echo "./firrtl-diagrammer is ready to run"
