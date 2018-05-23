package com.chat.nio;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class LoginAndRegisterFrame extends JFrame{
    private static final long serialVersionUID = 1L;
    private JButton mLoginButton,mRegisterButton;
    private JLabel mUserName,mPassword;
    private JTextField mUserNameField,mPasswordField;
    // Client业务类
    private ChatClient client;
    private JLabel mTitle,mNotice;
    private boolean isStart = false;
    private static final int MAX_WEIDTH=300,MAX_HEIGHT=200;
    private static String userName="";
    public LoginAndRegisterFrame(ChatClient client) {
        this.client = client;
        new Thread(client).start();
        initView();
        initAction();
        isStart = true;
    }
    private void initView() {
        setLayout(null);
        setTitle("聊天室");
        setLocation(600,300);
        setSize(MAX_WEIDTH,MAX_HEIGHT);
        this.setResizable(false);
        mLoginButton = new JButton("登陆");
        mLoginButton.setBounds(70,140,80,20);
        mRegisterButton = new JButton("注册");
        mRegisterButton.setBounds(150,140,80,20);
        mUserName = new JLabel("用户名:");
        mPassword = new JLabel("密    码:");
        mUserName.setBounds(40,40,80,20);
        mPassword.setBounds(40,70,80,20);
        mUserNameField = new JTextField();
        mPasswordField = new JTextField();
        mUserNameField.setBounds(100,40,160,20);
        mPasswordField.setBounds(100,70,160,20);
        mTitle = new JLabel("登陆聊天室");
        mTitle.setBounds(130,10,100,20);
        mNotice = new JLabel();
        mNotice.setBounds(0,100,MAX_WEIDTH,20);
        mNotice.setHorizontalAlignment(SwingConstants.CENTER);
        add(mLoginButton);
        add(mRegisterButton);
        add(mUserName);
        add(mPassword);
        add(mUserNameField);
        add(mPasswordField);
        add(mTitle);
        add(mNotice);
    }
    
    public void display() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        new Thread(new ctUsers()).start();
    }
    private void initAction() {
        mRegisterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = mUserNameField.getText();
                String password = mPasswordField.getText();
                try {
                    client.writeMessage("register:"+username+"|"+password);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            
        });
        
        mLoginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mNotice.setText("");
                String username = mUserNameField.getText();
                String password = mPasswordField.getText();
                userName = username;
                try {
                    client.writeMessage("login:"+username+"|"+password);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 发送关闭信息
                try {
                    client.writeMessage("close:"+"null");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                System.exit(0);
            }
        });
    }
    
    class ctUsers extends Thread {
        public void run() {
            Selector selector = client.getSelector();
            try {
                while ((selector.select() > 0)&&isStart) {
                    for (SelectionKey sk : selector.selectedKeys()) {
                      if (sk.isReadable()) {
                          String registerMessage = null;
                          try {
                              if(client.isConnection) {
                                  registerMessage = client.readMessage();
                                  System.out.println("message:"+registerMessage);
                              }
                          } catch (IOException e) {
                              e.printStackTrace();
                          }
                          if(registerMessage.startsWith("login:")) {
                              registerMessage = registerMessage.substring("login:".length());
                              if(registerMessage.equals("true")) {
                                  ChatFrame chatFrame = new ChatFrame(client,userName);
                                  chatFrame.show();
                                  isStart = false;
                                  LoginAndRegisterFrame.this.setVisible(false);
                              }else {
                                  System.out.println("registerMessage:  "+registerMessage);
                                  if(registerMessage.startsWith("false:")) {
                                      mNotice.setText(registerMessage.substring("false:".length()));
                                  }else if(registerMessage.equals("false")) {
                                      mNotice.setText("登陆失败请重新登录");
                                  }
                                
                              }
                          }else if(registerMessage.startsWith("register:")) {
                              registerMessage = registerMessage.substring("register:".length());
                              if(registerMessage.equals("true")) {
                                  mNotice.setText("注册成功，请登陆");
                              }else {
                                  if(registerMessage.startsWith("false:")) {
                                      mNotice.setText(registerMessage.substring("false:".length()));
                                  }else {
                                      mNotice.setText("注册失败，请重新注册");
                                  }
                              }
                          }
                      }
                      // 删除正在处理的SelectionKey
                      selector.selectedKeys().remove(sk);
                    }
                  }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
