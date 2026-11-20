package com.trackergastos.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class GastoTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Gasto getGastoSample1() {
        return new Gasto().id(1L).descripcion("descripcion1");
    }

    public static Gasto getGastoSample2() {
        return new Gasto().id(2L).descripcion("descripcion2");
    }

    public static Gasto getGastoRandomSampleGenerator() {
        return new Gasto().id(longCount.incrementAndGet()).descripcion(UUID.randomUUID().toString());
    }
}
