import StringIO
import numpy as np
import caffe
import socket
import sys

PORT = int(sys.argv[1])
dim=int(sys.argv[2])
nplanes=int(sys.argv[3])
size=dim*dim*nplanes
definition=sys.argv[4]
model=sys.argv[5]

HOST = "localhost"

caffe.set_mode_cpu()
#caffe.set_device(1)
#caffe.set_mode_gpu()
net = caffe.Net(
                #'data/caffe/marius_8x8_deploy_v3.prototxt',
                #'data/caffe/marius_8x8_rtsnet_v3.caffemodel',
                #'data/caffe/'+str(dim)+'x'+str(dim)+'.prototxt',
                #'data/caffe/'+str(dim)+'x'+str(dim)+'.caffemodel',
                definition,
                model,
                caffe.TEST)

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect((HOST, PORT))

sock.setblocking(1) 
#create empty 1600 array
plane_data = np.zeros(size)

while True:
    plane_data = np.zeros(size)
    data = sock.recv(8192)
    #data = sock.recv(16384)
    #while len(data)==0:
    #    data = sock.recv(16384)
    #data = sock.recv(16384000)
    #print len(data)
    a = map(int, data.split())
    w, l, planes = a[0:3]
    #print w,l,planes

    # f = open("debug", "w")
    # f.write(data)
    # # f.write("\n".join(map(lambda x: str(x), a)))
    # f.close()

    # mask = a[3:len(a)]
    plane_data[a[3:len(a)]] = 1

    # a = np.reshape(a[3:len(a)], [planes, w, l])
    # net.blobs['data'].data[...] = np.reshape(a[3:len(a)], [planes, w, l])
    # net.blobs['data'].data[...] = np.reshape(plane_data, [planes, w, l])
    x = np.reshape(plane_data, [planes, w, l])
    #net.blobs['data'].data[...] =  np.tile(x,(32,1,1,1))
    net.blobs['data'].data[...] =  x


    # compute
    out = net.forward()
    #print out['prob']
    #print out['prob'][0][1]
    #print 'before'
    sock.sendall(str(out['prob'][0][1])+"\n")
    #print 'after'
    # sock.sendall(str(out['prob'].argmax())+"\n")

sock.close()
print "Socket closed"
