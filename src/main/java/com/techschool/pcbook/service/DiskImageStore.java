package com.techschool.pcbook.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DiskImageStore implements ImageStore {
    private String imageFolder;
    private ConcurrentMap<String, ImageMetaData> data;

    public DiskImageStore(String folder) {
        this.imageFolder = folder;
        this.data = new ConcurrentHashMap<>(0);
    }


    @Override
    public String Save(String laptopID, String imageType, ByteArrayOutputStream imageData) throws IOException {
        String imageID = UUID.randomUUID().toString();
        String imagePath = String.format("%s/%s%s", imageFolder, imageID, imageType);

        createDirIfNotExists(imageFolder);

        FileOutputStream fileOutputStream = new FileOutputStream(imagePath);
        imageData.writeTo(fileOutputStream);
        fileOutputStream.close();

        ImageMetaData metaData = new ImageMetaData(laptopID, imageType, imagePath);
        data.put(imageID, metaData);

        return imageID;
    }

    private void createDirIfNotExists(String folder) throws IOException {
        File dir = new File(folder);
        if (!dir.exists()) {
            // mkdirs in case there are multiple parents. e.g. tmp/a/b
            dir.mkdirs();
        }
    }
}
