package com.techschool.pcbook.service;

import com.techschool.pcbook.pb.Laptop;

public interface LaptopStore {
    // It could be a db, in memory store for now
    void Save(Laptop laptop) throws Exception;
    Laptop Find(String id);
}




