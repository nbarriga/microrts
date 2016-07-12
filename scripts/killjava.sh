#!/bin/bash
for i in `seq 1 4`; do
	ssh barriga@fedorah$i.cs.ualberta.ca "killall -9 java"
done
