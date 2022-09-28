package com.techschool.pcbook.service;

import com.techschool.pcbook.pb.CreateLaptopRequest;
import com.techschool.pcbook.pb.CreateLaptopResponse;
import com.techschool.pcbook.pb.Laptop;
import com.techschool.pcbook.pb.LaptopServiceGrpc;
import com.techschool.pcbook.pb.LaptopServiceGrpc.LaptopServiceBlockingStub ;
import com.techschool.pcbook.sample.Generator;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LaptopClient {
    private static final Logger logger = Logger.getLogger(LaptopClient.class.getName());

    private final ManagedChannel channel;
    private final LaptopServiceBlockingStub blockingStub;

    public LaptopClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        blockingStub = LaptopServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void createLaptop(Laptop laptop) {
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();
        CreateLaptopResponse response = CreateLaptopResponse.getDefaultInstance();

        try {
            response = blockingStub.createLaptop(request);
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

    public static void main(String[] args) {
        LaptopClient client = new LaptopClient("localhost", 50051);

        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop().toBuilder().setId("").build();

        try {
            client.createLaptop(laptop);
        } finally {
            try {
                client.shutdown();
            } catch (InterruptedException e) {
                logger.info("client is interrupted: " + e.getMessage());
            }
        }

    }

}
