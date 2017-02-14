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
    
    dims = [8, 16, 24, 128]
    
    
    #caffe.set_mode_cpu()
    caffe.set_device(1)
    caffe.set_mode_gpu()
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

        data = conn.recv(8192)
        definition, model = data.split()
        print("Loading model: "+model)
        net = caffe.Net(
                    definition,
                    model,
                    caffe.TEST)
        th = threading.Thread(target=processRequests, args = (conn,net))
        th.start()
        print("Thread started")

    sock.close()

def processRequests(conn, net):
    while True:
        data = conn.recv(8192)
        
        if len(data)==0:
            conn.close()
            return

        a = map(int, data.split())
        w, l, planes = a[0:3]
        
        size=w*l*planes
        plane_data = np.zeros(size)
    
        plane_data[a[3:len(a)]] = 1
    
        x = np.reshape(plane_data, [planes, w, l])
        net.blobs['data'].data[...] =  x
    
    
        # compute
        out = net.forward()
    
        conn.sendall(str(out['prob'][0][1][0][0])+"\n")
        #sock.sendall(str(out['prob'][0][0])+"\n")

main()
