#!/bin/bash
for i in {00..34};do
	echo ug$i
	ssh barriga@ug$i.cs.ualberta.ca $1 
done
