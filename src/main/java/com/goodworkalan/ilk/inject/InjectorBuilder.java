package com.goodworkalan.ilk.inject;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Provider;
import javax.inject.Scope;
import javax.inject.Singleton;

import com.goodworkalan.ilk.Ilk;

/**
 * A builder for injectors.
 * <p>
 * This builder employs a minimal builder interface that provides only a single
 * method for each type of binding. The bindings must be provided as {@link Ilk}
 * super type tokens. For unqualified bindings, pass null as the qualifier
 * annotation. For unscoped bindings, pass null for the scope annotation.
 * <p>
 * This builder can be employed as a module by creating a subclass and
 * implementing the {@link #build() build} method, then importing the module
 * into another injector builder using the {@link #module(InjectorBuilder)
 * module} method. The subclass will have access to static the static
 * {@link #ilk(Class) ilk} helper method that makes binding definitions much
 * more succinct when only classes, not parameterized types, are bound. This
 * module pattern makes for binding definitions that are quite legible.
 * 
 * @author Alan Gutierrez
 */
public class InjectorBuilder {
    /** The parent of this injector. */
    private final Injector parent;

    /** The map of types to instructions on how to provide them. */
    private final Map<Class<? extends Annotation>, Map<Ilk.Key, Vendor<?>>> builders = new HashMap<Class<? extends Annotation>, Map<Ilk.Key, Vendor<?>>>();
    
    /** The scopes to create in the injector. */
    private final Map<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>> scopes = new HashMap<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>>();

    /**
     * Create a root injector builder.
     * <p>
     * The root injector builder will start by defining a scope for the
     * <code>Singleton</code> scope annotation and one for the
     * <code>InjectorScoped</code> annotation. Child injectors will have a scope
     * defined for the <code>InjectorScoped</code> annotation, but none for the
     * <code>Singleton</code> scope annotation. Unless you decide to define a
     * <code>Singleton</code> in a child injector, the singleton scope will hold
     * singleton instances for the injector and all child injectors.
     * <p>
     * The injector builder will automatically create an internal binding for
     * for injectors, which is of no concern unless you attempt to bind a
     * different injector provider.
     */
    public InjectorBuilder() {
        this(null);
    }

    /**
     * Create an injector builder that builder an inejector that is a child of
     * the given injector builder. This injector builder will always define an
     * scope for the <code>InjectorScoped</code> scope annotation.
     * 
     * @param parent
     *            The parent injector.
     */
    InjectorBuilder(Injector parent) {
        this.parent = parent;
        scope(InjectorScoped.class);
        builders.put(NoQualifier.class, new HashMap<Ilk.Key, Vendor<?>>());
    }

    private final Map<Package, Ilk.Reflector> reflectors = new HashMap<Package, Ilk.Reflector>();

    /**
     * Build package private implementations and <code>Provider&lt;T&gt;</code>
     * implementations defined in the package of the given <code>reflect</code>
     * implementation with the given <code>reflect</code>.
     * <p>
     * The given reflect will only be used to reflect upon classes specified
     * in the bindings in this injector builder. It cannot be used to instanciate
     * arbitrary package private classes in the package.
     * 
     * @param reflect The reflect implementation.
     */
    public void reflector(Ilk.Reflector reflector) {
        reflectors.put(reflector.getClass().getPackage(), reflector);
    }

    /**
     * Add the given injector builder as a module. This will invoke the build
     * method of the given injector builder to define bindings and scopes in the
     * injector builder, then the injector builder will be consumed. Any
     * bindings or scopes defined outside of the build method of the given
     * module are also consumed.
     * <p>
     * The given module is copied prior to invoking build, then reset and
     * restored from the copy after invoking build and consuming the module. In
     * this way, the module can be used multiple times, each time build will be
     * rerun from its initial state. This is a defensive measure.
     * <p>
     * Note that the package specific reflectors are not copied.
     * 
     * @param module
     *            The module to build and consume.
     */
    public void module(InjectorBuilder module) {
        InjectorBuilder copy = new InjectorBuilder();
        copy.consume(module);
        module.build();
        consume(module);
        module.builders.clear();
        module.scopes.clear();
        module.consume(copy);
    }

    /**
     * Expand the definition of the this injector builder by copying the
     * definitions from the given injector builder. This injector builder will
     * subsequently define all of the bindings and scopes in the given injector
     * builder. This injector builder will have its own copy of all of the
     * definitions so that further changes to the given injector builder will
     * not effect this injector builder.
     * 
     * @param newInjector
     *            The injector builder to copy.
     */
    public void consume(InjectorBuilder newInjector) {
        for (Map.Entry<Class<? extends Annotation>, Map<Ilk.Key, Vendor<?>>> entry : newInjector.builders.entrySet()) {
            Map<Ilk.Key, Vendor<?>> associations = this.builders.get(entry.getKey());
            if (associations == null) {
                associations = new HashMap<Ilk.Key, Vendor<?>>();
                this.builders.put(entry.getKey(), associations);
            }
            associations.putAll(entry.getValue());
        }
        for (Map.Entry<Class<? extends Annotation>, ConcurrentMap<List<Object>, Ilk.Box>> entry : newInjector.scopes.entrySet()) {
            ConcurrentMap<List<Object>, Ilk.Box> scope = this.scopes.get(entry.getKey());
            if (scope == null) {
                scope = new ConcurrentHashMap<List<Object>, Ilk.Box>();
                this.scopes.put(entry.getKey(), scope);
            }
            scope.putAll(entry.getValue());
        }
    }

