package com.chat.nio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class ChatClient implements Runnable{
    private SocketChannel mSc;
    private Selector mSelector;
    private boolean isStart = false;
    public boolean isConnection  = false;
    public ChatClient() {
        isStart = true;
        initChannel();
    }
    public void initChannel() {
        try {
            mSelector = Selector.open();
            mSc = SocketChannel.open();
            mSc.configureBlocking(false);
            mSc.connect(new InetSocketAddress("localhost",9999));
            mSc.register(mSelector,SelectionKey.OP_CONNECT);
        } catch (IOException e) {
            
            e.printStackTrace();
        }
    }
    
    public Selector getSelector() {
        return mSelector;
    }
    
    public void connection(SelectionKey mSKey) throws IOException {
        SocketChannel sc = (SocketChannel) mSKey.channel();
        if(sc.isConnectionPending()){  
            sc.finishConnect();  
        } 
        isConnection = true;
        sc.configureBlocking(false);
        sc.register(mSelector,SelectionKey.OP_READ|SelectionKey.OP_WRITE);
    }
    
    public void writeMessage(String data) throws IOException {
        mSc.write(ByteBuffer.wrap(data.toString().getBytes()));
    }
    
    public String readMessage() throws IOException {
        ByteBuffer bby = ByteBuffer.allocate(1024);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String object = "";
        bby.clear();
        int count = 0;
        byte[] data;
        while((count = mSc.read(bby))>0) {
            bby.flip();
            data = new byte[count];
            bby.get(data);
            baos.write(data);
        }
        object = new String(baos.toByteArray());
        if(baos!=null) {
            baos.close();
        }
        return object;
    }
    @Override
    public void run() {
        if(isStart) {
            try {
                while(mSelector.select()>0) {
                    Iterator<SelectionKey> its = mSelector.selectedKeys().iterator();
                    SelectionKey sks = null;
                    while(its.hasNext()) {
                        sks = its.next();
                        its.remove();
                        if(sks.isConnectable()) {
                            connection(sks);
                        }
                    }
                }
            }catch(Exception e) {
                e.printStackTrace();
                isStart = false;
            }
        }
    }
}
