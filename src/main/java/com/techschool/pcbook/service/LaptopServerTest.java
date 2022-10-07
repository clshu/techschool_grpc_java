package com.techschool.pcbook.service;

import com.techschool.pcbook.pb.*;
import com.techschool.pcbook.sample.Generator;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class LaptopServerTest {
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private LaptopStore laptopStore;
    private  ImageStore imageStore;
    private  RatingStore ratingStore;
    private LaptopServer server;
    private ManagedChannel channel;

    @Before
    public void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(serverName).directExecutor();

        laptopStore = new InMemoryLaptopStore();
        imageStore = new DiskImageStore("tmp");
        ratingStore = new InMemoryRatingStore();

        server = new LaptopServer(serverBuilder, 0, laptopStore, imageStore, ratingStore);
        server.start();

        channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build()
        );
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void createLaptopWithValidID() {
        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop();
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();

        LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
        CreateLaptopResponse response = stub.createLaptop(request);
        assertNotNull(response);
        assertEquals(response.getId(), laptop.getId());

        Laptop found = laptopStore.Find(response.getId());
        assertNotNull(found);
    }

    @Test
    public void createLaptopWithEmptyID() {
        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop().toBuilder().setId("").build();
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();

        LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
        CreateLaptopResponse response = stub.createLaptop(request);
        assertNotNull(response);
        assertFalse(response.getId().isEmpty());

        Laptop found = laptopStore.Find(response.getId());
        assertNotNull(found);
    }

    @Test(expected = StatusRuntimeException.class)
    public void createLaptopWithInvalidID1() {
        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop().toBuilder().setId("invalid").build();
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();

        LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
        CreateLaptopResponse response = stub.createLaptop(request);
    }

    @Test
    public void createLaptopWithInvalidID2() {
        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop().toBuilder().setId("invalid").build();
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();

        LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
        CreateLaptopResponse response;
        try {
            response = stub.createLaptop(request);
        } catch (StatusRuntimeException e) {
            assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
        }
    }

    @Test
    public void createLaptopWithAlreadyExistsID() {
        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop();
        try {
            laptopStore.Save(laptop);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();

        LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
        CreateLaptopResponse response;
        try {
            response = stub.createLaptop(request);
        } catch (StatusRuntimeException e) {
            assertEquals(Status.Code.ALREADY_EXISTS, e.getStatus().getCode());
        }
    }

    @Test
    public void rateLaptop() throws Exception {
        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop();
        laptopStore.Save(laptop);

        LaptopServiceGrpc.LaptopServiceStub stub = LaptopServiceGrpc.newStub(channel);
        RateLaptopResponseStreamObserver responseStreamObserver = new RateLaptopResponseStreamObserver();
        StreamObserver<RateLaptopRequest> requestStreamObserver = stub.rateLaptop(responseStreamObserver);

        double[] scores = {8, 7.5, 10};
        double[] averages = {8, 7.75, 8.5};
        int n = scores.length;

        for (int i = 0; i < n; i++) {
            RateLaptopRequest request = RateLaptopRequest.newBuilder()
                    .setLaptopId(laptop.getId())
                    .setScore(scores[i])
                    .build();
            requestStreamObserver.onNext(request);
        }

        requestStreamObserver.onCompleted();
        assertNull(responseStreamObserver.err);
        assertTrue(responseStreamObserver.complete);
        assertEquals(n, responseStreamObserver.responses.size());

        int idx = 0;
        for (RateLaptopResponse response : responseStreamObserver.responses) {
            assertEquals(laptop.getId(), response.getLaptopId());
            assertEquals(idx + 1, response.getRatedCount());
            assertEquals(averages[idx], response.getAverageScore(), 1e-9);
            idx++;
        }
    }

    private class RateLaptopResponseStreamObserver implements StreamObserver<RateLaptopResponse> {
        public List<RateLaptopResponse> responses;
        public Throwable err;
        public boolean complete;

        public RateLaptopResponseStreamObserver() {
            responses = new LinkedList<>();
        }

        @Override
        public void onNext(RateLaptopResponse response) {
            responses.add(response);
        }

        @Override
        public void onError(Throwable t) {
            err = t;
        }

        @Override
        public void onCompleted() {
            complete = true;
        }
    }
}