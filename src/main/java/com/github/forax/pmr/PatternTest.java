package com.github.forax.pmr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static com.github.forax.pmr.PatternBuilder.*;

public class PatternTest {
  @Test
  public void equalCases() {
    var doc = """
        switch(x) {
          case "foo" -> 0;
          case "bar" -> 1;
          total -> 2;
        }
        """;
    var pattern =
        _switch()
            ._case(_equals("foo", _index(0)))
            ._case(_equals("bar", _index(1)))
            ._total(2)
            .toPattern();

    record Carrier(int __index__) {
      Carrier() { this(-1); }
    }

    assertAll(
        () -> assertEquals(new Carrier(0), pattern.match(new Carrier(), "foo").orElseThrow()),
        () -> assertEquals(new Carrier(1), pattern.match(new Carrier(), "bar").orElseThrow()),
        () -> assertEquals(new Carrier(2), pattern.match(new Carrier(), "baz").orElseThrow()),
        () -> assertEquals(new Carrier(2), pattern.match(new Carrier(), null).orElseThrow())
    );
  }


  sealed interface I {}
  record A(int a) implements I {}
  record B(String s) implements I {}

  @Test
  public void sealedCases() {
    var doc = """
        switch(x) {
          case A -> 0;
          case B -> 1;
        }
        """;
    var pattern =
        _switch()
            ._case(_instanceof(A.class, _index(0)))
            ._case(_instanceof(B.class, _index(1)))
            .toPattern();

    record Carrier(int __index__) {
      Carrier() { this(-1); }
    }

    assertAll(
        () -> assertEquals(new Carrier(0), pattern.match(new Carrier(), new A(3)).orElseThrow()),
        () -> assertEquals(new Carrier(1), pattern.match(new Carrier(), new B("foo")).orElseThrow()),
        () -> assertThrows(IncompatibleClassChangeError.class, () -> pattern.match(new Carrier(), "baz")),
        () -> assertThrows(NullPointerException.class, () -> pattern.match(new Carrier(), null))
    );
  }

  record Point(int x, int y) {}

  @Test
  public void destructuring() {
    var doc = """
        switch(x) {
          case Point(int my_x, int my_y) -> 0;
        }
        """;
    var pattern =
        _switch()
            ._case(_instanceof(Point.class,
                _destruct(b -> b.bind("x", "my_x").bind("y",  "my_y"), _index(0))))
            .toPattern();

    record Carrier(int __index__, int my_x, int my_y) {
      Carrier() { this(-1, -1, -1); }
    }

    assertAll(
        () -> assertEquals(new Carrier(0, 3, 4), pattern.match(new Carrier(), new Point(3, 4)).orElseThrow()),
        () -> assertThrows(IncompatibleClassChangeError.class, () -> pattern.match(new Carrier(), "baz")),
        () -> assertThrows(NullPointerException.class, () -> pattern.match(new Carrier(), null))
    );
  }

  sealed interface Shape {}
  record Rect(Point start, Point end) implements Shape {}
  record Circle(Point center, int radius) implements Shape {}
  record Box<T>(T content) {}

  @Test
  public void destructuringNested() {
    var doc = """
        switch(x) {
          case Box(Rect(Point point1, Point point2)) -> 0;
          case Box(Circle(Point point, _ _)) -> 1;
        }
        """;
    var pattern =
        _switch()
            ._case(_instanceof(Box.class,
                _destruct(b -> b.bind("content", "$content"),
                    _with("$content",
                        _or(b -> b
                            ._case(_instanceof(Rect.class,
                                _destruct(b2 -> b2.bind("start", "point1").bind("end", "point2"),
                                    _index(0))))
                            ._case(_instanceof(Circle.class,
                                _destruct(b2 -> b2.bind("center", "point"),
                                    _index(1))))))
                        )))
            .toPattern();

    record Carrier(int __index__, Shape $content, Point point1, Point point2, Point point) {
      Carrier() { this(-1, null, null, null, null); }
    }

    var box1 = new Box<>(new Rect(new Point(1, 2), new Point(3, 4)));
    var box2 = new Box<>(new Circle(new Point(5, 6), 7));
    var box3 = new Box<>(new Circle(null, 8));
    assertAll(
        () -> assertEquals(
            new Carrier(0, box1.content, new Point(1, 2), new Point(3, 4), null),
            pattern.match(new Carrier(), box1).orElseThrow()),
        () -> assertEquals(
            new Carrier(1, box2.content, null, null, new Point(5, 6)),
            pattern.match(new Carrier(), box2).orElseThrow()),
        () -> assertEquals(
            new Carrier(1, box3.content, null, null, null),
            pattern.match(new Carrier(), box3).orElseThrow()),
        () -> assertThrows(NullPointerException.class, () -> pattern.match(new Carrier(), null)),
        () -> assertThrows(NullPointerException.class, () -> pattern.match(new Carrier(), new Box<>(null))),
        () -> assertThrows(IncompatibleClassChangeError.class, () -> pattern.match(new Carrier(), "baz"))
    );
  }

  @Test
  public void deconstructor() {
    record Anonymous(int x, String s) { }
    class NamedPoint {
      int x;
      int y;
      String s;

      NamedPoint(int x, String s) {
        this.x = x;
        this.y = -x;
        this.s = s;
      }

      Anonymous deconstructor() {
        return new Anonymous(x, s);
      }
    }

    var doc = """
        switch(x) {
          case null -> 0;
          case NamedPoint(_ _, "hello") -> 1;
          total -> 2;
        }
        """;
    var pattern =
        _switch()
            ._null(0)
            ._case(_instanceof(NamedPoint.class,
                _destruct(b -> b.bind("s", "s"),
                    _with("s",
                        _equals("hello",
                            _index(1))))))
            ._total(2)
            .toPattern();

    record Carrier(int __index__, String s) {
      Carrier() { this(-1, null); }
    }

    var namedPoint1 = new NamedPoint(2, "hello");
    var namedPoint2 = new NamedPoint(5, "foo");
    assertAll(
        () -> assertEquals(
            new Carrier(1, "hello"),
            pattern.match(new Carrier(), namedPoint1).orElseThrow()),
        () -> assertEquals(
            new Carrier(2, null),
            pattern.match(new Carrier(), namedPoint2).orElseThrow()),
        () -> assertEquals(
            new Carrier(0, null),
            pattern.match(new Carrier(), null).orElseThrow()),
        () -> assertEquals(
            new Carrier(2, null),
            pattern.match(new Carrier(), "baz").orElseThrow())
    );
  }
}