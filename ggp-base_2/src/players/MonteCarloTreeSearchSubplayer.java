package players;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/*** NOT TESTED ***/

public class MonteCarloTreeSearchSubplayer extends Subplayer{

	//	private class Tuple<X,Y>(){
	//		public final X x;
	//		public final Y y;
	//		public Tuple(X x, Y y){
	//			this.x = x;
	//			this.y = y;
	//		}
	//	}

	private Map<MachineState,Double> stateValues = new HashMap<MachineState, Double>();
	private Map<MachineState,Integer> stateVisits = new HashMap<MachineState, Integer>();
	private Map<MachineState,List<MachineState>> stateChildren = new HashMap<MachineState,List<MachineState>>();
	private static Random rand = new Random();
	private static final double epsilon = 1e-6;

	public MonteCarloTreeSearchSubplayer(StateMachine stateMachine, Role role,
			PlayerResult playerResult, MachineState currentState, Logger log) {
		super(stateMachine, role, playerResult, currentState, log);

	}
	@Override
	public void run() {
		try {

			List<MachineState> visited = new LinkedList<MachineState>();
			MachineState state = currentState;
			visited.add(state);
			while(true){
				while(!isLeaf(state)){
					state = select(state);
					visited.add(state);
				}
				expand(state);
				MachineState nextState = select(state);
				visited.add(nextState);
				if(nextState==null) break;
				if(stateMachine.isTerminal(nextState)) break;
				double goalValue = stateMachine.getGoal(stateMachine.performDepthCharge(nextState, null),role);
				for(MachineState visitedState : visited){
					updateStates(visitedState,goalValue);
				}
			}
			
			
			List<Move> legalMoves = stateMachine.getLegalMoves(currentState, role);
			double bestScore = Double.MIN_VALUE;
			Move bestMove = null;
			for (Move move : legalMoves) {
				List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(currentState, role, move);
				double minVal = Double.MAX_VALUE;
				for (List<Move> jointMove : jointMoves) {
					MachineState nextState = stateMachine.getNextState(currentState, jointMove);
					double goal = stateValues.get(nextState);
					if (goal < minVal) {
						minVal = goal;
					}
				}
				if (minVal > bestScore) {
					bestMove = move;
					bestScore = minVal;
				}	
			}
			playerResult.setBestMoveScore(bestScore);
			playerResult.setBestMoveSoFar(bestMove);
			/*  Choose the best child of the current node */

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
	
	/**
	 * Selects unexplored node in the tree
	 * @param state
	 * @return a state that has not yet been explored by the algorithm
	 * @throws MoveDefinitionException
	 * @throws TransitionDefinitionException
	 */

	private MachineState select(MachineState state) throws MoveDefinitionException, TransitionDefinitionException{
		MachineState selected = null;
		double bestValue = Double.MIN_VALUE;
		List<MachineState> nextStates = stateChildren.get(state);
		if(nextStates!=null){
			for(MachineState nextState: nextStates){
				if(!stateVisits.containsKey(nextState)){
					stateVisits.put(nextState, 0);
				}
				if(!stateValues.containsKey(nextState)){
					stateValues.put(nextState,0.0);
				}
				int numVisits = stateVisits.get(nextState);
				double uctValue = stateValues.get(nextState) / (numVisits+epsilon)+
						Math.sqrt(Math.log(numVisits+1) / (numVisits+epsilon)) + 
						rand.nextDouble() * epsilon;
				if(uctValue > bestValue){
					selected = nextState;
					bestValue = uctValue;
				}
			}
		}
		System.out.println("selected state: " + selected);
		return selected;
	}

	private void expand(MachineState state) throws MoveDefinitionException, TransitionDefinitionException{
		stateChildren.put(state, stateMachine.getNextStates(state));
		for(MachineState childState : stateChildren.get(state)){
			stateValues.put(childState,0.0);
			stateVisits.put(childState,0);
		}
	}

	private void updateStates(MachineState state, double goalValue){
		if(!stateVisits.containsKey(state)){
			stateVisits.put(state, 0);
		}
		if(!stateValues.containsKey(state)){
			stateValues.put(state,0.0);
		}
		stateVisits.put(state,stateVisits.get(state)+1);
		stateValues.put(state,stateValues.get(state)+goalValue);
	}

	private boolean isLeaf(MachineState state){
		return stateChildren.get(state)==null;
	}

}
