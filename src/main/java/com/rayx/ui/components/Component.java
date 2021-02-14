package com.rayx.ui.components;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Component {

    private static final AtomicInteger counter = new AtomicInteger();

    private final int id;

    public Component() {
        id = counter.getAndIncrement();
    }

    public abstract void render();

    public int getId() {
        return id;
    }

}
