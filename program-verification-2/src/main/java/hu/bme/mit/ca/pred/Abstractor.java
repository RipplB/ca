package hu.bme.mit.ca.pred;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

import hu.bme.mit.ca.pred.CegarChecker.SearchStrategy;
import hu.bme.mit.ca.pred.arg.ArgNode;
import hu.bme.mit.ca.pred.domain.PredDomain;
import hu.bme.mit.ca.pred.domain.PredPrecision;
import hu.bme.mit.ca.pred.domain.PredState;
import hu.bme.mit.ca.pred.waitlist.Waitlist;
import hu.bme.mit.theta.cfa.CFA;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.StmtUtils;
import hu.bme.mit.theta.core.utils.VarIndexing;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;

final class Abstractor {
	private final CFA cfa;
	private final SearchStrategy strategy;
	private final PredDomain domain;

	private Abstractor(final CFA cfa, final SearchStrategy strategy) {
		this.cfa = checkNotNull(cfa);
		this.strategy = checkNotNull(strategy);
		domain = PredDomain.create();
	}

	public static Abstractor create(final CFA cfa, final SearchStrategy strategy) {
		return new Abstractor(cfa, strategy);
	}

	////

	/**
	 * Explores the abstract state space of the CFA model with the given precision (=set of predicates).
	 * Returns whether the result of the exploration is Unsafe or Safe.
	 */
	public AbstractionResult check(final PredPrecision precision) {
		return new AbstractionBuilder(precision).buildAbstraction();
	}

	private final class AbstractionBuilder {
		private final PredPrecision precision;
		private final Collection<ArgNode> reachedSet;
		private final Waitlist waitlist;

		public AbstractionBuilder(final PredPrecision precision) {
			this.precision = checkNotNull(precision);
			reachedSet = new ArrayList<>();
			waitlist = strategy.createWaitlist();
		}

		public AbstractionResult buildAbstraction() {
			// 		Implement the abstract state space exploration here:
			//  	build an ARG using the expand and close methods;
			//		use the waitlist to store and retrieve the non-expanded reached nodes
			//		--> this makes the exploration strategy configurable
			//			(FIFO waitlist => BFS, LIFO waitlist => DFS)
			//
			//		The method should return Unsafe if an abstract state with the error location is found,
			//			with the reached error node provided in it as abstract counterexample.
			//		If no such state is found, the method should return Safe, with the root node
			//			of the ARG given as proof.
			//
			//		Start the exploration from the initial location of the CFA with fully unknown predicate
			//		values (= the top element of the predicate domain)
			ArgNode root = ArgNode.root(cfa.getInitLoc(), PredState.top());
			if (cfa.getErrorLoc().isEmpty()) {
				return AbstractionResult.safe(root);
			}
			CFA.Loc errorLoc = cfa.getErrorLoc().get();
			expand(root);
			while (!waitlist.isEmpty()) {
				ArgNode node = waitlist.remove();
				if (node.getLoc().equals(errorLoc)) {
					return AbstractionResult.unsafe(node);
				}
				close(node);
				if (node.isCovered())
					continue;
				reachedSet.add(node);
				expand(node);
			}
			return AbstractionResult.safe(root);
		}

		private void close(final ArgNode node) {
			// 		Implement cover checking here:
			//  	for each non-covered reached ARG node, check whether it can cover the node given in the argument,
			//		and if one is found, use the coverWith function of ArgNode to set the covering edge
			reachedSet.stream().filter(Predicate.not(ArgNode::isCovered)).forEach(reachedNode -> {
				if (reachedNode.getLoc().equals(node.getLoc()) && domain.isLessAbstractThan(reachedNode.getState(), node.getState())) {
					node.coverWith(reachedNode);
				}
			});
		}

		private void expand(final ArgNode node) {
			final Expr<BoolType> nodeExpression = node.getState().toExpr();
			final PredState nodeState = node.getState();
			node.getLoc().getOutEdges().forEach(edge -> {
				domain.getSuccStates(nodeState, precision, edge).forEach(successorState -> {
					/*Solver solver = Z3SolverFactory.getInstance().createSolver();
					solver.add(nodeExpression);
					solver.add(StmtUtils.toExpr(edge.getStmt(), VarIndexing.all(0)).getExprs());
					solver.add(successorState.toExpr());
					solver.check();
					if (solver.getStatus().isSat()) {
						waitlist.add(node.createChild(edge, successorState));
					}*/
					waitlist.add(node.createChild(edge, successorState));
				});
			});
		}
	}

}
