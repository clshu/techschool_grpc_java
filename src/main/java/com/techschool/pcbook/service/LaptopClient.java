package com.techschool.pcbook.service;

import com.google.protobuf.ByteString;
import com.techschool.pcbook.pb.*;
import com.techschool.pcbook.pb.LaptopServiceGrpc.LaptopServiceBlockingStub;
import com.techschool.pcbook.pb.LaptopServiceGrpc.LaptopServiceStub;
import com.techschool.pcbook.sample.Generator;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.handler.ssl.SslContext;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LaptopClient {
    private static final Logger logger = Logger.getLogger(LaptopClient.class.getName());

    private final ManagedChannel channel;
    private final LaptopServiceBlockingStub blockingStub;
    private final LaptopServiceStub asyncStub;

    public LaptopClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        blockingStub = LaptopServiceGrpc.newBlockingStub(channel);
        asyncStub = LaptopServiceGrpc.newStub(channel);
    }

    public LaptopClient(String host, int port, SslContext sslContext) {
        channel = NettyChannelBuilder.forAddress(host, port)
                .sslContext(sslContext)
                .build();

        blockingStub = LaptopServiceGrpc.newBlockingStub(channel);
        asyncStub = LaptopServiceGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void createLaptop(Laptop laptop) {
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();
        CreateLaptopResponse response = CreateLaptopResponse.getDefaultInstance();

        try {
//            response = blockingStub.createLaptop(request);
            response = blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).createLaptop(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.ALREADY_EXISTS) {
                // not a big deal
                logger.info("laptop ID already exists");
                return;
            }
            logger.log(Level.SEVERE, "request failed: " + e.getMessage());
            return;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "request failed: " + e.getMessage());
            return;
        }

        logger.info("laptop create with ID:" + response.getId());
    }

    public void searchLaptop(LaptopFilter filter) {
        logger.info("search started");

        SearchLaptopRequest request = SearchLaptopRequest.newBuilder().setFilter(filter).build();
        try {
            Iterator<SearchLaptopResponse> responseIterator = blockingStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .searchLaptop(request);
            while (responseIterator.hasNext()) {
                SearchLaptopResponse response = responseIterator.next();
                Laptop laptop = response.getLaptop();
                logger.info("_ found: " + laptop.getId());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "request failed: " + e.getMessage());
            return;
        }
        logger.info("search completed");
    }

    public void uploadImage(String laptopID, String imagePath) {
        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<UploadImageRequest> requestObserver =
                asyncStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .uploadImage(new StreamObserver<UploadImageResponse>() {
            @Override
            public void onNext(UploadImageResponse response) {
                logger.info("received response:\n" + response);
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.SEVERE, "upload failed: " + t);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                logger.info("image uploaded.");
                finishLatch.countDown();
            }
        });

        FileInputStream fileInputStream;

        try {
            fileInputStream = new FileInputStream(imagePath);
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "cannot find image file: " + imagePath);
            return;
        }

        String imageType = imagePath.substring(imagePath.lastIndexOf("."));
        ImageInfo info = ImageInfo.newBuilder().setLaptopId(laptopID).setImageType(imageType).build();
        UploadImageRequest request = UploadImageRequest.newBuilder().setInfo(info).build();

        try {
            requestObserver.onNext(request);
            logger.info("sent image info:\n" + info);

            byte[] buffer = new byte[1024];
            while (true) {
                int n = fileInputStream.read(buffer);
                if (n <= 0) {
                    break;
                }
                if (finishLatch.getCount() == 0) {
                    return;
                }

                request = UploadImageRequest.newBuilder().setChunkData(ByteString.copyFrom(buffer, 0, n)).build();
                requestObserver.onNext(request);
                logger.info("sent chunk data size: " + n);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "unexpected error: " + e.getMessage());
            requestObserver.onError(e);
            return; // ??
        }

        requestObserver.onCompleted();

        awaitCountDownLatch(finishLatch);
    }

    public void rateLaptop(String[] laptopIDs, double[] scores) {
        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<RateLaptopRequest> requestObserver =
                asyncStub
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .rateLaptop(new StreamObserver<RateLaptopResponse>() {
                            @Override
                            public void onNext(RateLaptopResponse response) {
                                logger.info("received response: laptopID: "
                                        + response.getLaptopId()
                                        + " count: " + response.getRatedCount()
                                        + " average: " + response.getAverageScore());
                            }

                            @Override
                            public void onError(Throwable t) {
                                logger.log(Level.SEVERE, "rate laptop failed. " + t.getMessage());
                                finishLatch.countDown();
                            }

                            @Override
                            public void onCompleted() {
                                logger.info("rate laptop completed.");
                                finishLatch.countDown();
                            }
                        });

        int n = laptopIDs.length;
        try {
            for (int i = 0; i < n; i++) {
                RateLaptopRequest request = RateLaptopRequest.newBuilder()
                        .setLaptopId(laptopIDs[i])
                        .setScore(scores[i])
                        .build();
                requestObserver.onNext(request);
                logger.info("send rate-laptop request. id:" + laptopIDs[i] + " score: " + scores[i]);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "sending request failed. " + e.getMessage());
            requestObserver.onError(e);
            return;
        }

        requestObserver.onCompleted();

        awaitCountDownLatch(finishLatch);

    }

    private void awaitCountDownLatch(CountDownLatch latch) {
        try {
            if (!latch.await(1 , TimeUnit.MINUTES)) {
                logger.warning("request cannot finish within 1 minute");
            }
        } catch (InterruptedException e) {
            logger.info("request is interrupted: " + e.getMessage());
        }
    }

    public static void testCreateLaptop(LaptopClient client, Generator generator) {
        Laptop laptop = generator.NewLaptop();
        client.createLaptop(laptop);
    }
    public static void testSearchLaptop(LaptopClient client, Generator generator) {
        for (int i = 0; i < 10; i++) {
            Laptop laptop = generator.NewLaptop();
            client.createLaptop(laptop);
        }

        Memory minRam = Memory.newBuilder()
                .setValue(8)
                .setUnit(Memory.Unit.GIGABYTE)
                .build();

        LaptopFilter filter = LaptopFilter.newBuilder()
                .setMaxPriceUsd(3000)
                .setMinCpuCores(4)
                .setMinCpuGhz(2.5)
                .setMinRam(minRam)
                .build();

        client.searchLaptop(filter);
    }

    public static void testUploadImage(LaptopClient client, Generator generator) {
        Laptop laptop = generator.NewLaptop();
        client.createLaptop(laptop);
        client.uploadImage(laptop.getId(), "tmp/macbook-air-gold-2015-16.jpg");
    }

    public static void testRateLaptop(LaptopClient client, Generator generator) {
        int n = 3;
        String[] laptopIDs = new String[n];

        for (int i = 0; i < n; i++) {
            Laptop laptop = generator.NewLaptop();
            laptopIDs[i] = laptop.getId();
            client.createLaptop(laptop);
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            logger.info("rate laptop (y/n)? ");
            String answer = scanner.nextLine();
            if (answer.toLowerCase().trim().equals("n")) {
                break;
            }

            double[] scores = new double[n];
            for (int i = 0; i < n; i++) {
                scores[i] = generator.NewLaptopScore();
            }
            client.rateLaptop(laptopIDs, scores);
        }
    }

    public static SslContext loadTLSCredentials() throws SSLException {
        File serverCACertFile = new File("cert/ca-cert.pem");

        return GrpcSslContexts.forClient()
                .trustManager(serverCACertFile)
                .build();
    }

    public static void main(String[] args) {
        SslContext sslContext;
        try {
             sslContext = LaptopClient.loadTLSCredentials();
        } catch (SSLException e) {
            logger.warning("Cannot load TLS credentials: " + e.getMessage());
            return;
        }
        LaptopClient client = new LaptopClient("localhost", 50051, sslContext);

        Generator generator = new Generator();

        try {
            testRateLaptop(client, generator);

        } finally {
            try {
                client.shutdown();
            } catch (InterruptedException e) {
                logger.info("client is interrupted: " + e.getMessage());
            }
        }

    }

}
