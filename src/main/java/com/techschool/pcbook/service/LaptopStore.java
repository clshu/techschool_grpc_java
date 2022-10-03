package com.techschool.pcbook.service;

import com.techschool.pcbook.pb.Laptop;
import com.techschool.pcbook.pb.LaptopFilter;

public interface LaptopStore {
    // It could be a db, in memory store for now
    void Save(Laptop laptop) throws Exception;
    Laptop Find(String id);
    void Search(LaptopFilter filter, LaptopStream stream);
}






