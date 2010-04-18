package com.goodworkalan.ilk.io;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import com.goodworkalan.ilk.Ilk;

/**
 * Create an object output stream that records a super type token for each
 * object written.
 * 
 * @author Alan Gutierrez
 */
public class IlkOutputStream extends ObjectOutputStream {
    /**
     * Wrap the given output stream with an object output stream that records
     * super type tokens.
     * 
     * @param out
     *            The output stream.
     * @throws IOException
     *             If an I/O error occurs.
     */
    public IlkOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    /**
     * Write the given super type token and an instance of the type indicated by
     * the super type token.
     * 
     * @param <T>
     *            The type of object to write.
     * @param ilk
     *            The super type token of the object to write.
     * @param object
     *            The object to write.
     * @throws IOException
     *             If an I/O error occurs.
     */
    public <T> void writeObject(Ilk<T> ilk, T object) throws IOException {
        writeObject(ilk.box(object));
    }
}
