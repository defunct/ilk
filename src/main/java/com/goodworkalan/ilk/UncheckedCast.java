package com.goodworkalan.ilk;

public class UncheckedCast<T>
{
    @SuppressWarnings("unchecked")
    public T cast(Object object) 
    {
        return (T) object;
    }
}
