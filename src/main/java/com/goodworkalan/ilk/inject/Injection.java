package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.IlkReflect;

/**
 * Used in combination with a stack maintained in thread local storage, this
 * stack element class represents an invocation of the injector for a single
 * thread of execution.
 * 
 * @author Alan Gutierrez
 */
class Injection {
    /** The injection depth. */
    public int injectionDepth;
    
    /** The number of parent levels locked for scope update. */
    public int lockHeight;
    
    public Queue<Ilk.Box> unset = new LinkedList<Ilk.Box>();
    
    public Queue<IlkReflect.Reflector> reflectors = new LinkedList<IlkReflect.Reflector>();

    public boolean setting;

    /**
     * The temporary scope where instances are kept while they are being
     * constructed so that partially injected instances are not visible to other
     * threads.
     */
   public final Map<Class<? extends Annotation>, Map<List<Object>, Ilk.Box>> scopes = new HashMap<Class<? extends Annotation>, Map<List<Object>, Ilk.Box>>();
}
