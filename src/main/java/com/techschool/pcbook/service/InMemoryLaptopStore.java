package com.techschool.pcbook.service;

import com.techschool.pcbook.pb.Laptop;
import com.techschool.pcbook.pb.LaptopFilter;
import com.techschool.pcbook.pb.Memory;

import io.grpc.Context;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class InMemoryLaptopStore  implements  LaptopStore {
    private static final Logger logger = Logger.getLogger(InMemoryLaptopStore.class.getName());
    private ConcurrentMap<String, Laptop> data;

    public InMemoryLaptopStore() {
        data = new ConcurrentHashMap<>(0);
    }

    @Override
    public void Save(Laptop laptop) throws Exception {
        if (data.containsKey(laptop.getId())) {
            throw new AlreadyExistException("laptop already exists.");
        }

        // deep copy
        Laptop other = laptop.toBuilder().build();
        data.put(other.getId(), other);
    }

    @Override
    public Laptop Find(String id) {
        if (!data.containsKey(id)) {
            return null;
        }
        return data.get(id);
    }

    @Override
    public void Search(Context ctx, LaptopFilter filter, LaptopStream stream) {
        for (Map.Entry<String, Laptop> entry: data.entrySet()) {
            if (ctx.isCancelled()) {
                logger.info("context is cancelled");
                return;
            }
            // Heavy load simulation
//            try {
//                TimeUnit.SECONDS.sleep(1);
//            } catch (InterruptedException e)  {
//                e.printStackTrace();
//            }
            Laptop laptop = entry.getValue();
            if (isQualified(filter, laptop)) {
                stream.Send(laptop.toBuilder().build());
            }
        }
    }

    private boolean isQualified(LaptopFilter filter, Laptop laptop) {
        if (laptop.getPriceUsd() > filter.getMaxPriceUsd()) {
            return false;
        }
        if (laptop.getCpu().getNumCores() < filter.getMinCpuCores()) {
            return false;
        }
        if (laptop.getCpu().getMinGhz() < filter.getMinCpuGhz()) {
            return false;
        }
        if (toBit(laptop.getRam()) < toBit(filter.getMinRam())) {
            return false;
        }

        return true;
    }

    private long toBit(Memory ram) {
        long value = ram.getValue();
        switch (ram.getUnit()) {
            case BIT:
                return  value;
            case BYTE:
                return value << 3; // 8 bits = 2^3 BIT
            case KILLOBYTE:
                return value << 13; // 8 * 1024 = 2^13 BIT
            case MEGABYTE:
                return value << 23; // 8 * 1024 * 1024 = 2^23 BIT
            case GIGABYTE:
                return value << 33; // 8 * 1024 * 1024 * 1024 = 2^33 BIT
            case TERABYTE:
                return value << 43; // 8 * 1024 * 1024 * 1024 * 1024 = 2^43 BIT
            default:
                return 0;
        }
    }
}
