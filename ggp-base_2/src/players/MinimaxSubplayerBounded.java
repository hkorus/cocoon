package players;

import java.util.logging.Logger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MinimaxSubplayerBounded extends Subplayer {

	public MinimaxSubplayerBounded(StateMachine stateMachine, Role role,
			PlayerResult playerResult, MachineState currentState, Logger log) {
		super(stateMachine, role, playerResult, currentState, log);
		// TODO Auto-generated constructor stub
	}

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
						System.out.println("best move so far: "+ move);
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
		if (playerResult.containsMemoizedState(state)) {
			return playerResult.getMemoizedState(state);
		}
		if (currentDepth == maxDepth || stateMachine.isTerminal(state)) {
			
			
			//return Heuristic.getPlayerMobility(stateMachine, state, role);
		}
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		double score = Double.MIN_VALUE;
		Collections.shuffle(moves);
		for (Move move : moves) {
			double result = minscore(move, state, maxDepth, currentDepth+1);
			if (result > score) {
				score = result;
			}
		}
		playerResult.putMemoizedState(state, score);
		return score;
	}

}
