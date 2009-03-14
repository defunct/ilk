package com.goodworkalan.ilk.io.api;

import static org.testng.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.testng.annotations.Test;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.io.IlkInputStream;
import com.goodworkalan.ilk.io.IlkOutputStream;

public class IlkStreamTest
{
    @Test
    public void stream() throws IOException, ClassNotFoundException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        IlkOutputStream out = new IlkOutputStream(bytes);
        out.writeObject(new Ilk<String>() { }, "Hello, World!");
        out.close();
        
        IlkInputStream in = new IlkInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(in.readObject(new Ilk<String>() { }), "Hello, World!");
    }
    
    @Test(expectedExceptions=IOException.class)
    public void notAPair() throws IOException, ClassNotFoundException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        IlkOutputStream out = new IlkOutputStream(bytes);
        out.writeObject("Hello, World!");
        out.close();
        
        IlkInputStream in = new IlkInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(in.readObject(new Ilk<String>() { }), "Hello, World!");
    }
}
