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
    // �ı���
    private JTextArea readContext = new JTextArea(18, 30); // ��ʾ��Ϣ
    private JTextArea writeContext = new JTextArea(6, 30);// ������Ϣ
    private JScrollPane scroll = null;
    private JTree mTree = null;
    // ��ť
    private JButton btnSub = new JButton("�ύ"); // �ύ��ť
    private JButton btnRes = new JButton("ȡ��");// ȡ����ť
    // �������
    private JFrame aFrame = new JFrame("ChatFrame");
    // �û���
    private String userName;
    // Clientҵ����
    private ChatClient client;

    // ���췽��
    public ChatFrame(ChatClient clientBiz,String username) {
        // �����߳�
        client = clientBiz;
        userName = username;
    }
    // ��ʼ������
    private void init() throws IOException, ClassNotFoundException {
        aFrame.setLayout(null);
        aFrame.setTitle(userName + " ���촰��");
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
        // frame�Ĺرհ�ť�¼�
        aFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // ���͹ر���Ϣ
                try {
                    client.writeMessage("close:"+userName);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                System.exit(0);
            }
        });

        // �ύ��ť�¼�
        btnSub.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // ������Ϣ
                try {
                    client.writeMessage(userName + "^" + writeContext.getText());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                writeContext.setText(null);
            }
        });

        // �رհ�ť�¼�
        btnRes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // ���͹ر���Ϣ
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

    // ������ʾ
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
                                        DefaultMutableTreeNode top = new DefaultMutableTreeNode("������Ա");
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
                      // ɾ�����ڴ����SelectionKey
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
