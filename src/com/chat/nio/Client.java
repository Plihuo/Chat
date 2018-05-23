package com.chat.nio;
import java.awt.EventQueue;
public class Client {
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                ChatClient client = new ChatClient();
                LoginAndRegisterFrame lrf = new LoginAndRegisterFrame(client);
                lrf.display();
            }
       });
    }
}