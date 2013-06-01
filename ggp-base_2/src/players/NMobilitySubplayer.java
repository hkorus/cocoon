package players;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

/*** NOT TESTED ***/

public class NMobilitySubplayer extends Subplayer{
	
//	private class Tuple<X,Y>(){
//		public final X x;
//		public final Y y;
//		public Tuple(X x, Y y){
//			this.x = x;
//			this.y = y;
//		}
//	}
	
	
	Map<MachineState,Double> memoizedStatesMinValues = new HashMap<MachineState,Double>();
	Map<MachineState,Double> memoizedStatesMaxValues = new HashMap<MachineState,Double>();

	public NMobilitySubplayer(StateMachine stateMachine, Role role,
			PlayerResult playerResult, MachineState currentState, Logger log) {
		super(stateMachine, role, playerResult, currentState, log);
		
	}
	@Override
	public void run() {
		try {
			int maxDepth = 1;
			List<Move> moves = stateMachine.getLegalMoves(currentState, role);
			double score = Integer.MIN_VALUE;
			Move bestMoveSoFar = null;
			while(true){
				for (Move move : moves) {
					if (Thread.currentThread().isInterrupted()) return;
					double result = minscore(move, currentState, maxDepth, maxDepth-1);
					System.out.println("MOVE: " + move + ", result: " + result);
					if (result > score) {
						System.out.println("Updating to "+move);
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
		if(memoizedStatesMinValues.containsKey(state)){
			//System.out.println(state);
			return memoizedStatesMinValues.get(state);
		}
		
		//if(Thread.currentThread().isInterrupted()) return Double.MAX_VALUE;
		
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
		//memoizedStatesMinValues.put(state,score);
		return score;
	}

	//consider adding a depth parameter (int depth) to only search tree to a certain depth
	// and add condition to base case if (depth == maxDepth)

	private double maxscore(MachineState state, int maxDepth, int currentDepth) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		if(Thread.currentThread().isInterrupted()) return Integer.MAX_VALUE;
		if (currentDepth == maxDepth || stateMachine.isTerminal(state)){
			return evalFn(state,role);
		}
		if(playerResult.containsMemoizedState(state)){
			//System.out.println("Memoized value!");
			return playerResult.getMemoizedState(state);
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
		memoizedStatesMaxValues.put(state,new Double(score));
		
		return score;
	}
	
	private double evalFn(MachineState state, Role role) throws GoalDefinitionException, MoveDefinitionException{
		if (stateMachine.isTerminal(state)){
			return stateMachine.getGoal(state,role);
		}else{
			return Heuristic.getNMobilityNormalized(stateMachine, state, role, 2);
		}
	}
}
