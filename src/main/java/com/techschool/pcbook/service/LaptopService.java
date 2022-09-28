package com.techschool.pcbook.service;

import com.techschool.pcbook.pb.CreateLaptopRequest;
import com.techschool.pcbook.pb.CreateLaptopResponse;
import com.techschool.pcbook.pb.Laptop;
import com.techschool.pcbook.pb.LaptopServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.UUID;
import java.util.logging.Logger;

public class LaptopService extends LaptopServiceGrpc.LaptopServiceImplBase {
    private static final Logger logger = Logger.getLogger(LaptopService.class.getName());
    private LaptopStore store;

    public LaptopService(LaptopStore store) {
        this.store = store;
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

        Laptop other = laptop.toBuilder().setId(uuid.toString()).build();
        // Save other laptop to the store
        try {
            store.Save(other);
        } catch(AlreadyExistException e) {
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
}
