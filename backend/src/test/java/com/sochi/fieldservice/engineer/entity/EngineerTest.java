package com.sochi.fieldservice.engineer.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EngineerTest {

    @Test
    void onCreateShouldInitializeCreatedAt() {
        Engineer engineer = new Engineer();
        assertNull(engineer.getCreatedAt());

        engineer.onCreate();

        assertNotNull(engineer.getCreatedAt());
    }
}
