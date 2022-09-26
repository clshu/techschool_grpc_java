package com.techschool.pcbook.serializer;

import com.techschool.pcbook.pb.Laptop;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Serializer {
    public void WriteBinaryFile(Laptop laptop, String filename) throws IOException {
        FileOutputStream outStream = new FileOutputStream(filename);
        laptop.writeTo(outStream);
        outStream.close();
    }

    public Laptop ReadBinaryFile(String filename) throws IOException {
        FileInputStream inStream = new FileInputStream(filename);
        Laptop laptop = Laptop.parseFrom(inStream);
        inStream.close();
        return  laptop;
    }
}
