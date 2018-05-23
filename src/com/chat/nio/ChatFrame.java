package com.chat.nio;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class ChatFrame {
    // 文本框
    private JTextArea readContext = new JTextArea(18, 30); // 显示信息
    private JTextArea writeContext = new JTextArea(6, 30);// 发送信息
    private JScrollPane scroll = null;
    private JTree mTree = null;
    // 按钮
    private JButton btnSub = new JButton("提交"); // 提交按钮
    private JButton btnRes = new JButton("取消");// 取消按钮
    // 窗体界面
    private JFrame aFrame = new JFrame("ChatFrame");
    // 用户名
    private String userName;
    // Client业务类
    private ChatClient client;

    // 构造方法
    public ChatFrame(ChatClient clientBiz,String username) {
        // 开启线程
        client = clientBiz;
        userName = username;
    }
    // 初始化界面
    private void init() throws IOException, ClassNotFoundException {
        aFrame.setLayout(null);
        aFrame.setTitle(userName + " 聊天窗口");
        aFrame.setSize(620, 500);
        aFrame.setLocation(400, 200);
        aFrame.setResizable(false);
        readContext.setEditable(false);
        scroll = new JScrollPane(readContext); 
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setBounds(10, 10,465, 285);
        writeContext.setBounds(10, 305, 465, 100);
        aFrame.add(scroll);
        aFrame.add(writeContext);
        btnSub.setBounds(150, 415, 80, 30);
        btnRes.setBounds(250, 415, 80, 30);
        // frame的关闭按钮事件
        aFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 发送关闭信息
                try {
                    client.writeMessage("close:"+userName);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                System.exit(0);
            }
        });

        // 提交按钮事件
        btnSub.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 发送信息
                try {
                    client.writeMessage(userName + "^" + writeContext.getText());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                writeContext.setText(null);
            }
        });

        // 关闭按钮事件
        btnRes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 发送关闭信息
                try {
                    client.writeMessage("close:"+userName);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                System.exit(0);
            }
        });
        aFrame.add(btnSub);
        aFrame.add(btnRes);
        aFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // 界面显示
    public void show() throws IOException, ClassNotFoundException {
        init();
        aFrame.setVisible(true);
        client.writeMessage("open:"+userName);
        new Thread(new ctUsers()).start();
    }

    class ctUsers extends Thread {
        public void run() {
            Selector selector = client.getSelector();
            try {
                while (selector.select() > 0) {
                    for (SelectionKey sk : selector.selectedKeys()) {
                      if (sk.isReadable()) {
                          Object obj = null;
                          try {
                              if(client.isConnection) {
                                  obj = client.readMessage();
                              }
                          } catch (IOException e) {
                              e.printStackTrace();
                          }
                          if (obj != null) {
                              if(((String)obj).startsWith("users:")) {
                                  String message = obj.toString().substring("users:".length());
                                  String[] users = message.split("\\|");
                                  String str = readContext.getText() +"\n"+ users[1];
                                  readContext.setText(str);
                                  JScrollBar bar=scroll.getVerticalScrollBar();
                                  bar.setValue(bar.getMaximum());
                                  String allUsers = users[0].replaceAll("\\[","").replaceAll("\\]","");
                                  String[] displayUser = allUsers.split(",");
                                  new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        DefaultMutableTreeNode top = new DefaultMutableTreeNode("在线人员");
                                        for (String user:displayUser) {
                                            top.add(new DefaultMutableTreeNode(user));
                                        }
                                        mTree = new JTree(top);
                                        JScrollPane treePanel = new JScrollPane(mTree);
                                        treePanel.setBounds(480,10,120,445);
                                        aFrame.add(treePanel);
                                        expandAll(mTree,new TreePath(top),true);
                                        aFrame.validate();
                                    }
                                      
                                  }).start();
                              }else {
                                  String str = readContext.getText() +"\n"+ obj.toString();
                                  readContext.setText(str);
                                  JScrollBar bar=scroll.getVerticalScrollBar();
                                  bar.setValue(bar.getMaximum());
                              }
                          }
                      }
                      // 删除正在处理的SelectionKey
                      selector.selectedKeys().remove(sk);
                    }
                  }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @SuppressWarnings("rawtypes")
    private static void expandAll(JTree tree, TreePath parent, boolean expand) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }
}
