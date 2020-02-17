package com.affirm.android.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class AddressSerializer implements JsonSerializer<AbstractAddress> {

    @Override
    public JsonElement serialize(AbstractAddress src,
                                 Type typeOfSrc,
                                 JsonSerializationContext context) {
        return context.serialize(src, src.getClass());
    }
}