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
 * Used with thread local storage, this class represents an invocation of the
 * injector for a single thread of execution. It tacks the injection depth and
 * gathers the newly created objects for setter injection when the constructor
 * injection call stack terminates.
 * 
 * @author Alan Gutierrez
 */
class Injection {
    /** The injection depth. */
    public int injectionDepth;
    
    /** The number of parent levels locked for scope update. */
    public int lockHeight;

    /** The queue of constructed objects that need to undergo setter injection. */
    public Queue<Ilk.Box> unset = new LinkedList<Ilk.Box>();
    
    /**
     * The parallel queue of reflectors used to perform setter injection against
     * the boxed instances in <code>unset</code>.
     */ 
    public Queue<IlkReflect.Reflector> reflectors = new LinkedList<IlkReflect.Reflector>();

    /**
     * Indicates that the injection is already in the process of setter
     * injection and the setter injection logic should not be invoked. The
     * constructor injection call stack has terminated and that subsequent call
     * stacks are setter injection call stacks.
     */
    public boolean setting;

    /**
     * The temporary scope where instances are kept while they are being
     * constructed so that partially injected instances are not visible to other
     * threads.
     */
   public final Map<Class<? extends Annotation>, Map<List<Object>, Ilk.Box>> scopes = new HashMap<Class<? extends Annotation>, Map<List<Object>, Ilk.Box>>();
}
