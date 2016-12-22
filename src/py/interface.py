import StringIO
import numpy as np
import caffe
import socket
import sys

dim=int(sys.argv[1])
nplanes=int(sys.argv[2])
size=dim*dim*nplanes

HOST = "localhost"
PORT = 8080

caffe.set_mode_cpu()
#caffe.set_mode_gpu()
net = caffe.Net(
                #'data/caffe/marius_8x8_deploy_v3.prototxt',
                #'data/caffe/marius_8x8_rtsnet_v3.caffemodel',
                'data/caffe/'+str(dim)+'x'+str(dim)+'.prototxt',
                'data/caffe/'+str(dim)+'x'+str(dim)+'.caffemodel',
                caffe.TEST)

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect((HOST, PORT))

#create empty 1600 array
plane_data = np.zeros(size)

while True:
    plane_data = np.zeros(size)
    data = sock.recv(8192)
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
    print out['prob'][0][1]
    sock.sendall(str(out['prob'][0][1])+"\n")
    # sock.sendall(str(out['prob'].argmax())+"\n")

sock.close()
print "Socket closed"
