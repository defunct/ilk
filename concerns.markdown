---
layout: default
title: Ilk Concerns and Decisions
---


# Ilk Concerns and Decisions

Concerns and decisions about Ilk.

## Missing

 * Eager singletons.
 * Proxy constructors.

## Type Variables

Can I forgo the construction of new classes by using type variables directly,
plucking them from methods, then using them to do assignments? Am I throwing
away type safety, or will isAssignableFrom do the magic?

## Handling Providers

Considering how to get hands onto a provider, so that providers need to become
some very clever form of instance. You can't get the provider directly, it
seems, I've decided that below. What is the right way to do this?

Problem is the case of the heterogenous containers, wanting to include
participants used optionally. Does that make sense? Don't we know, generally,
when we need to open an `EntityManager`. I'm pretty sure that multi-bindings
generally don't allow for this great degree of mix or match. Maybe I can
annotate the class with a way to indicate which multi-binding map to use?

## Which Provider Do You Get?

When you ask for a provider, you don't get the provider type you specified, you
get a proxy that will cache the provider result in a scope. Don't count on
getting the specific provider bound to the interface.

## Provider Setter/Method Injection

Providers can only be constructed through constructor injection. Setters are
never invoked. This may be something to provide in the future, but it would mean
creating a stack of Injection instances, so that a full injection could be
performed to create the Provider.

I've decided that providers get build through injection, using that injection
stack, starting a new injection for the Provider. The Provider binding will add
a binding for the Provider implementation to the bindings, so users explicity
defining Providers becomes a "if it hurts, don't do it sort of thing". It might
not hurt, just that redefining means that the user is going to overwrite what is
already there.

## Caching Failure

Is there a way to cache them if they are correct?  (Probably not since it is
really a graph, and sticking objects in the cache is bad if the graph is bad,
unless you create a queue of good temporary scopes and that queue gets filled.
(No, all constructed, then all setter injected, we would have to check that the
setters are good.) You really shouldn't throw exceptions during injection, so
scratch that, and a singleton can probably initialize twice.

## Setter Injection

Inject setters, or just invoke methods using inject?

## Verbosity

When you use ilk, you're always adding an extra parameter. That is how it works.
You need to pass the extra parameter and you cannot contain it in another
object, since you need the type variables.

## Size

My primary concern is the size of Ilk. It turns out that, Ilk itself cannot be
as small as I would like. Ideally, it would be a tiny library, under 5k, but
once you add the bulk that comes from having to define implementations for
ParametrizedType and WildcardType, it is hard to come back down to a reasonable
size. It doesn't make sense to split the `Ilk.Box` out of `Ilk`. There is not
much room for savings, so employing `Ilk` has a 20k cost.

It does a lot, however. Which is why Ilk Inject is only 93k with all
dependencies.

Yet, there are applications like Stash, where things like assignability are not
needed, only equality. (But it used anyway, since Stash uses boxes, but we could
just follow the turnstile rule. Hmmm... What is that? That is, we have a
turnstyle, so we know what goes in and what comes out.)

## Assignability

I was using assignability naiavely before, and changed the matching to equality.

Does it make sense to create a situation where you can bind an implementation to
an interface, then if you ask for an interface that is assignable from that
bound interface, you win? The qustion becomes, how do you resolve specificity?

You might have bound `List` to an `ArrayList` in your most childish injector,
but have a `Collection` bound in the parent. If someone requests a `Collection`,
which one will bind? What if you have a class that can be assigned from two
different types that are not assignable to each other, which one wins?

Obviously, we could consider everything, then choose the ones based on rules
baed on proximity and childishness. 

The initial naive attempt at assignability blew up when I wanted to bind a Paste
controller to `Object`. Bind to `Object` and have an exact match win is would
sort that out.

What I'm concerned about now is Stencil, developing a general purpuse stencil
that takes a `Collection` say, but having someone put a `List` in the context.
The shouldn't have to know that the Stencil needs to have things rebound.

Sun May 23 11:29:56 CDT 2010

    Once you've got for assignability, you need acutalization. Once you have
    actualization, you might as well do variable assignment, since it won't do
    well to have that external. Thus, it is hard to make this a tiny library and
    the word "token" in super type token is misleading. Ilk will do a lot more.

    Ilk makes the downstream libraries smaller. Ilk Inject is thread-safe,
    constructor and setter dependency injection for under 90K. Ilk Assocation is
    a mere 1.8K to map object type, assignability or annotations to other
    objects, so think of handlers or filters based on type in your application
    framework.

    Reflection split out nicely, but try as I might, I couldn't get it down to
    the size of what I consider a "token", under 10K. Ilk is now hovering around
    18K. There is little that I can take away, but nothing to add. Using the
    variable substitution I'm able to derive any sort of type necessary.

    I was working to keep this small to keep Ilk Inject small. I tried to make
    this minimal, but now I'm adding a few bits and pieces to round it out.

    I believe the upper limit on this library for size is 20K.

## From Things

 * Find a way to build IlkReflect at the same time as Ilk so that the Javadoc
   references are correct. - Javadoc is the same, but the artifact is split into
   two jars.
 * Could you end up with wildcard bindings? - Thinking about this in terms for
   how to create VendorProvider.
 * ComfortIOException is outgoing, or shoud be a checked exception? 
 * Owner object should use same qualifier as nested object.
 * Ilk Tests as Arguments
 ** Coverage for Ilk.Box.
 ** Complete coverage of Types.equals.
 * Reconsider How You Might Provide an EntityManagerProvider.
 * Private LinkedList Version of getActualTypes - Probably smaller overall.
 * Use the Provider Instance Instead of VendorProvider.
