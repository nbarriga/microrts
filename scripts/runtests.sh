#!/bin/bash
m="ug"
#       12 12 12 6 1 1 1 3 3 6 6 6 6 6
#suffixes L L L F A A A C C F F F F F	
#maps="8x8 16x16 24x24 (4)BloodBath.scm (2)Benzene.scx (2)Destination.scx (2)HeartBreakRidge.scx (3)Aztec.scx (3)TauCross.scx (4)Andromeda.scx (4)CircuitBreaker.scx (4)EmpireoftheSun.scx (4)Fortress.scx (4)Python.scx"

mapDir="8x8"
mapName="basesWorkers8x8"
suffix=$(echo {A..L} )
mapNames=$(echo ${mapName}{A..L})
mapPaths=$(echo maps/$mapDir/${mapName}{A..L}.xml)
mapNamesArr=($mapNames)
mapPathsArr=($mapPaths)


mapDir="16x16"
mapName="basesWorkers16x16"
suffix=$(echo {A..L} )
mapNames=$(echo ${mapName}{A..L})
mapPaths=$(echo maps/$mapDir/${mapName}{A..L}.xml)
mapNamesArr+=($mapNames)
mapPathsArr+=($mapPaths)

mapDir="24x24"
mapName="basesWorkers24x24"
suffix=$(echo {A..L} )
mapNames=$(echo ${mapName}{A..L})
mapPaths=$(echo maps/$mapDir/${mapName}{A..L}.xml)
mapNamesArr+=($mapNames)
mapPathsArr+=($mapPaths)

mapDir="BroodWar"
mapName="\(4\)BloodBath.scm"
suffix=$(echo {A..F} )
mapNames=$(echo ${mapName}{A..F})
mapPaths=$(echo maps/$mapDir/${mapName}{A..F}.xml)
mapNamesArr+=($mapNames)
mapPathsArr+=($mapPaths)

mapDir="BroodWar"
mapName="\(2\)Benzene.scx"
suffix=$(echo {A..A} )
mapNames=$(echo ${mapName}{A..A})
mapPaths=$(echo maps/$mapDir/${mapName}{A..A}.xml)
mapNamesArr+=($mapNames)
mapPathsArr+=($mapPaths)

mapDir="BroodWar"
mapName="\(2\)Destination.scx"
suffix=$(echo {A..A} )
mapNames=$(echo ${mapName}{A..A})
mapPaths=$(echo maps/$mapDir/${mapName}{A..A}.xml)
mapNamesArr+=($mapNames)
mapPathsArr+=($mapPaths)

mapDir="BroodWar"
mapName="\(2\)HeartbreakRidge.scx"
suffix=$(echo {A..A} )
mapNames=$(echo ${mapName}{A..A})
mapPaths=$(echo maps/$mapDir/${mapName}{A..A}.xml)
mapNamesArr+=($mapNames)
mapPathsArr+=($mapPaths)

mapDir="BroodWar"
mapName="\(3\)Aztec.scx"
suffix=$(echo {A..C} )
mapNames=$(echo ${mapName}{A..C})
mapPaths=$(echo maps/$mapDir/${mapName}{A..C}.xml)
mapNamesArr+=($mapNames)
mapPathsArr+=($mapPaths)

mapDir="BroodWar"
mapName="\(3\)TauCross.scx"
suffix=$(echo {A..C} )
mapNames=$(echo ${mapName}{A..C})
mapPaths=$(echo maps/$mapDir/${mapName}{A..C}.xml)
mapNamesArr+=($mapNames)
mapPathsArr+=($mapPaths)

mapDir="BroodWar"
mapName="\(4\)Andromeda.scx"
suffix=$(echo {A..F} )
mapNames=$(echo ${mapName}{A..F})
mapPaths=$(echo maps/$mapDir/${mapName}{A..F}.xml)
mapNamesArr+=($mapNames)
mapPathsArr+=($mapPaths)

mapDir="BroodWar"
mapName="\(4\)CircuitBreaker.scx"
suffix=$(echo {A..F} )
mapNames=$(echo ${mapName}{A..F})
mapPaths=$(echo maps/$mapDir/${mapName}{A..F}.xml)
mapNamesArr+=($mapNames)
mapPathsArr+=($mapPaths)

mapDir="BroodWar"
mapName="\(4\)EmpireoftheSun.scx"
suffix=$(echo {A..F} )
mapNames=$(echo ${mapName}{A..F})
mapPaths=$(echo maps/$mapDir/${mapName}{A..F}.xml)
mapNamesArr+=($mapNames)
mapPathsArr+=($mapPaths)

mapDir="BroodWar"
mapName="\(4\)Fortress.scx"
suffix=$(echo {A..F} )
mapNames=$(echo ${mapName}{A..F})
mapPaths=$(echo maps/$mapDir/${mapName}{A..F}.xml)
mapNamesArr+=($mapNames)
mapPathsArr+=($mapPaths)

mapDir="BroodWar"
mapName="\(4\)Python.scx"
suffix=$(echo {A..F} )
mapNames=$(echo ${mapName}{A..F})
mapPaths=$(echo maps/$mapDir/${mapName}{A..F}.xml)
mapNamesArr+=($mapNames)
mapPathsArr+=($mapPaths)

outDir=resultsSimpleNoTie

for machine in {00..34}; do
	mapPath=${mapPathsArr[10#$machine]}
	mapName=${mapNamesArr[10#$machine]}
	mapPath2=${mapPathsArr[10#$machine+35]}
	mapName2=${mapNamesArr[10#$machine+35]}
	if [ "$mapName" = "" ]; then
    		break
	fi
	#ssh -f barriga@$m${machine}.cs.ualberta.ca "cd git-working/microrts&& mkdir -p $outDir && nice -n19 ~/jdk1.8.0_66/bin/java -Xmx4096m -cp bin/:lib/jdom.jar tests.RunConfigurableExperiments bots2.txt - $mapPath $outDir/results-$mapName.txt 1 &> $outDir/output-$mapName.out"
	ssh -f barriga@$m${machine}.cs.ualberta.ca "cd git-working/microrts&& mkdir -p $outDir && nice -n19 ~/jdk1.8.0_66/bin/java -Xmx4096m -cp bin/:lib/jdom.jar tests.RunConfigurableExperiments bots1.txt - $mapPath $outDir/results-$mapName.txt 1 &> $outDir/output-$mapName.out && nice -n19 ~/jdk1.8.0_66/bin/java -Xmx4096m -cp bin/:lib/jdom.jar tests.RunConfigurableExperiments bots1.txt - $mapPath2 $outDir/results-$mapName2.txt 1 &> $outDir/output-$mapName2.out"
done
