Ilk
===

An implementation of super type tokens (Gafter's Gadget). 

* [Issues and Tasks](http://blogometer.com/trac/incubator)
* [Source Code](http://github.com/bigeasy/ilk/tree/master)
* API Javadoc [public](http://curlybraces.org/documentation/ilk/api/public/) and
  [private](http://curlybraces.org/documentation/ilk/api/private/)
* Alan Gutierrez [alan@blogometer.com](mailto:alan@blogoemter.com)

Purpose
-------

Ilk is my implementation of super type tokens, for inclusion in my projects, so
that I don't have to borrow an implementation from Guice.

<pre>
Ilk.Key mapKey = new Ilk&lt;Map&lt;String, List&lt;String&gt;&gt;() {};

assertTrue(mapKey.get(1).getKey().equals(new Ilk&lt;List&lt;String&gt;&gt;(){}.key));
assertFalse(mapKey.get(1).getKey().equals(new Ilk&lt;List&lt;Integer&gt;&gt;(){}.key));
</pre>

<pre>
IlkOutputStream out = new IlkOutputStream(new FileOutputStream("ilky.data"));
out.writeObject(new Ilk&lt;List&lt;String&gt;&gt;(){}, new List&lt;String&gt;());
out.close();

IlkInputStream in = new IlkInputStream(new FileInputSTream("ilky.data"));
List&lt;String&gt; strings = in.readObject(new Ilk&lt;List&lt;String&gt;&gt;(){});
in.close();
</pre>

At this point, it is hard to imagine how super type tokens could be used to
create meaninful containers. Ordered containers don't make sense, because it is
hard to imagine how to order disparate types in relation to each other.

A container that groups items by type can actually be useful however.  One could
imagine a hash that keyed by Ilk returning lists of objects of that Ilk. A
database is pretty much that.

A particularly useful container is a stash. A map keyed by a combination of type
and any keyed object. (Duh!) Make Stash generic!

<pre>
import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.stash.Stash;

public class Stashed
{
    private final static Stash.Key STRING_LIST = new Stash.Key();

    public static void setStringList(Stash stash, List&lt;String&gt; strings)
    {
        stash.put(new Ilk&lt;List&lt;String&gt;&gt;(){}, STRING_LIST, new List&lt;String&gt;());
    }

    public List&lt;String&gt; getStringList(Stash stash)
    {
        return stash.get(new Ilk&lt;List&lt;String&gt;&gt;() { }, STRING_LIST);
    }
}
</pre>

Motivation
----------

Creating a super type token to pull into projects encourages type-safe thinking.
