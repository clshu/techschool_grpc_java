package com.techschool.pcbook.service;

import com.google.protobuf.ByteString;
import com.techschool.pcbook.pb.*;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

public class LaptopService extends LaptopServiceGrpc.LaptopServiceImplBase {
    private static final Logger logger = Logger.getLogger(LaptopService.class.getName());
    private LaptopStore laptopStore;
    private ImageStore imageStore;

    public LaptopService(LaptopStore laptopStore, ImageStore imageStore) {
        this.laptopStore = laptopStore;
        this.imageStore = imageStore;
    }

    @Override
    public void createLaptop(CreateLaptopRequest request, StreamObserver<CreateLaptopResponse> responseStreamObserver) {
        Laptop laptop = request.getLaptop();

        String id = laptop.getId();
        logger.info("get a create-laptop request with ID: " + id);

        UUID uuid;
        if (id.isEmpty()) {
            uuid = UUID.randomUUID();
        } else {
            try {
                uuid = UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                responseStreamObserver.onError(
                        Status.INVALID_ARGUMENT
                                .withDescription(e.getMessage())
                                .asRuntimeException()

                );
                return;
            }
        }

        // heavy processing
//        try {
//            TimeUnit.SECONDS.sleep(3);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        if (Context.current().isCancelled()) {
            logger.info("request is cancelled");
            responseStreamObserver.onError(
                    Status.CANCELLED
                            .withDescription("request is cancelled")
                            .asRuntimeException()
            );
            return;
        }

        Laptop other = laptop.toBuilder().setId(uuid.toString()).build();
        // Save other laptop to the store
        try {
            laptopStore.Save(other);
        } catch (AlreadyExistException e) {
            responseStreamObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription(e.getMessage())
                            .asRuntimeException()
            );
            return;
        } catch (Exception e) {
            responseStreamObserver.onError(
                    Status.INTERNAL
                            .withDescription((e.getMessage()))
                            .asRuntimeException()
            );
            return;
        }

        CreateLaptopResponse response = CreateLaptopResponse.newBuilder().setId(other.getId()).build();
        responseStreamObserver.onNext(response);
        responseStreamObserver.onCompleted();

        logger.info("saved laptop with ID: " + other.getId());
    }


    @Override
    public void searchLaptop(SearchLaptopRequest request, StreamObserver<SearchLaptopResponse> responseStreamObserver) {
        LaptopFilter filter = request.getFilter();
        logger.info("get a search-laptop request with filter:\n" + filter);

        laptopStore.Search(Context.current(), filter, new LaptopStream() {
            @Override
            public void Send(Laptop laptop) {
                logger.info("found laptop with ID: " + laptop.getId());
                SearchLaptopResponse response = SearchLaptopResponse.newBuilder().setLaptop(laptop).build();
                responseStreamObserver.onNext(response);
            }
        });

        responseStreamObserver.onCompleted();
        logger.info("search laptop completed");
    }

    @Override
    public StreamObserver<UploadImageRequest> uploadImage(StreamObserver<UploadImageResponse> responseObserver) {
        return new StreamObserver<UploadImageRequest>() {
            private static final long maxImageSize = 1 << 20;
            private String laptopID;
            private String imageType;
            private ByteArrayOutputStream imageData;

            @Override
            public void onNext(UploadImageRequest request) {
                if (request.getDataCase() == UploadImageRequest.DataCase.INFO) {
                    ImageInfo info = request.getInfo();
                    logger.info("received image info\n" + info);

                    laptopID = info.getLaptopId();
                    imageType = info.getImageType();
                    imageData = new ByteArrayOutputStream();

                    Laptop found = laptopStore.Find(laptopID);
                    if (found == null) {
                        responseObserver.onError(
                                Status.INVALID_ARGUMENT
                                        .withDescription("laptop ID doesn't exist. ID: " + laptopID)
                                        .asRuntimeException()
                        );
                    }

                    return;
                }

                ByteString chunkData = request.getChunkData();
                logger.info("received image chunk with size: " + chunkData.size());

                if (imageData == null) {
                    logger.info("image info wasn't sent before");
                    responseObserver.onError(
                            Status.INVALID_ARGUMENT
                                    .withDescription("image info wasn't sent before")
                                    .asRuntimeException()
                    );
                    return;
                }

                long size = imageData.size() + chunkData.size();
                if (size > maxImageSize) {
                    logger.info("image size is too large: " + size);
                    responseObserver.onError(
                            Status.INVALID_ARGUMENT
                                    .withDescription("image size is too large: " + size)
                                    .asRuntimeException()
                    );
                    return;
                }

                try {
                    chunkData.writeTo(imageData);
                } catch (IOException e) {
                    responseObserver.onError(
                            Status.INTERNAL
                                    .withDescription("cannot write chunk data: " + e.getMessage())
                                    .asRuntimeException()
                    );
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warning(t.getMessage());
            }

            @Override
            public void onCompleted() {
                String imageID = "";
                int imageSize = imageData.size();

                try {
                    imageID = imageStore.Save(laptopID, imageType, imageData);
                } catch (IOException e) {
                    responseObserver.onError(
                            Status.INTERNAL
                                    .withDescription("cannot save image to the store: " + e.getMessage())
                                    .asRuntimeException()
                    );
                    return;
                }

                UploadImageResponse response = UploadImageResponse.newBuilder()
                        .setId(imageID)
                        .setSize(imageSize)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                logger.info("save image with ID: " + imageID + " size: " + imageSize);
            }
        };
    }
}
