#!/usr/bin/ipython3 --i
# coding: utf-8
import sys
bots = [line.rstrip('\n') for line in open(sys.argv[1])]
bots = [s for s in bots if s[0]!='#']
bots = [s for s in bots if s[0]!='']
wins=[]
ties=[]
loses=[]
for f in range(len(sys.argv)-2):
	lines = [line.rstrip('\n') for line in open(sys.argv[f+2])]
	for i in range(len(lines)):
    		if lines[i].startswith("Wins:"):
        		linenum=i
	wins.append(lines[linenum+1:linenum+len(bots)+1])
	for i in range(len(lines)):
    		if lines[i].startswith("Ties:"):
        		linenum=i
	ties.append(lines[linenum+1:linenum+len(bots)+1])
	for i in range(len(lines)):
    		if lines[i].startswith("Loses:"):
        		linenum=i
	loses.append(lines[linenum+1:linenum+len(bots)+1])
	for i in range(len(bots)):loses[f][i]=[int(elem) for elem in loses[f][i].split(',')[0:len(bots)]]
	for i in range(len(bots)):ties[f][i]=[int(elem) for elem in ties[f][i].split(',')[0:len(bots)]]
	for i in range(len(bots)):wins[f][i]=[int(elem) for elem in wins[f][i].split(',')[0:len(bots)]]

totalwins=wins[0]
totalties=ties[0]
totalloses=loses[0]
for f in range(1,len(wins)):
	totalwins=[[a + b for a, b in zip(first,second)] for first,second in zip(totalwins,wins[f])]
	totalties=[[a + b for a, b in zip(first,second)] for first,second in zip(totalties,ties[f])]
	totalloses=[[a + b for a, b in zip(first,second)] for first,second in zip(totalloses,loses[f])]

totalpoints=[[a + b*0.5 for a, b in zip(first,second)] for first,second in zip(totalwins,totalties)]

points=[b for b in zip(bots,[sum(a) for a in totalpoints])]
percentages=[b for b in zip(bots,[sum(a)*100.0/(2*len(wins)*len(bots)) for a in totalpoints])]
