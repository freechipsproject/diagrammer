#!/bin/bash
# Proof-of-concept mill build that can be used with ammonite.
# TODO: make this single file and add maven version

mill diagrammer.assembly
cp out/diagrammer/assembly/dest/out.jar diagrammer.jar
echo "./firrtl-diagrammer is ready to run"
