#!/bin/bash
m="ug"
#suffixes L L L F A A A C C F	
#maps="8x8 16x16 24x24 BloodBath Benzene Destination HeartBreakRidge Aztec TauCross Andromeda"
maps="8x8/basesWorkers8x8"
suffix=$(echo {A..L} )
mapNames=$(echo maps/${maps}{A..L}.xml)
mapArr=($mapNames)

maps="16x16/basesWorkers16x16"
suffix=$(echo {A..L} )
mapNames=$(echo maps/${maps}{A..L}.xml)
mapArr+=($mapNames)


maps="24x24/basesWorkers24x24"
suffix=$(echo {A..L} )
mapNames=$(echo maps/${maps}{A..L}.xml)
mapArr+=($mapNames)

for machine in {00..01}; do
	map=${mapArr[10#$machine]}
	echo ssh -f barriga@$m${machine}.cs.ualberta.ca "cd git-working/microrts && nice -n19 ~/jdk1.8.0_66/bin/java -Xmx4096m -cp bin/:lib/jdom.jar tests.RunConfigurableExperiments bots1.txt - $map results-$map.txt 1 &>output-$machine.out&"
done
