#!/bin/bash
for i in {00..34}; do
	ssh barriga@ug$i.cs.ualberta.ca "killall -9 java"
done
