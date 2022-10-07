package com.techschool.pcbook.service;

public interface RatingStore {
    Rating Add(String laptopID, double score);
}
