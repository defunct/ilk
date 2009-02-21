package com.goodworkalan.ilk.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.UncheckedCast;

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
        if (!(object instanceof Ilk.Key))
        {
            throw new IOException("Object Ilk not recorded.");
        }
        Ilk.Key key = (Ilk.Key) object;
        if (!key.equals(ilk.key))
        {
            throw new ClassCastException();
        }
        return new UncheckedCast<T>().cast(readObject());
    }
}
