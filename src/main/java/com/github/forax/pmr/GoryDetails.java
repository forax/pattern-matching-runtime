package com.github.forax.pmr;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;

class GoryDetails {
  private GoryDetails() {
    throw new AssertionError();
  }

  static Record deconstruct(Object value) {
    var deconstructor = DECONSTRUCTORS.get(value.getClass());
    return invokeDeconstructor(value, deconstructor);
  }

  static Object extract(Record tuple, String name) {
    var cache = CACHE.get(tuple.getClass());
    var accessor = cache.accessor(name);
    return invokeAccessor(tuple, accessor);
  }

  static Record with(Record record, String name, Object value) {
    var type = record.getClass();
    var cache = CACHE.get(type);
    if (!cache.accessorMap.containsKey(name)) {
      throw new IncompatibleClassChangeError("no component " + name + " for record " + type.getName());
    }
    var accessorMap = cache.accessorMap;
    var values = new Object[accessorMap.size()];
    var i = 0;
    for(var entry: accessorMap.entrySet()) {
      var key = entry.getKey();
      if (key.equals(name)) {
        values[i++] = value;
      } else {
        var accessor = entry.getValue();
        values[i++] = invokeAccessor(record, accessor);
      }
    }
    return invokeConstructor(values, cache.constructor);
  }

  private static Record invokeDeconstructor(Object value, Method destructor) {
    try {
      return (Record) destructor.invoke(value);
    } catch (IllegalAccessException e) {
      throw (IncompatibleClassChangeError) new IncompatibleClassChangeError(
          "no deconstructor for class " + destructor.getDeclaringClass().getName()).initCause(e);
    } catch (InvocationTargetException e) {
      var cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw (IncompatibleClassChangeError) new IncompatibleClassChangeError(
          "the deconstructor of class " + destructor.getDeclaringClass().getName() + " throws a checked exception").initCause(e);
    }
  }

  private static Object invokeAccessor(Record record, Method accessor) {
    try {
      return accessor.invoke(record);
    } catch (IllegalAccessException e) {
      throw (IncompatibleClassChangeError) new IncompatibleClassChangeError(
          "no accessor for component " + accessor.getName() + " of record " + accessor.getDeclaringClass().getName()).initCause(e);
    } catch (InvocationTargetException e) {
      var cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw (IncompatibleClassChangeError) new IncompatibleClassChangeError(
          "accessor for component " + accessor.getName() + " of record " + accessor.getDeclaringClass().getName() + " throws a checked exception").initCause(e);
    }
  }

  private static Record invokeConstructor(Object[] values, Constructor<?> constructor) {
    try {
      return (Record) constructor.newInstance(values);
    } catch (InstantiationException | IllegalAccessException e) {
      throw (IncompatibleClassChangeError) new IncompatibleClassChangeError(
          "invalid constructor " + constructor).initCause(e);
    } catch (InvocationTargetException e) {
      var cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw (IncompatibleClassChangeError) new IncompatibleClassChangeError(
          "constructor " + constructor + " throws a checked exception").initCause(e);
    }
  }

  private static final ClassValue<Method> DECONSTRUCTORS = new ClassValue<>() {
    @Override protected Method computeValue(Class<?> type) {
      try {
        return type.getDeclaredMethod("deconstructor");
      } catch (NoSuchMethodException e) {
        throw (IncompatibleClassChangeError) new IncompatibleClassChangeError(
            "no deconstructor for " + type.getName()).initCause(e);
      }
    }
  };

  record Cache(Constructor<?> constructor, LinkedHashMap<String, Method> accessorMap) {
    Method accessor(String name) {
      var method = accessorMap.get(name);
      if (method == null) {
        throw new IncompatibleClassChangeError("no component " + name + " for record " + constructor.getDeclaringClass().getName());
      }
      return method;
    }
  }
  private static final ClassValue<Cache> CACHE = new ClassValue<>() {
    @Override protected Cache computeValue(Class<?> type) {
      var accessorMap = new LinkedHashMap<String, Method>();
      var components = type.getRecordComponents();
      if (components == null) {
        throw new IncompatibleClassChangeError(type.getName() + " is not a record");
      }
      var types = new Class<?>[components.length];
      for (var i = 0; i < components.length; i++) {
        var component = components[i];
        types[i] = component.getType();
        accessorMap.put(component.getName(), component.getAccessor());
      }
      Constructor<?> constructor;
      try {
        constructor = type.getDeclaredConstructor(types);
      } catch (NoSuchMethodException e) {
        throw (NoSuchMethodError) new NoSuchMethodError("invalid canonical constructor for " + type.getName()).initCause(e);
      }
      return new Cache(constructor, accessorMap);
    }
  };
}
