package com.lightbend.model.java;

import com.lightbend.modelserver.support.java.ModelToServe;

import java.util.Optional;

/**
 * Created by boris on 7/14/17.
 */
public interface ModelFactory {
    Optional<Model> create(ModelToServe descriptor);
    Model restore(byte[] bytes);
}
