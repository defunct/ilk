package com.goodworkalan.ilk.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import com.goodworkalan.ilk.Ilk;

/**
 * Create an object input stream stream that verifies the type an object that
 * against a super type token written with the object.
 * 
 * @author Alan Gutierrez
 */
public class IlkInputStream extends ObjectInputStream {
    /**
     * Wrap the given object input stream with an object input stream that
     * records super type tokens.
     * 
     * @param in
     *            The input output stream.
     * @throws IOException
     *             If an I/O error occurs.
     */
    public IlkInputStream(InputStream in) throws IOException {
        super(in);
    }

    /**
     * Read an object of the type indicated by the given super type token from
     * the object input stream and verify
     * 
     * @param <T>
     *            The type of object to write.
     * @param ilk
     *            The super type token of the object to write.
     * @return T An object read from the input stream.
     * @throws IOException
     *             If the super type token for the object was not written to the
     *             object input stream or if an other I/O error occurs.
     * @throws ClassNotFoundException
     *             If the class of a serialized object cannot be found.
     * @exception ClassCastException
     *                If the object written is not of the type indicated by the
     *                super type token.
     */
    public <T> T readObject(Ilk<T> ilk) throws IOException, ClassNotFoundException {
        Object object = readObject();
        if (!(object instanceof Ilk.Box)) {
            throw new IOException("Object Ilk not recorded.");
        }
        return ((Ilk.Box) object).cast(ilk);
    }
}
