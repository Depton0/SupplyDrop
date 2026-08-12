package com.depton.supplydrop.util;

import java.util.concurrent.ThreadLocalRandom;

public class BoundingBox2D {

    //bounding coordinates
    private final double minX;
    private final double minZ;
    private final double maxX;
    private final double maxZ;

    //construct 2d bounding box
    public BoundingBox2D(double x1, double z1, double x2, double z2) {
        this.minX = Math.min(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxZ = Math.max(z1, z2);
    }

    //check coordinate inside box
    public boolean contains(double x, double z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    //pick random x within bounds
    public int getRandomX() {
        int min = (int) Math.floor(minX);
        int max = (int) Math.floor(maxX);
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    //pick random z within bounds
    public int getRandomZ() {
        int min = (int) Math.floor(minZ);
        int max = (int) Math.floor(maxZ);
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    //getters for bounds
    public double getMinX() { return minX; }
    public double getMinZ() { return minZ; }
    public double getMaxX() { return maxX; }
    public double getMaxZ() { return maxZ; }
}
