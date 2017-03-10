import os
os.environ['GLOG_minloglevel'] = '2' 
import StringIO
import numpy as np
import caffe
import socket
import sys
import time
import multiprocessing as mp

def main():
    
    PORT = int(sys.argv[1])
    HOST = ""
    
#    caffe.set_device(0)
#    caffe.set_mode_gpu()
    
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind((HOST, PORT))
    sock.listen(5)
    while True:
        print("Waiting for connection")
        conn, addr = sock.accept()
        conn.setblocking(1) 

        th = mp.Process(target=processRequests, args = (conn.makefile(),1))
        th.start()
        print("Thread started")

    sock.close()

def processRequests(conn, dummy):
    try:
        definition, model = conn.readline().split()
        print("Loading model: "+model)
        caffe.set_device(0)
        caffe.set_mode_gpu()
        net = caffe.Net(
                        definition,
                        model,
                        caffe.TEST)
        while True:
            header = conn.readline()
            #print header
            
            if len(header)==0:
                del net
                conn.close()
                return
    
            w, l, planes = map(int, header.split())
        
            #a=time.clock()
            maxChoices = 4
            maxDepth = 4
            states = maxChoices**maxDepth
            size=w*l*(planes+maxChoices*maxDepth)
            plane_data = np.zeros((states,size))
        
            indices =  conn.readline().split()
            #print indices
            for state in range(0,states):
                plane_data[state,map(int, indices)] = 1
        
            x = np.reshape(plane_data, [states, planes+maxChoices*maxDepth, w, l])

            for i in range(0,maxChoices):
                for j in range(0,maxChoices):
                    for k in range(0,maxChoices):
                        for l in range(0,maxChoices):
                            for index,var in enumerate([i,j,k,l]):
                                x[i*64+j*16+k*4+l,planes+maxChoices*index+var]=1

            #b=time.clock()
            #x = np.reshape(plane_data, [1, planes, w, l])
    	    #x = augment(x)
            #net.blobs['data'].reshape(8,planes,w,l)
            net.blobs['data'].data[...] =  x
        
            #c=time.clock()
            # compute
            out = net.forward()
            #d=time.clock()
            #print out['score']
            output=" ".join([str(prob) for prob in out['score'][:,0,0,0]])+"\n"
            #print output
            #print str(b-a)+" "+str(c-b)+" "+str(d-c)
            conn.write(output)
            conn.flush()
            del x
            del out
            #conn.sendall(str(out['prob'][0][1][0][0])+"\n")
            #conn.sendall(str(np.average(out['prob'][0][1][0]))+"\n")
            #sock.sendall(str(out['prob'][0][0])+"\n")
    except Exception as ex:
        print str(ex)
    finally:
        conn.close()

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
