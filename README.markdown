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

With Ilk, you do not have to use @SuppressWarnings("unchecked") in your code.
You can safely cast to a generic type.

<pre>
Ilk.Pair pair = new Ilk&lt;List&lt;String&gt;&gt;() { }.pair(new ArrayList&lt;String&gt;());

List&lt;String&gt; list = pair.cast(new Ilk&lt;List&lt;String&gt;&gt;() { });
</pre>

Motivation
----------

Creating a super type token to pull into projects encourages type-safe thinking.