    public <I> Vendor<I> bind(Vendor<I> vendor) {
        Map<Ilk.Key, Vendor<?>> builderByIlk = builders.get(vendor.qualifier);
        if (builderByIlk == null) {
            builderByIlk = new HashMap<Ilk.Key, Vendor<?>>();
            builders.put(vendor.qualifier, builderByIlk);
        }
        builderByIlk.put(vendor.ilk.key, vendor);
        return vendor;
    }

    /**
     * Bind the type specified by the given super type token annotated with the
     * given qualifier to the implementation specified by the given
     * implementation super type token in the given scope. Neither the bound
     * super type token nor the implementation super type token may be null. If
     * the qualifier is null, then the binding is used for unqualified uses of
     * the given interface. If the scope is null, a new instance of the
     * implementation class is constructed for each use of the interface.
     * 
     * @param <I>
     *            The type to bind.
     * @param implementation
     *            The super type token of the implementation to bind to the
     *            type.
     * @param ilk
     *            The super type token of the type to bind.
     * @param qualifier
     *            The qualifier or null for unqualified.
     * @param scope
     *            The scope or null to build a new instance every time.
     */
    public <I> Vendor<I> implementation(Ilk<? extends I> implementation, Ilk<I> ilk, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        return bind(new ImplementationVendor<I>(ilk, implementation.key, qualifier, scope, reflectors.get(implementation.key.rawClass.getPackage())));
    }

    /**
     * Bind the type specified by the given super type token annotated with the
     * given qualifier to the given provider in the given scope. Neither the
     * bound super type token nor the provider may be null. If the qualifier is
     * null, then the binding is used for unqualified uses of the given
     * interface. If the scope is null, a new instance of the implementation
     * class is constructed for each use of the interface.
     * 
     * @param <I>
     *            The type to bind.
     * @param provider
     *            The provider to bind to the type.
     * @param ilk
     *            The super type token of the type to bind.
     * @param qualifier
     *            The qualifier or null for unqualified.
     * @param scope
     *            The scope or null to build a new instance every time.
     */
    public <I> Vendor<I> provider(Provider<? extends I> provider, Ilk<I> ilk, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        return bind(new ProviderInstanceVendor<I>(ilk, provider, qualifier, scope));
    }

    /**
     * Bind the type specified by the given super type token annotated with the
     * given qualifier to the given provider in the given scope. Neither the
     * bound super type token nor the provider super type token may be null. If
     * the qualifier is null, then the binding is used for unqualified uses of
     * the given interface. If the scope is null, a new instance of the
     * implementation class is constructed for each use of the interface.
     * 
     * @param <I>
     *            The type to bind.
     * @param provider
     *            The provider to bind to the type.
     * @param ilk
     *            The super type token of the type to bind.
     * @param qualifier
     *            The qualifier or null for unqualified.
     * @param scope
     *            The scope or null to build a new instance every time.
     */
    public <I> Vendor<I> provider(Ilk<? extends Provider<? extends I>> provider, Ilk<I> ilk, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        return bind(new ProviderVendor<I>(provider, ilk, qualifier, scope, reflectors.get(provider.getClass().getPackage())));
    }

    /**
     * Bind the the type specified by the given super type token annotated with
     * the given qualifier to the given instance. Neither the bound super type
     * token nor the instance may be null. If the qualifier is null, then the
     * binding is used for unqualified uses of the given interface.
     * 
     * @param <I>
     *            The type to bind.
     * @param instance
     *            The instance to bind to the type.
     * @param ilk
     *            The super type token of the type to bind.
     * @param qualifier
     *            The qualifier or null for unqualified.
     */
    public <I> Vendor<I> instance(I instance, Ilk<I> type, Class<? extends Annotation> qualifier) {
        return bind(new InstanceVendor<I>(type, instance, qualifier));
    }

    /**
     * Define a scope in the injector using the given scope annotation. The
     * scope will be used to store constructed values in the injector.
     * <p>
     * Scopes {@link #scope(Class, com.goodworkalan.ilk.Ilk.Box) can be
     * persisted} and laster restored
     * {@link #scope(Class, com.goodworkalan.ilk.Ilk.Box) restored} in order to
     * provide scopes that can outlive their injectors.
     * 
     * @param scope
     *            The scope annotation.
     */
    public final void scope(Class<? extends Annotation> scope) {
        scope(scope, null);
    }

