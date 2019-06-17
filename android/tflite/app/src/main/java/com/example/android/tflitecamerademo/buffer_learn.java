package com.example.android.tflitecamerademo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class buffer_learn {

    private ByteBuffer imgData = null;
    public void test() throws IOException {

        RandomAccessFile aFile = new RandomAccessFile("/home/chen/Documents/nio-data1.txt", "rw");
        FileChannel inChannel1 = aFile.getChannel();
        aFile = new RandomAccessFile("/home/chen/Documents/nio-data2.txt", "rw");
        FileChannel inChannel2 = aFile.getChannel();

        //create buffer with capacity of 48 bytes
        ByteBuffer buf = ByteBuffer.allocateDirect(20*4);
        buf.order(ByteOrder.nativeOrder());
        //int bytesRead = inChannel1.read(buf); //read into buffer.
        float num = (float)1.1;

        for(int i=0;i<10;i++) {
            buf.putFloat(num);
            num++;
        }
        num = (float)1.2;
        for(int i=0;i<10;i++) {
            buf.putFloat(num);
            num++;
        }
//
//        num = 0;
//        while(num<=5){
//            buf.putFloat((int)(num+3)*4,num);
//            num++;
//        }
        for(int i=0;i<10;i++) {
            float tmp = (float) buf.getFloat((i+10)*4);
            buf.putFloat((i)*4,tmp);
        }
        num = (float)1.3;
        for(int i=0;i<10;i++) {
            buf.putFloat((i+10)*4,num);
            num++;
        }


        for(int i=0;i<20;i++) {

            buf.flip();  //make buffer ready for read

            while(buf.hasRemaining()){
                System.out.print((float) buf.getFloat()+"_"); // read 1 byte at a time
            }

            buf.clear(); //make buffer ready for writing
        }
        aFile.close();
    }

    public Map<Integer, Object> allocateOutputBuffers(int[] shapes){
        int o_size = shapes.length;
        Map<Integer, Object> outputs = new HashMap<>();
        for (int i=0; i < o_size; i++) {
            ByteBuffer o_bytes = ByteBuffer.allocate(shapes[i]);
            outputs.put(i, o_bytes);
        }
        return outputs;
    }

    public static void  main(String[] args) throws IOException {
        System.out.println("helloworld");
        buffer_learn a = new buffer_learn();
        a.test();
        int oShape[] = {1 * 23 * 17 * 17 * 4, 1 * 23 * 17 * 34 * 4, 1 * 23 * 17 * 64 * 4, 1 * 23 * 17 * 1 * 4};
        a.allocateOutputBuffers(oShape);
    }
}