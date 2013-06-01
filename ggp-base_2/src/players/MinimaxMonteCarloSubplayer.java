package players;

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

public class MinimaxMonteCarloSubplayer extends Subplayer {

	private TerminalStateProximity terminalStateProximity;
	private int maxDepth;
	public MinimaxMonteCarloSubplayer(StateMachine stateMachine, Role role,
			PlayerResult playerResult, MachineState currentState, TerminalStateProximity terminalStateProximity, long timeout) {
		super(stateMachine, role, playerResult, currentState, timeout);
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
				for (int i = 0; i < moves.size(); i++) {
					Move move = moves.get(i);
					if (Thread.currentThread().isInterrupted()) return;
					long currentTimeout = System.currentTimeMillis() + ((timeout-System.currentTimeMillis())/((long)moves.size()-i));
					double result = minscore(move, currentState, 1, currentTimeout);
					//System.out.println("MOVE: " + move + ", result: " + result + ", depth: " + maxDepth);
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



	private double minscore(Move move, MachineState state, int currentDepth, long currentTimeout) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		if(Thread.currentThread().isInterrupted()) return Double.POSITIVE_INFINITY;
		double score = Double.POSITIVE_INFINITY;
		List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(state, role, move);
		Collections.shuffle(jointMoves);
		for (int i = 0; i < jointMoves.size(); i++) {
			List<Move> jointMove = jointMoves.get(i);
			MachineState newState = stateMachine.getNextState(state, jointMove);
			long moveTimeout = System.currentTimeMillis() + ((currentTimeout-System.currentTimeMillis())/((long)jointMoves.size()-i));
			double result = maxscore(newState, currentDepth, moveTimeout);
			if (result < score) {
				score = result;
			}
		}
		//memoizedStatesMinValues.put(state,score);
		return score;
	}

	//consider adding a depth parameter (int depth) to only search tree to a certain depth
	// and add condition to base case if (depth == maxDepth)
	private double maxscore(MachineState state, int currentDepth, long currentTimeout) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		if(Thread.currentThread().isInterrupted()) return Double.NEGATIVE_INFINITY;
		if (stateMachine.isTerminal(state) || currentDepth == maxDepth) {
			double goal = Heuristic.getMonteCarlo(stateMachine, state, role, timeout);
			System.out.println(" goal: " + goal);
			return goal;
		}
		if(playerResult.containsMemoizedState(state)){
			//System.out.println("Memoized value!");
			return playerResult.getMemoizedState(state);
		}
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		double score = Double.NEGATIVE_INFINITY;
		Collections.shuffle(moves);
		for (int i = 0; i < moves.size(); i++) {
			Move move = moves.get(i);
			System.out.print("move: " + move);
			long moveTimeout = System.currentTimeMillis() + ((currentTimeout-System.currentTimeMillis())/((long) moves.size()-i));
			System.out.println("moveTimeout: " + moveTimeout + ", systemTime: " + System.currentTimeMillis() + ", ultimate timeout: " + timeout);
			double result = minscore(move, state, currentDepth+1, moveTimeout);
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
