#!/bin/bash
for i in {00..34};do
	echo ug$i
	ssh barriga@ug$i.cs.ualberta.ca "ps aux|grep RunConfigurable|grep -v grep|grep -v bash"
done
