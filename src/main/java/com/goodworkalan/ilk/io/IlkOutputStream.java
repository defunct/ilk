package com.goodworkalan.ilk.io;

import java.io.IOException;
import java.io.ObjectOutputStream;

import com.goodworkalan.ilk.Ilk;

public class IlkOutputStream extends ObjectOutputStream
{
    public IlkOutputStream(ObjectOutputStream out) throws IOException
    {
        super(out);
    }
    
    public <T> void writeObject(Ilk<T> ilk, T object) throws IOException
    {
        writeObject(ilk.key);
        writeObject(object);
    }
}
