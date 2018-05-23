package com.chat.nio;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public class FileUtils {
    
    private static File mFile = new File("userdata.txt");
    
    public static List<String[]> readUser() throws IOException{
        List<String[]> allUser = new ArrayList<String[]>();
        BufferedReader br = null;
        if(!mFile.exists()) {
            return null;
        }
        try {
            br = new BufferedReader(new FileReader(mFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        String line = "";
        String[] strs = null;
        try {
            while ((line = br.readLine()) != null){
                strs = line.split("\\|");
                System.out.println(line);
                allUser.add(strs);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(br!=null) {
                br.close();
            }
        }
        for(String[] temp:allUser) {
            System.out.println("===========" + temp[0]+","+temp[1]);
        }
        return allUser;
    }
    public static boolean writeUser(String user) {
        BufferedWriter bw = null;
        try {
            if(!mFile.exists()) {
                mFile.createNewFile();
            }
            bw = new BufferedWriter(new FileWriter(mFile,true));
            bw.write(user+"\n");
            return true;
        }catch(IOException e) {
            e.printStackTrace();
        }finally {
           if(bw!=null) {
               try {
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
           }
        }
        return false;
    }
}