    /**
     * Define a scope in the injector using the that stores instances for types
     * bound to the given scope annotation.
     * <p>
     * This minimal interface to the scopes is meant to make them easier to work
     * with. Rather than defining custom scopes using a scope interface, simply
     * request a that a scope be provided for a scope annotation you'd like to
     * bind to in your application via the injector builder. This will create a
     * scope container in the injector where the instances bound to the scope
     * will be stored.
     * <p>
     * When an instance bound to a scope annotation is created, the injector
     * will look among its scope containers for a the scope container associated
     * with the scope annotation as defined by the injector builder. If the
     * scope was not defined for the injector, the injector will ascend the
     * hiearchy of injectors, looking among the scope containers of each parent
     * injector for a scope until a scope container is found. If a scope cannot
     * be found, an exception is thrown.
     * <p>
     * This scope hierarchy up allows applications to isolate scopes among
     * children, by defining the scope for each child. It can also create shared
     * scopes by leaving a scope undefined in the children that is defined in
     * the parent.
     * <p>
     * The {@link Singleton} scope is implemented as a scope that automatically
     * defined for every root injector, that should not be defined for child
     * injectors, so that all children will use the scope container for the
     * <code>Singleton</code> annotation defined in the root injector.
     * <p>
     * The {@link InjectorScoped} scope is automatically defined for every
     * injector, so that every injector has their own scope container for
     * <code>InjectorScoped</code>. An injector will always find a scope
     * container for <code>InjectorScoped</code> among its scopes, so an
     * instance bound to <code>InjectorScoped</code> will be unique to the
     * injector that created it.
     * <p>
     * Scopes can be persisted by calling the {@link Injector#scope
     * Injector.scope} method to retrieve an opaque, but serializable, object
     * that contains the values in scope. That value is an {@link Ilk.Box} which
     * contains all of the scope collections. The scopes can be restored by
     * providing opaque collection to the <code>scope</code> method of a new
     * injector builder, but <strong>only after</strong> the injector that
     * created the opaque scopes collection is no longer in use. Two injectors
     * cannot share scope collections, so please be careful when persisting
     * scopes.
     * <p>
     * This opaque scopes collection used to implement session scopes for
     * stateless protocols, such as a session scope for a web application. The
     * only real interface exposed by the box is the serializable interface. You
     * can hand this serializable blob off to your session storage, or write it
     * to a byte stream yourself. You may have an alternate serialization
     * library that will will do the right thing via reflection. The contents of
     * the opaque scopes collections are meant to be a black box.
     * <p>
     * If you do use Java serialization, you can only store objects in the scope
     * that also implement {@link Serializable}. If that is a problem, then you
     * can hold onto the scope in memory between injectors.
     * 
     * @param scope
     *            The scope annotation.
     * @param values
     *            The opaque collection of scope values.
     */
    public void scope(Class<? extends Annotation> scope, Ilk.Box values) {
        if (scope == null || scope.getAnnotation(Scope.class) == null) {
            throw new IllegalArgumentException();
        }
        scopes.put(scope, values == null ? new ConcurrentHashMap<List<Object>, Ilk.Box>() : values.cast(Injector.SCOPE_TYPE));
    }

    /**
     * Wrap the given class in a super type token. This is more succinct way of
     * defining classes.
     * <p>
     * Calling the {@link Ilk#Ilk(Class) class constructor} of <code>Ilk</code>
     * can be verbose.
     * <p>
     * <pre>
     * newInjector.module(new InjectorBuilder() {
     *     public void build() {
     *         bind(new Ilk&lt;StationWagon&gt;(StationWagon.class), new Ilk&lt;Car&gt;(Car.class),
     *             &#064;Inoperable, @JunkyardScope);
     *     }
     * });
     * </pre>
     * <p>
     * It is makes for easier reading with this little method.
     * <pre>
     * newInjector.module(new InjectorBuilder() {
     *     public void build() {
     *         bind(ilk(StationWagon.class), ilk(Car.class), @Inoperable, @JunkyardScope);
     *     }
     * });
     * </pre>
     * 
     * @param <T>
     *            The type.
     * @param unwrappedClass
     *            The unwrapped class.
     * @return The class wrapped in an ilk.
     */
    public static <T> Ilk<T> ilk(Class<T> unwrappedClass) { 
        return new Ilk<T>(unwrappedClass);
    }

    /**
     * Derived anonymous inner classes can use this method to populate the
     * builder taking advantage of the static helper methods.
     */
    protected void build() {
    }

    /**
     * Create a new injector using the bindings and scopes defined by this
     * injector builder.
     * 
     * @return A new injector.
     */
    public Injector newInjector() {
        return new Injector(parent, builders, scopes);
    }
}
