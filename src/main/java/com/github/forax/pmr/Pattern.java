package com.github.forax.pmr;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.github.forax.pmr.GoryDetails.deconstruct;
import static com.github.forax.pmr.GoryDetails.extract;
import static com.github.forax.pmr.GoryDetails.with;

public sealed interface Pattern {
  Optional<Record> match(Record carrier, Object o);

  record OrPattern(Optional<Pattern> nullPattern, List<Pattern> patterns, Optional<Pattern> defaultPattern) implements Pattern {
    @Override public Optional<Record> match(Record carrier, Object o) {
      // case null
      if (o == null) {
        if (nullPattern.isPresent()) {
          return nullPattern.orElseThrow().match(carrier, null);
        }
        if (defaultPattern.isPresent()) {
          return defaultPattern.orElseThrow().match(carrier, null);
        }
        throw new NullPointerException();
      }

      // try to match each case
      for(var pattern: patterns) {
        var result = pattern.match(carrier, o);
        if (result.isPresent()) {
          return result;
        }
      }

      // default
      if (defaultPattern.isPresent()) {
        return defaultPattern.orElseThrow().match(carrier, o);
      }
      throw new IncompatibleClassChangeError();
    }
  }

  record EqualPattern(Object value, Pattern pattern) implements Pattern {
    @Override public Optional<Record> match(Record carrier, Object o) {
      if (!Objects.equals(o, value)) {
        return Optional.empty();
      }
      return pattern.match(carrier, o);
    }
  }
  record InstanceofPattern(Class<?> type, Optional<String> name, Pattern pattern) implements Pattern {
    @Override public Optional<Record> match(Record carrier, Object o) {
      if (name.isPresent()) {
        carrier = with(carrier, name.orElseThrow(), o);
      }
      return type.isInstance(o)? pattern.match(carrier, o): Optional.empty();
    }
  }
  record BindPattern(List<Binding> bindings, Pattern pattern) implements Pattern {
    record Binding(String component, String name) {}
    @Override public Optional<Record> match(Record carrier, Object o) {
      Record tuple;
      if (o instanceof Record r) {
        tuple = r;
      } else { // need to call the deconstructor
        tuple = deconstruct(o);
      }
      for(var binding: bindings) {
        var value = extract(tuple, binding.component);
        carrier = with(carrier, binding.name, value);
      }

      return pattern.match(carrier, o);
    }
  }
  record WithPattern(String name, Pattern pattern) implements Pattern {
    @Override public Optional<Record> match(Record carrier, Object o) {
      return pattern.match(carrier, extract(carrier, name));
    }
  }
  record IndexPattern(int index) implements Pattern {
    @Override public Optional<Record> match(Record carrier, Object o) {
      return Optional.of(with(carrier, "__index__", index));
    }
  }
//  record AndPattern(List<Pattern> patterns) implements Pattern {
//    @Override public Optional<Record> match(Record record, Object o) {
//      for(var pattern: patterns) {
//        var result = pattern.match(record, o);
//        if (result.isEmpty()) {
//          return Optional.empty();
//        }
//        record = result.orElseThrow();
//      }
//      return Optional.of(record);
//    }
//  }
}
