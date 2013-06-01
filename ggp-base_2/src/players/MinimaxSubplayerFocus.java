package players;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MinimaxSubplayerFocus extends Subplayer {


	public MinimaxSubplayerFocus(StateMachine stateMachine, Role role,
			PlayerResult playerResult, MachineState currentState, Logger log) {
		super(stateMachine, role, playerResult, currentState, log);
			// TODO Auto-generated constructor stub
	}



	Map<MachineState,Double> memoizedStatesMinValues = new HashMap<MachineState,Double>();
	Map<MachineState,Double> memoizedStatesMaxValues = new HashMap<MachineState,Double>();

	@Override
	public void run() {
		try {
			List<Move> moves = stateMachine.getLegalMoves(currentState, role);
			double score = Double.MIN_VALUE;
			Move bestMoveSoFar = null;
			int maxDepth = 1;
			while (true) {
				for (Move move : moves) {
					if (Thread.currentThread().isInterrupted()) return;
					double result = minscore(move, currentState, maxDepth, maxDepth-1);
					System.out.println("MOVE: " + move + ", result: " + result);
					if (result > score) {
						score = result;
						bestMoveSoFar = move;
						playerResult.setBestMoveSoFar(bestMoveSoFar);
						playerResult.setBestMoveScore(score);
					}
					//parentThread.interrupt();
				}
				maxDepth++;
			}

		} catch (MoveDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GoalDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransitionDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	private double minscore(Move move, MachineState state, int maxDepth, int currentDepth) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		double score = Double.MAX_VALUE;
		List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(state, role, move);
		Collections.shuffle(jointMoves);
		for (int i = 0; i < jointMoves.size(); i++) {
			List<Move> jointMove = jointMoves.get(i);
			MachineState newState = stateMachine.getNextState(state, jointMove);
			double result = maxscore(newState, maxDepth, currentDepth);
			if (result < score) {
				score = result;
			}
		}
		return score;
	}

	private double maxscore(MachineState state, int maxDepth, int currentDepth) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		if (currentDepth == maxDepth || stateMachine.isTerminal(state)) {
			return evalFn(state, role);
		}
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		double score = Integer.MIN_VALUE;
		Collections.shuffle(moves);
		for (Move move : moves) {
			double result = minscore(move, state, maxDepth, currentDepth+1);
			if (result > score) {
				score = result;
			}
		}
		memoizedStatesMaxValues.put(state, score);
		return score;
	}
	
	

	//opponent focus heuristic
	//the more opponent moves, the lower the score
	private double evalFn(MachineState state, Role role) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (memoizedStatesMaxValues.containsKey(state)) {
			return memoizedStatesMaxValues.get(state);
		}
		List<List<Move>> allMoves = stateMachine.getLegalJointMoves(state);
		int numOpponentMoves = 0;
		for (int i = 0; i < allMoves.size(); i++) {
			if (i != stateMachine.getRoleIndices().get(role)) {
				numOpponentMoves += allMoves.get(i).size();
			}
		}
		return (1.0/numOpponentMoves);
	}
}
