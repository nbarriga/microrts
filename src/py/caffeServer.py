import os
os.environ['GLOG_minloglevel'] = '2' 
import StringIO
import numpy as np
import caffe
import socket
import sys
import threading

def main():
    
    PORT = int(sys.argv[1])
    #dim=int(sys.argv[2])
    #nplanes=int(sys.argv[3])
    #size=dim*dim*nplanes
    #definition=sys.argv[4]
    #model=sys.argv[5]
    
    #HOST = "localhost"
    HOST = ""
    
    #caffe.set_mode_cpu()
    #caffe.set_device(1)
    #caffe.set_mode_gpu()
    #net = caffe.Net(
                    #'data/caffe/marius_8x8_deploy_v3.prototxt',
                    #'data/caffe/marius_8x8_rtsnet_v3.caffemodel',
                    #'data/caffe/'+str(dim)+'x'+str(dim)+'.prototxt',
                    #'data/caffe/'+str(dim)+'x'+str(dim)+'.caffemodel',
     #               definition,
     #               model,
     #               caffe.TEST)
    
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    #sock.connect((HOST, PORT))
    sock.bind((HOST, PORT))
    sock.listen(5)
    while True:
        print("Waiting for connection")
        conn, addr = sock.accept()
        conn.setblocking(1) 

        th = threading.Thread(target=processRequests, args = (conn.makefile(),1))
        th.start()
        print("Thread started")

    sock.close()

def processRequests(conn, dummy):

    definition, model = conn.readline().split()
    print("Loading model: "+model)
    caffe.set_device(1)
    caffe.set_mode_gpu()
    net = caffe.Net(
                    definition,
                    model,
                    caffe.TEST)
    while True:
        header = conn.readline()
        #print header
        
        if len(header)==0:
            conn.close()
            return

        w, l, planes = map(int, header.split())
    
        
        size=w*l*planes
        plane_data = np.zeros(size)
    
        plane_data[map(int, conn.readline().split())] = 1
    
	
        x = np.reshape(plane_data, [planes, w, l])
        #x = np.reshape(plane_data, [1, planes, w, l])
	#x = augment(x)
        #net.blobs['data'].reshape(8,planes,w,l)
        net.blobs['data'].data[...] =  x
    
    
        # compute
        out = net.forward()
    
        output=" ".join([str(prob) for prob in out['prob'][0,:,0,0]])+"\n"
        #print output
        conn.write(output)
        conn.flush()
        #conn.sendall(str(out['prob'][0][1][0][0])+"\n")
        #conn.sendall(str(np.average(out['prob'][0][1][0]))+"\n")
        #sock.sendall(str(out['prob'][0][0])+"\n")

def augment(data):
    aug_data = np.zeros((8, data[0].shape[0], data[0].shape[1], data[0].shape[2]), dtype=np.uint8)

    for idx, array in enumerate(data):
        for i in xrange(array.shape[0]):
            aug_data[idx*8+0][i] = array[i]
            aug_data[idx*8+1][i] = np.rot90(array[i])
            aug_data[idx*8+2][i] = np.rot90(array[i], 2)
            aug_data[idx*8+3][i] = np.rot90(array[i], 3)
            aug_data[idx*8+4][i] = np.flipud(aug_data[idx*8+0][i])
            aug_data[idx*8+5][i] = np.fliplr(aug_data[idx*8+1][i])
            aug_data[idx*8+6][i] = np.flipud(aug_data[idx*8+2][i])
            aug_data[idx*8+7][i] = np.fliplr(aug_data[idx*8+3][i])
    return aug_data

main()
