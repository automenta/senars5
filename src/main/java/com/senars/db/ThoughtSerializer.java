package com.senars.db;

import com.google.gson.Gson;
import com.senars.core.Thought;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;

public class ThoughtSerializer implements Serializer<Thought> {
    private final Gson gson = new Gson();

    @Override
    public void serialize(DataOutput2 out, Thought value) throws IOException {
        String json = gson.toJson(value);
        out.writeUTF(json);
    }

    @Override
    public Thought deserialize(DataInput2 input, int available) throws IOException {
        String json = input.readUTF();
        return gson.fromJson(json, Thought.class);
    }
}
