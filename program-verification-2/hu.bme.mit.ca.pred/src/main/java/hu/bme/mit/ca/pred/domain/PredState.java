package hu.bme.mit.ca.pred.domain;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.common.Utils.singleElementOf;
import static hu.bme.mit.theta.core.expr.impl.Exprs.And;
import static hu.bme.mit.theta.core.expr.impl.Exprs.True;

import java.util.Collection;

import com.google.common.collect.ImmutableSet;

import hu.bme.mit.theta.common.ObjectUtils;
import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.type.BoolType;

public final class PredState {
	private static final int HASH_SEED = 7621;
	private static final PredState TOP = PredState.of(ImmutableSet.of());

	private final Collection<Expr<? extends BoolType>> predicates;

	private volatile Expr<? extends BoolType> expr;
	private volatile int hashCode = 0;

	private PredState(final Collection<? extends Expr<? extends BoolType>> predicates) {
		checkNotNull(predicates);
		this.predicates = ImmutableSet.copyOf(predicates);
		expr = convertToExpr(predicates);
	}

	private static Expr<? extends BoolType> convertToExpr(
			final Collection<? extends Expr<? extends BoolType>> predicates) {
		if (predicates.size() == 0) {
			return True();
		} else if (predicates.size() == 1) {
			return singleElementOf(predicates);
		} else {
			return And(predicates);
		}
	}

	public static PredState of(final Collection<? extends Expr<? extends BoolType>> predicates) {
		return new PredState(predicates);
	}

	public static PredState top() {
		return TOP;
	}

	////

	public Collection<Expr<? extends BoolType>> getPredicates() {
		return predicates;
	}

	public Expr<? extends BoolType> toExpr() {
		Expr<? extends BoolType> result = expr;
		if (expr == null) {
			if (predicates.size() == 0) {
				result = True();
			} else if (predicates.size() == 1) {
				result = singleElementOf(predicates);
			} else {
				result = And(predicates);
			}
			expr = result;
		}
		return expr;
	}

	////

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = HASH_SEED;
			result = 31 * result + predicates.hashCode();
			hashCode = result;
		}
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof PredState) {
			final PredState that = (PredState) obj;
			return this.predicates.equals(that.predicates);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return ObjectUtils.toStringBuilder(getClass().getSimpleName()).addAll(predicates).toString();
	}

}
