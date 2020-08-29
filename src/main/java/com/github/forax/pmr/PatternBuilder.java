package com.github.forax.pmr;

import com.github.forax.pmr.Pattern.BindPattern.Binding;
import com.github.forax.pmr.Pattern.EqualPattern;
import com.github.forax.pmr.Pattern.OrPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class PatternBuilder {
  public static OrBuilder _switch() {
    return new OrBuilder();
  }

  public static Pattern _or(UnaryOperator<OrBuilder> operator) {
    return operator.apply(new OrBuilder()).toPattern();
  }

  public static class OrBuilder {
    private final ArrayList<Pattern> patterns = new ArrayList<>();
    private Pattern _null;
    private Pattern _default;

    public OrBuilder _null(int index) {
      _null = _index(index);
      return this;
    }
    public OrBuilder _case(Pattern pattern) {
      patterns.add(pattern);
      return this;
    }
    public OrBuilder _total(int index) {
      _default = _index(index);
      return this;
    }

    public Pattern toPattern() {
      return new OrPattern(Optional.ofNullable(_null), patterns, Optional.ofNullable(_default));
    }
  }

  public static Pattern _equals(Object value, Pattern pattern) {
    return new EqualPattern(value, pattern);
  }

  public static Pattern _instanceof(Class<?> type, Pattern pattern) {
    return new Pattern.InstanceofPattern(type, Optional.empty(), pattern);
  }

  public static Pattern _index(int index) {
    return new Pattern.IndexPattern((index));
  }

  public static Pattern _destruct(UnaryOperator<BindingBuilder> operator, Pattern pattern) {
    var bindings = operator.apply(new BindingBuilder()).toBindings();
    return new Pattern.BindPattern(bindings, pattern);
  }

  public static class BindingBuilder {
    private final ArrayList<Binding> bindings = new ArrayList<>();

    public BindingBuilder bind(String component, String name) {
      bindings.add(new Binding(component, name));
      return this;
    }

    public List<Binding> toBindings() {
      return bindings;
    }
  }

  public static Pattern _with(String name, Pattern pattern) {
    return new Pattern.WithPattern(name, pattern);
  }

  //public static Pattern _and(Pattern... patterns) {
  //  return new Pattern.AndPattern(List.of(patterns));
  //}
}
