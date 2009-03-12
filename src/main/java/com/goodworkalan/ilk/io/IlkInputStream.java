package com.goodworkalan.ilk.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import com.goodworkalan.ilk.Ilk;

// TODO Document.
public class IlkInputStream extends ObjectInputStream
{
    // TODO Document.
    public IlkInputStream(InputStream in) throws IOException
    {
        super(in);
    }
    
    // TODO Document.
    public <T> T readObject(Ilk<T> ilk) throws IOException, ClassNotFoundException
    {
        Object object = readObject();
        if (!(object instanceof Ilk.Pair))
        {
            throw new IOException("Object Ilk not recorded.");
        }
        return ((Ilk.Pair) object).cast(ilk);
    }
}
