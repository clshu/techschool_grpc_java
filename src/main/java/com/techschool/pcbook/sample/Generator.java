package com.techschool.pcbook.sample;

import com.google.protobuf.Timestamp;
import com.techschool.pcbook.pb.*;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

public class Generator {
    private Random rand;

    public Generator() {
        rand = new Random();
    }

    public Keyboard NewKeyboard() {
        return Keyboard.newBuilder()
                .setLayout(randomKeyboardLayout())
                .setBacklit(rand.nextBoolean())
                .build();
    }

    public CPU NewCPU() {
        String brand = randomCPUBrand();
        String name = randomCPUName(brand);

        int numCores = randomInt(2, 8);
        int numThreads = randomInt(numCores, 12);

        double minGhz = randomDouble(2.0, 3.5);
        double maxGhz = randomDouble(minGhz, 5.0);

        return CPU.newBuilder()
                .setBrand(brand)
                .setName(name)
                .setNumCores(numCores)
                .setNumThreads(numThreads)
                .setMinGhz(minGhz)
                .setMaxGhz(maxGhz)
                .build();
    }

    public GPU NewGPU() {
        String brand = randomGPUBrand();
        String name = randomGPUName(brand);

        double minGhz = randomDouble(1.0, 1.5);
        double maxGhz = randomDouble(minGhz, 2.0);

        Memory memory = Memory.newBuilder()
                .setValue(randomInt(2, 6))
                .setUnit(Memory.Unit.GIGABYTE)
                .build();

        return GPU.newBuilder()
                .setBrand(brand)
                .setName(name)
                .setMinGhz(minGhz)
                .setMaxGhz(maxGhz)
                .setMemory(memory)
                .build();
    }

    public Memory NewRAM() {
        return Memory.newBuilder()
                .setValue(randomInt(4, 64))
                .setUnit(Memory.Unit.GIGABYTE)
                .build();
    }

    public Storage NewSSD() {
        Memory memory = Memory.newBuilder()
                .setValue((randomInt(128, 1024)))
                .setUnit(Memory.Unit.GIGABYTE)
                .build();

        return Storage.newBuilder()
                .setDriver(Storage.Driver.SSD)
                .setMemory(memory)
                .build();
    }

    public Storage NewHDD() {
        Memory memory = Memory.newBuilder()
                .setValue((randomInt(1, 6)))
                .setUnit(Memory.Unit.TERABYTE)
                .build();

        return Storage.newBuilder()
                .setDriver(Storage.Driver.HDD)
                .setMemory(memory)
                .build();
    }

    public Screen NewScreen() {
        int height = randomInt(1080, 4320);
        int width = height * 16 / 9;

        Screen.Resolution resolution = Screen.Resolution.newBuilder()
                .setHeight(height)
                .setWidth(width)
                .build();

        return Screen.newBuilder()
                .setSizeInch(randomFloat(13, 17))
                .setResolution(resolution)
                .setPanel(randomScreenPanel())
                .setMultiTouch(rand.nextBoolean())
                .build();
    }

    public Laptop NewLaptop() {
        String brand = randomLaptopBrand();
        String name = randomLaptopName(brand);

        float weightKg = randomFloat(1, 3);
        float priceUsd = randomFloat(1500, 3500);

        int releaseYear = randomInt(2015, 2022);

        return Laptop.newBuilder()
                .setId(randomID())
                .setBrand(brand)
                .setName(name)
                .setCpu(NewCPU())
                .setRam(NewRAM())
                .addGpu(NewGPU())
                .addStorage(NewSSD())
                .addStorage(NewHDD())
                .setScreen(NewScreen())
                .setKeyboard(NewKeyboard())
                .setWeightKg(weightKg)
                .setPriceUsd(priceUsd)
                .setReleaseYear(releaseYear)
                .setUpdatedAt(timestampNow())
                .build();
    }

    public double NewLaptopScore() {
        return randomInt(1, 10);
    }
    // private functions as random generator of objects
    private String randomCPUBrand() {
        return randomStringFromSet("Intel", "AMD", "ARM");
    }

    private String randomCPUName(String brand) {
        switch(brand) {
            case "Intel":
                return randomStringFromSet("i3", "i5", "i7", "i9");
            case "AMD":
                return randomStringFromSet("Ryzen 3", "Ryzen 5", "Ryzen 7", "Ryzen 9");
            case "ARM":
                return randomStringFromSet("Cortex A53", "Cortex A55", "Cortex A57", "Cortex A72", "Cortex A73", "Cortex A75");
            default:
                return "";
        }
    }

    private String randomGPUBrand() {
        return randomStringFromSet("NVIDIA", "AMD", "Intel");
    }

