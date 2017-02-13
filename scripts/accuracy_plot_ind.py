import matplotlib.cm as cmx
import matplotlib.colors as colors
import matplotlib.pyplot as plt
import matplotlib.legend as lgd
import matplotlib.markers as mks
import numpy as np
import scipy.stats as ss
import math
import sys

file=sys.argv[1]
num=int(sys.argv[2])

def dev(k, N):
    m = float(k)/N
    return np.sqrt((float(k)*(1.0 - m)*(1.0 - m) + float(N-k)*m*m) / N)

lines = [line.rstrip('\n') for line in open(file)]
res = []
counts = []
for alg in range(num): 
    res.append(lines[alg*2].translate(None, "[],").split())
    del res[alg][-1]
    counts.append(lines[alg*2+1].translate(None, "[],").split())
    del counts[alg][-1]

for alg in range(num): 
    res[alg] = np.asarray(map(float, res[alg]))
    counts[alg] = np.asarray(map(float, counts[alg]))
    #print res[alg].sum() / counts.sum()

fig = plt.figure(figsize=(12, 9))

# major ticks every 20, minor ticks every 5
ax = fig.add_subplot(1, 1, 1)
ax.set_xlim([0, 100])

major_ticks = np.arange(0, 101, 10)
minor_ticks = np.arange(0, 101, 5)
ax.set_xticks(major_ticks)
ax.set_xticks(minor_ticks, minor=True)
ax.set_yticks(major_ticks)
ax.set_yticks(minor_ticks, minor=True)
# or if you want differnet settings for the grids:
ax.grid(which='minor', alpha=0.5)
ax.grid(which='major', alpha=0.9)

i_sd=[]
for alg in range(num): 
    i_sd.append( np.zeros(20, dtype=np.float))

for alg in range(num): 
    for i in range(20):
        i_sd[alg][i] = dev(res[alg][i], counts[alg][i])*120.0/math.sqrt(counts[alg][i])
    res[alg] = res[alg] * 100 / counts[alg]

x = np.arange(20)*5 + 2.5

colors=['black','red','blue','green','yellow']
for alg in range(num): 
    plt.plot(x, res[alg], label='Algorithm '+str(alg), color=colors[alg], linewidth=2)
    plt.fill_between(x, res[alg]-i_sd[alg],  res[alg]+i_sd[alg], color=colors[alg], alpha=0.1)

plt.legend(loc=4, ncol=1, fancybox=True, shadow=False)
plt.setp(plt.gca().get_legend().get_texts(), fontsize='24')

plt.xlabel('Game time (as percentage)', fontsize=28)
plt.ylabel('Accuracy', fontsize=28)
ax.set_ylim([40, 100])

plt.grid(True)
fig.tight_layout()
plt.show()

print "done!"






