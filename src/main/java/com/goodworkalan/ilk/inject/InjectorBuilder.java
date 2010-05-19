package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;
import javax.inject.Scope;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.association.IlkAssociation;

public class InjectorBuilder {
    /** The map of types to instructions on how to provide them. */
    private final Map<Class<? extends Annotation>, IlkAssociation<Stipulation>> stipulations = new HashMap<Class<? extends Annotation>, IlkAssociation<Stipulation>>();
    
    /** The scopes to create in the injector. */
    private final Map<Class<? extends Annotation>, Map<QualifiedType, Ilk.Box>> scopes = new HashMap<Class<? extends Annotation>, Map<QualifiedType, Ilk.Box>>();
    
    private final Injector parent;
    
    /**
     * Create a constructor builder and call its build method.
     */
    public InjectorBuilder() {
        this(null);
    }
    
    InjectorBuilder(Injector parent) {
        this.parent = parent;
        this.stipulations.put(NoQualifier.class, new IlkAssociation<Stipulation>(false));
    }
    
    public void module(InjectorBuilder module) {
        module.buildificate();
        consume(module);
    }
    
    public void consume(InjectorBuilder newInjector) {
        consume(newInjector.stipulations);
        scopes.putAll(newInjector.scopes);
    }
    
    void consume(Map<Class<? extends Annotation>, IlkAssociation<Stipulation>> stipulations) {
        for (Map.Entry<Class<? extends Annotation>, IlkAssociation<Stipulation>> entry : stipulations.entrySet()) {
            IlkAssociation<Stipulation> stipulationByIlk = stipulations.get(entry.getKey());
            if (stipulationByIlk == null) {
                stipulationByIlk = new IlkAssociation<Stipulation>(false);
            }
            stipulationByIlk.addAll(entry.getValue());
        }
    }
    
    /**
     * Bind the given interface class annotated with the given qualifier to the
     * given implementation class in the given scope. Neither the interface
     * class nor the implementation class may be null. If the qualifier is null,
     * then the binding is used for unqualified uses of the given interface. If
     * the scope is null, a new instance of the implementation class is
     * constructed for each use of the interface.
     * 
     * @param <T>
     *            The type of the interface.
     * @param interfaceClass
     *            The class of the interface.
     * @param qualifier
     *            The qualifier or null for unqualified.
     * @param implementationClass
     *            The implementation class.
     * @param scope
     *            The scope or null to reuse.
     */
    public <I, C extends I> void implementation(Ilk<C> implementation, Ilk<I> type, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        bind(new NewImplementationBuilder(provider(implementation).key, type.key, implementation.key), type, qualifier, scope);
    }
    
    <T> Ilk<BuilderProvider<T>> provider(Ilk<T> ilk) {
        return new Ilk<BuilderProvider<T>>(ilk.key) {};
    }
    
    <I> void bind(Builder builder, Ilk<I> type, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        if (qualifier == null) {
            qualifier = NoQualifier.class;
        }
        IlkAssociation<Stipulation> stipulationByIlk = stipulations.get(qualifier);
        if (stipulationByIlk == null) {
            stipulationByIlk = new IlkAssociation<Stipulation>(false);
            stipulations.put(qualifier, stipulationByIlk);
        }
        if (scope == null) {
            scope = NoScope.class;
        }
        stipulationByIlk.assignable(type.key, new Stipulation(builder, scope));
    }

    /**
     * Bind the given interface class annotated with the given qualifier to the
     * given provider in the given scope. Neither the interface class nor the
     * provider class may be null. If the qualifier is null, then the binding is
     * used for unqualified uses of the given interface. If the scope is null, a
     * new instance of the implementation class is constructed for each use of
     * the interface.
     * 
     * @param <T>
     *            The type of the interface.
     * @param interfaceClass
     *            The class of the interface.
     * @param qualifier
     *            The qualifier or null for unqualified.
     * @param implementationClass
     *            The implementation class.
     * @param scope
     *            The scope or null to reuse.
     */
    public <I, C extends I> void provider(Provider<C> provider, Ilk<I> type, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        bind(new ProviderBuilder<I, C>(type, provider), type, qualifier, scope);
    }
    
    public <I> void provider(Ilk<? extends Provider<? extends I>> provider, Ilk<I> type, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        bind(new NewProviderBuilder(type.key, type.key), type, qualifier, scope);
    }

    /**
     * Bind the given interface class annotated with the given qualifier to the
     * given instance. Neither the interface class nor the implementation class
     * may be null. If the qualifier is null, then the binding is used for
     * unqualified uses of the given interface. If the scope is null, a new
     * instance of the implementation class is constructed for each use of the
     * interface.
     * 
     * @param <T>
     *            The type of the interface.
     * @param interfaceClass
     *            The class of the interface.
     * @param qualifier
     *            The qualifier or null for unqualified.
     * @param implementationClass
     *            The implementation class.
     * @param scope
     *            The scope or null to reuse.
     */
    public <T> void instance(T instance, Ilk<T> type, Class<? extends Annotation> qualifier) {
        bind(new InstanceProvider<T>(type, instance), type, qualifier, null);
    }

    public void scope(Class<? extends Annotation> scope) {
        scope(scope, null);
    }
    
    public void scope(Class<? extends Annotation> scope, Ilk.Box values) {
        if (scope.getAnnotation(Scope.class) == null) {
            throw new IllegalArgumentException();
        }
        scopes.put(scope, values == null ? new HashMap<QualifiedType, Ilk.Box>() : values.cast(Injector.SCOPE_TYPE));
    }
    
    public Injector newInjector() {
        return new Injector(parent, stipulations);
    }
    
    /**
     * Wrap the given class in an ilk.
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
    
    void buildificate() {
        build();
    }
}
