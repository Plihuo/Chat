package com.chat.nio;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ChatServer implements Runnable{
    private Selector mSelector;
    private ServerSocketChannel mSSC;
    private SelectionKey mSSKey;
    private boolean isStart = false;
    private List<String> userNames = new ArrayList<String>();
    public ChatServer() {
        isStart = true;
        initChannel();
    }
    private void initChannel() {
        try {
            mSelector = Selector.open();
            mSSC = ServerSocketChannel.open();
            mSSC.configureBlocking(false);
            mSSC.bind(new InetSocketAddress(9999));
            mSSKey = mSSC.register(mSelector,SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void connection(SelectionKey mSKey) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) mSKey.channel();
        SocketChannel sc = ssc.accept();
        sc.configureBlocking(false);
        sc.register(mSelector,SelectionKey.OP_READ);
    }
    private void writeMessage(SelectionKey mSKey) throws IOException {
        SocketChannel sc = (SocketChannel) mSKey.channel();
        Object object = mSKey.attachment();
        sc.write(ByteBuffer.wrap(object.toString().getBytes()));
        if(object.equals("close")) {
            mSKey.cancel();
            sc.socket().close();
            sc.close();
            return;
        }
        mSKey.interestOps(SelectionKey.OP_READ);
    }
    private boolean isLoginSuccess = false;
    private boolean isUserExist = false;
    private boolean isLogin = false;
    private void readMessage(SelectionKey mSKey) throws IOException {
        SocketChannel sc = (SocketChannel) mSKey.channel();
        ByteBuffer bby = ByteBuffer.allocate(1024);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String object = "";
        bby.clear();
        int count = 0;
        byte[] data;
        while((count = sc.read(bby))>0) {
            bby.flip();
            data = new byte[count];
            bby.get(data);
            baos.write(data);
        }
        object = new String(baos.toByteArray());
        if(baos!=null) {
            baos.close();
        }
        System.out.println(object);
        if(object.startsWith("register:")) {
            String userAndPass = object.substring("register:".length());
            System.out.println("===="+userAndPass);
            String[] currentUser = userAndPass.split("\\|");
            List<String[]> allUser = FileUtils.readUser();
            if(allUser!=null&&allUser.size()>0) {
                for(String[] user:allUser) {
                    if(user[0].equals(currentUser[0])) {
                        isUserExist = true;
                        break;
                    }
                    isUserExist = false;
                }
            }
            System.out.println("User is exist:"+isUserExist);
            if(isUserExist) {
                System.out.println("User is exist attachToSingleClient");
                attachToSingleClient(mSKey,"register:false:用户已经存在");
            }else {
                boolean isWrite = FileUtils.writeUser(userAndPass);
                attachToSingleClient(mSKey,"register:"+isWrite+"");
            }
        } else if(object.startsWith("login:")) {
            String userAndPass = object.substring("login:".length());
            System.out.println("==== "+userAndPass);
            String[] currentUser = userAndPass.split("\\|");
            List<String[]> allUser = FileUtils.readUser();
            if(allUser!=null&&allUser.size()>0) {
                for(String[] user:allUser) {
                    if(user[0].equals(currentUser[0])&&user[1].equals(currentUser[1])) {
                        for(String name:userNames) {
                            if(name.equals(currentUser[0])) {
                                isLogin = true;
                                break;
                            }
                            isLogin = false;
                        }
                        if(!isLogin) {
                            isLoginSuccess = true;
                            attachToSingleClient(mSKey,"login:true");
                            break;
                        }else {
                            attachToSingleClient(mSKey,"login:false:"+"用户已经登陆");
                            break;
                        }
                    }
                    isLoginSuccess = false;
                }
            }
            if(!isLoginSuccess&&!isLogin) {
                attachToSingleClient(mSKey,"login:false");
            }
        } else if(object.startsWith("close:")) {
            String userName = object.substring("close:".length());
            userNames.remove(userName);
            System.out.println("========" + userName + "退出窗体========");
            attachToSingleClient(mSKey, "close");
            attachToOtherClient(mSKey, "users:"+userNames+"|"+userName+"退出聊天室\n");
        }else if(object.startsWith("open:")){
            String userName = object.substring(5);
            userNames.add(userName);
            System.out.println("========" + userName + "进入聊天室========");
            attachToAllClient(mSKey, "users:"+userNames+"|"+userName+" 进入聊天室"+"\n");
        }else {
            System.out.println("进入默认处理");
            String userName = object.toString().substring(0, object.toString().indexOf("^"));
            String mess = object.toString().substring(object.toString().indexOf("^") + 1);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String dateTime = dateFormat.format(new Date());
            String mss = userName + " " + dateTime + "\n" + mess + "\n";
            System.out.println("mss "+mss);
            attachToAllClient(mSKey, mss);
        }
    }
    
    private void attachToSingleClient(SelectionKey mSKey,Object object) {
        mSKey.attach(object);
        mSKey.interestOps(mSKey.interestOps()|SelectionKey.OP_WRITE);
    }
    
    private void attachToOtherClient(SelectionKey mSKey,Object object) {
        Iterator<SelectionKey> keys = mSKey.selector().keys().iterator();
        while(keys.hasNext()) {
            SelectionKey key = keys.next();
            if(key!=mSSKey&&key!=mSKey) {
                key.attach(object);
                key.interestOps(key.interestOps()|SelectionKey.OP_WRITE);
            }
        }
    }
    
    private void attachToAllClient(SelectionKey mSKey,Object object) {
        Iterator<SelectionKey> keys = mSKey.selector().keys().iterator();
        while(keys.hasNext()) {
            SelectionKey key = keys.next();
            if(key!=mSSKey) {
                key.attach(object);
                key.interestOps(key.interestOps()|SelectionKey.OP_WRITE);
            }
        }
    }
    @Override
    public void run() {
        if(isStart) {
            try {
                while(mSelector.select()>0) {
                    Iterator<SelectionKey> its = mSelector.selectedKeys().iterator();
                    while(its.hasNext()) {
                        SelectionKey sks = its.next();
                        its.remove();
                        if(sks.isAcceptable()) {
                            connection(sks);
                        }
                        if(sks.isReadable()) {
                            readMessage(sks);
                        }
                        if(sks.isWritable()) {
                            writeMessage(sks);
                        }
                    }
                }
            }catch(Exception e) {
                e.printStackTrace();
                isStart = false;
            }
        }
    }
    public static void main(String[] args) {
        ChatServer cs = new ChatServer();
        new Thread(cs).start();
    }
}