    private String randomGPUName(String brand){
        switch (brand) {
            case "NVIDIA":
                return randomStringFromSet("GTX 1050", "GTX 1060", "GTX 1070", "GTX 1080", "GTX 1650", "GTX 1660", "GTX 1660 Ti", "GTX 2060", "GTX 2070", "GTX 2080", "GTX 2080 Ti");
            case "AMD":
                return randomStringFromSet("Radeon 530", "Radeon 550", "Radeon 560", "Radeon 570", "Radeon 580", "Radeon 590", "Radeon 630", "Radeon 640", "Radeon 650", "Radeon 660", "Radeon 670", "Radeon 680", "Radeon 690", "Radeon 700", "Radeon 710", "Radeon 720", "Radeon 730", "Radeon 740", "Radeon 750", "Radeon 760", "Radeon 770", "Radeon 780", "Radeon 790", "Radeon 800", "Radeon 810", "Radeon 820", "Radeon 830", "Radeon 840", "Radeon 850", "Radeon 860", "Radeon 870", "Radeon 880", "Radeon 890", "Radeon 900", "Radeon 910", "Radeon 920", "Radeon 930", "Radeon 940", "Radeon 950", "Radeon 960", "Radeon 970", "Radeon 980", "Radeon 990", "Radeon 1000", "Radeon 1010", "Radeon 1020", "Radeon 1030", "Radeon 1040", "Radeon 1050", "Radeon 1060", "Radeon 1070", "Radeon 1080", "Radeon 1090", "Radeon 1100", "Radeon 1110", "Radeon 1120", "Radeon 1130", "Radeon 1140", "Radeon 1150", "Radeon 1160", "Radeon 1170", "Radeon 1180", "Radeon 1190", "Radeon 1200", "Radeon 1210", "Radeon 1220", "Radeon 1230", "Radeon 1240", "Radeon 1250", "Radeon 1260", "Radeon 1270", "Radeon 1280", "Radeon 1290", "Radeon 1300", "Radeon 1310", "Radeon 1320");
            case "Intel":
                return randomStringFromSet("UHD Graphics 620", "UHD Graphics 630", "UHD Graphics 640", "UHD Graphics 650", "UHD Graphics 660", "UHD Graphics 670", "UHD Graphics 680", "UHD Graphics 690", "UHD Graphics 700", "UHD Graphics 710", "UHD Graphics 720", "UHD Graphics 730", "UHD Graphics 740", "UHD Graphics 750", "UHD Graphics 760", "UHD Graphics 770", "UHD Graphics 780", "UHD Graphics 790", "UHD Graphics 800", "UHD Graphics 810", "UHD Graphics 820", "UHD Graphics 830", "UHD Graphics 840", "UHD Graphics 850", "UHD Graphics 860", "UHD Graphics 870", "UHD Graphics 880", "UHD Graphics 890", "UHD Graphics 900", "UHD Graphics 910", "UHD Graphics 920", "UHD Graphics 930", "UHD Graphics 940", "UHD Graphics 950", "UHD Graphics 960", "UHD Graphics 970", "UHD Graphics 980", "UHD Graphics 990", "UHD Graphics 1000", "UHD Graphics 1010", "UHD Graphics 1020", "UHD Graphics 1030", "UHD Graphics 1040", "UHD Graphics 1050", "UHD Graphics 1060", "UHD Graphics 1070", "UHD Graphics 1080", "UHD Graphics 1090", "UHD Graphics 1100", "UHD Graphics 1110", "UHD Graphics 1120", "UHD Graphics 1130", "UHD Graphics 1140", "UHD Graphics 1150", "UHD Graphics 1160", "UHD Graphics 1170", "UHD Graphics 1180", "UHD Graphics 1190", "UHD Graphics 1200", "UHD Graphics 1210", "UHD Graphics 1220", "UHD Graphics 1230", "UHD Graphics 1240", "UHD Graphics 1250", "UHD Graphics 1260", "UHD Graphics 1270");
            default:
                return "";
        }
    }

    private Keyboard.Layout randomKeyboardLayout() {
        switch (rand.nextInt(3)) {
            case 0:
                return Keyboard.Layout.QWERTY;
            case 1:
                return Keyboard.Layout.QWERTZ;
            default:
                return Keyboard.Layout.AZERTY;
        }
    }

    private Screen.Panel randomScreenPanel() {
        switch(randomInt(1, 2)) {
            case 1:
                return Screen.Panel.IPS;
            default:
                return Screen.Panel.OLED;
        }
    }

    private String randomLaptopBrand() {
        return randomStringFromSet("Apple", "Dell", "Lenovo", "Microsoft", "Asus");
    }

    private String randomLaptopName(String brand) {
        switch (brand) {
            case "Apple":
                return randomStringFromSet("Macbook Air", "Macbook Pro", "Macbook Pro 16");
            case "Dell":
                return randomStringFromSet("XPS 13", "XPS 15", "XPS 17");
            case "Lenovo":
                return randomStringFromSet("Thinkpad X1 Carbon", "Thinkpad X1 Yoga", "Thinkpad X1 Extreme");
            case "Microsoft":
                return randomStringFromSet("Surface Book 2", "Surface Laptop 3", "Surface Pro 7");
            case "Asus":
                return randomStringFromSet("Zenbook", "Vivobook", "TUF Gaming");
            default:
                return "";
        }
    }


    // Helper functions of random generators
    private String randomStringFromSet(String ... a) {
        int n = a.length;
        if (n == 0) {
            return "";
        }
        return a[rand.nextInt(n)];
    }

    private int randomInt(int min, int max) {
        return min + rand.nextInt(max - min + 1);
    }

    private double randomDouble(double min, double max) {
        return min + rand.nextDouble() * (max - min);
    }

    private float randomFloat(float min, float max) {
        return min + rand.nextFloat() * (max - min);
    }

    private String randomID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    private Timestamp timestampNow() {
        Instant now = Instant.now();
        // Timestamp from com.google.protobuf
        return Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();
    }

    // main
    public static void main(String[] args) {
        Generator generator = new Generator();

        Laptop laptop = generator.NewLaptop();

        System.out.println(laptop);
    }
}
