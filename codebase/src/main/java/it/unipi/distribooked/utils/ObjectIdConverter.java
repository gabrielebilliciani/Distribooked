package it.unipi.distribooked.utils;

import org.bson.types.ObjectId;

public class ObjectIdConverter {
    public static ObjectId convert(String id) {
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid ID format: " + id, ex);
        }
    }
}