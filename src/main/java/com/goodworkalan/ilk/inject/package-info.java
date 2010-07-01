/**
 * A dependency injection utility class and supporting classes.
 * <p>
 * Having created a super type token and seeing some of what it could do, I created
 * a nice association class, a generic collection that mapped parameterized types
 * to an object or list of objects based on assignability. From there, creating a
 * dependency injector was short walk.
 * <p>
 * The motivations were that there was a lot about the existing dependency
 * injection frameworks that I didn't understand, and there was a lot that I didn't
 * use.
 * <p>
 * Much of what I saw was dependency injection as framework, how to build your
 * application for a container. What I wanted was dependency injection as utility.
 * I wanted to wield the sword of dependency injection and cut through
 * object graph initialization at the micro level as well as the macro level.
 * <p>
 * Dependency injection as framework seems to gear development toward offering
 * fancy features as the motivation for adoption of dependency injection. Notions
 * such as two classes being able to inject each other into their constructors. 
 * <p>
 * I find this sort of thing to be dishonest. The illusion of immutability is
 * tranparent for me and does not satisfy. I do not need the dependency injetion
 * framework to build object graphs that would be impossible to build without the
 * dependency injection framework.
 * <p>
 * For the most part, I wanted to get my hands on the injector and build something
 * with it, not look at it as a boundary, but as any other builder.
 * <p>
 * Which meant creating a library that was easy to use and documented for use from
 * both the injected and the injectee. Creating a library that was small and light
 * with an API that was small and light, so that it wouldn't be a burden to employ
 * dependency injection whereever an object graph needed to be wired up.
 * <p>
 * The most important aspects of a dependency injector are supported.
 * <p>
 * Ilk Inject is design for concurrent use, so that singleton objects can be shared
 * across threads. Object graphs that are shared through scopes are build entirely,
 * then added to the scopes, while other threads are made to wait if they are
 * attempting to obtain the objects being built.
 * <p>
 * Ilk Inject supports constructor injection and setter injection both duriong
 * construction, injecting any method or field annotated with <code>Inject</code>.
 * You can subsequently inject any public field or method on an object created by
 * the injector if you feel like injecting some more. This makes it easy to get all
 * frameworky in any corner of your code, build objects via the injector and call
 * their methods with the injector, so that you don't have to pass arguments, just
 * inject.
 * <p>
 * It provides custom scopes, but with only one implementation of a scope that
 * could be used in different ways to get the most common effects. It supports
 * child injectors with scopes that are local to the child injector, that disappear
 * when the child injector is collected.
 * <p>
 * Ilk Inject uses no proxies nor generates any byte code nor disables any
 * ecapsulation via reflection. It is all Java API Java.
 */
package com.goodworkalan.ilk.inject;
