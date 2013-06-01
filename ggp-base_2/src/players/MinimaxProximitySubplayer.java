package players;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.ggp.base.player.gamer.statemachine.PlatypusPlayer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import platypus.logging.PlatypusLogger;

public class MinimaxProximitySubplayer extends Subplayer {

	private TerminalStateProximity terminalStateProximity;
	private int maxDepth;
	public MinimaxProximitySubplayer(StateMachine stateMachine, Role role,
			PlayerResult playerResult, MachineState currentState, TerminalStateProximity terminalStateProximity, int maxDepth, Logger log) {
		super(stateMachine, role, playerResult, currentState, log);

		this.terminalStateProximity = terminalStateProximity;
	}

	@Override
	public void run() {
		try {

			List<Move> moves = stateMachine.getLegalMoves(currentState, role);
			double score = Double.NEGATIVE_INFINITY;
			Move bestMoveSoFar = null;
			maxDepth = 1;
			while(true){
				for (Move move : moves) {
					if (Thread.currentThread().isInterrupted()) return;
					double result = minscore(move, currentState, 1);
					System.out.println("MOVE: " + move + ", result: " + result + ", depth: " + maxDepth);
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



	private double minscore(Move move, MachineState state, int currentDepth) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		if(Thread.currentThread().isInterrupted()) return Double.POSITIVE_INFINITY;
		double score = Double.POSITIVE_INFINITY;
		List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(state, role, move);
		Collections.shuffle(jointMoves);
		for (int i = 0; i < jointMoves.size(); i++) {
			List<Move> jointMove = jointMoves.get(i);
			MachineState newState = stateMachine.getNextState(state, jointMove);
			double result = maxscore(newState, currentDepth);
			if (result < score) {
				score = result;
			}
		}
		//memoizedStatesMinValues.put(state,score);
		return score;
	}

	//consider adding a depth parameter (int depth) to only search tree to a certain depth
	// and add condition to base case if (depth == maxDepth)
	private double maxscore(MachineState state, int currentDepth) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		if(Thread.currentThread().isInterrupted()) return Double.NEGATIVE_INFINITY;
		if (stateMachine.isTerminal(state) || currentDepth == maxDepth) {
			double stateHeuristic = terminalStateProximity.evaluateState(state);
			return stateHeuristic;
		}
		if(playerResult.containsMemoizedState(state)){
			//System.out.println("Memoized value!");
			return playerResult.getMemoizedState(state);
		}
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		double score = Double.NEGATIVE_INFINITY;
		Collections.shuffle(moves);
		for (Move move : moves) {
			double result = minscore(move, state, currentDepth+1);
			if (result > score) {
				score = result;
			}
		}
		if(!Thread.currentThread().isInterrupted()){
			playerResult.putMemoizedState(state,score);
		}
		return score;
	}

}
