#!/bin/bash
for i in `seq 1 4`;do
	ssh barriga@fedorah$i.cs.ualberta.ca "ps aux|grep RunConfigurable|head -n1"
done
