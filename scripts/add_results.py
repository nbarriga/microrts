#!/usr/bin/ipython3 --i
# coding: utf-8
import sys
import statistics
import glob

bots = [line.rstrip('\n') for line in open(sys.argv[1])]
bots = [s for s in bots if s[0]!='#']
bots = [s for s in bots if s[0]!='']

def parseGames(files):
	wins=[]
	ties=[]
	loses=[]
	for f in range(len(files)):
		lines = [line.rstrip('\n') for line in open(files[f])]
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
	percentages=[b for b in zip(bots,[sum(a)*100.0/(2*len(wins)*(len(bots)-1)) for a in totalpoints])]
	
	assert(50.0==statistics.mean([b for a,b in percentages]))
	return percentages,totalpoints

def filterIndices(indices,totalpoints):
	newpoints=[]
	newbots=[]
	indices.sort()
	for i in indices:
		newpoints.append([totalpoints[i][j] for j in indices])
		newbots.append(bots[i])

	numMaps= sum([sum(a) for a in newpoints])/len(newbots)/(len(newbots)-1)
	newpercentages=[b for b in zip(newbots,[sum(a)*100.0/(2*numMaps*(len(newbots)-1)) for a in newpoints])]
	assert(50.0==statistics.mean([b for a,b in newpercentages]))
	return newpercentages,newpoints 

def filter(indices):
	newpoints=[]
	newpercentages=[]
	for i in totalpoints:
		p,t=filterIndices(indices,i)
		newpercentages.append(p)
		newpoints.append(t)
	return newpercentages,newpoints

def printAll():
	for i in percentages:
		[print(x[1],end=",") for x in i]
		print("")


prefix="../resultsSimpleNoTie/results-"
files=[
"basesWorkers8x8*",
"basesWorkers16x16*",
"basesWorkers24x24*",
"(4)BloodBath.scm*",

#"(2)Benzene.scx*",
#"(2)Destination.scx*",
#"(2)HeartbreakRidge.scx*",

"(2)*",
"(3)*",
#"(3)Aztec.scx*",
#"(3)TauCross.scx*",
"(4)Andromeda.scx*",
"(4)CircuitBreaker.scx*",
#"(4)EmpireoftheSun.scx*",
#"(4)Fortress.scx*",
#"(4)Python.scx*",
]
percentages=[]
totalpoints=[]
for i in files:
	print(i)
	p,t=parseGames(glob.glob(prefix+i))
	percentages.append(p)
	totalpoints.append(t)

