#!/bin/bash
for i in `seq 1 4`; do
	ssh barriga@fedorah$i.cs.ualberta.ca "cd tmp/microrts;nohup /local/scratch/barriga/jdk1.8.0_66/bin/java -cp bin/:lib/jdom.jar tests.RunConfigurableExperiments $1/bots.txt - $1/maps.txt $1/results$i.txt 1 &>$1/nohup$i.out&"
done
