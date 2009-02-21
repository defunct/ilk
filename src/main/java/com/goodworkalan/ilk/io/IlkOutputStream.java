package com.goodworkalan.ilk.io;

import java.io.IOException;
import java.io.ObjectOutputStream;

import com.goodworkalan.ilk.Ilk;

// TODO Document.
public class IlkOutputStream extends ObjectOutputStream
{
    // TODO Document.
    public IlkOutputStream(ObjectOutputStream out) throws IOException
    {
        super(out);
    }
    
    // TODO Document.
    public <T> void writeObject(Ilk<T> ilk, T object) throws IOException
    {
        writeObject(ilk.key);
        writeObject(object);
    }
}
