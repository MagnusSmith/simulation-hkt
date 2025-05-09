package org.higherkindedj.hkt.state;

import java.util.Objects;
import java.util.function.Function;
import org.higherkindedj.hkt.Kind;
import org.higherkindedj.hkt.exception.KindUnwrapException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class StateKindHelper {

  // Error Messages
  public static final String INVALID_KIND_NULL_MSG = "Cannot unwrap null Kind for State";
  public static final String INVALID_KIND_TYPE_MSG = "Kind instance is not a StateHolder: ";
  public static final String INVALID_HOLDER_STATE_MSG = "StateHolder contained null State instance";

  private StateKindHelper() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * Unwraps a StateKind back to the concrete {@code State<S, A>} type. Throws KindUnwrapException
   * if the Kind is null or not a valid StateHolder.
   */
  @SuppressWarnings("unchecked")
  public static <S, A> @NonNull State<S, A> unwrap(@Nullable Kind<StateKind<S, ?>, A> kind) {

    return switch (kind) {
      // Case 1: Input Kind is null
      case null -> throw new KindUnwrapException(StateKindHelper.INVALID_KIND_NULL_MSG);

      // Case 2: Input Kind is a StateHolder (record pattern extracts non-null stateInstance)
      // The @NonNull contract on StateHolder.stateInstance guarantees stateInstance is not null
      // here.
      case StateKindHelper.StateHolder<?, ?>(var stateInstance) ->
          // Cast is safe because pattern matched and stateInstance is known non-null.
          (State<S, A>) stateInstance;

      // Case 3: Input Kind is non-null but not a StateHolder
      default ->
          throw new KindUnwrapException(
              StateKindHelper.INVALID_KIND_TYPE_MSG + kind.getClass().getName());
    };
  }

  /**
   * Wraps a concrete {@code State<S, A>} value into the StateKind simulation type. Requires a
   * non-null State instance as input.
   */
  public static <S, A> @NonNull StateKind<S, A> wrap(@NonNull State<S, A> state) {
    Objects.requireNonNull(state, "Input State cannot be null for wrap");
    return new StateHolder<>(state);
  }

  /**
   * Creates a StateKind that returns the given value and leaves the state unchanged. Equivalent to
   * State.pure(value) wrapped in Kind.
   */
  public static <S, A> @NonNull StateKind<S, A> pure(@Nullable A value) {
    return wrap(State.pure(value));
  }

  /** Creates a StateKind that returns the current state as the value. */
  public static <S> @NonNull StateKind<S, S> get() {
    return wrap(State.get());
  }

  /** Creates a StateKind that replaces the state and returns Void. */
  public static <S> @NonNull StateKind<S, Void> set(@NonNull S newState) {
    return wrap(State.set(newState));
  }

  /** Creates a StateKind that modifies the state using a function and returns Void. */
  public static <S> @NonNull StateKind<S, Void> modify(
      @NonNull Function<@NonNull S, @NonNull S> f) {
    return wrap(State.modify(f));
  }

  /** Creates a StateKind that inspects the state using a function, returning the result. */
  public static <S, A> @NonNull StateKind<S, A> inspect(
      @NonNull Function<@NonNull S, @Nullable A> f) {
    return wrap(State.inspect(f));
  }

  /** Runs the State computation within the Kind with the given initial state. */
  public static <S, A> State.@NonNull StateTuple<S, A> runState(
      @NonNull Kind<StateKind<S, ?>, A> kind, @NonNull S initialState) {
    return unwrap(kind).run(initialState);
  }

  /** Evaluates the State computation, returning only the final value. */
  public static <S, A> @Nullable A evalState(
      @NonNull Kind<StateKind<S, ?>, A> kind, @NonNull S initialState) {
    return runState(kind, initialState).value();
  }

  /** Executes the State computation, returning only the final state. */
  public static <S, A> @NonNull S execState(
      @NonNull Kind<StateKind<S, ?>, A> kind, @NonNull S initialState) {
    return runState(kind, initialState).state();
  }

  // Internal holder record implementing the Kind interface
  record StateHolder<S, A>(@NonNull State<S, A> stateInstance) implements StateKind<S, A> {}
}
