package players;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class Heuristic {
	private Heuristic() {/* static only */}

	/* Constants ?*/

	
	/* Player mobility
	 * Returns the number of legal moves that role has in the specified state
	 * Or +inf if in a winning terminal state
	 * Or -inf if in a losing terminal state 
	 */
	public static double getPlayerMobility(StateMachine stateMachine, MachineState state, Role role) {
		try {
			if (stateMachine.isTerminal(state)) {
				int goal = stateMachine.getGoal(state, role);
				if (stateMachine.getGoal(state,role) == 0 || stateMachine.getGoal(state,role) == 100) {
					return (goal == 100) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
				}
			}
			return stateMachine.getLegalMoves(state, role).size();
		} catch (GoalDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MoveDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0.0;
	}
	
	
	/* 
	 * NMobility
	 */
	private static double normalizeNumber(double num){
		return num;
		//return (1.0 - (1.0 / num));
	}
	
	public static double getNMobilityNormalized(StateMachine stateMachine, MachineState state, Role role, int n, HashSet<MachineState> set){
		return normalizeNumber(getNMobility(stateMachine, state, role, n, set));
	}
	
	public static double getNMobilityNormalized(StateMachine stateMachine, MachineState state, Role role, int n){
		return normalizeNumber(getNMobility(stateMachine, state, role, n, new HashSet<MachineState>()));
	}
	
	/* Return number of states that the current role  can end up in after n turns */
	public static double getNMobility(StateMachine stateMachine, MachineState state, Role role, int n, HashSet<MachineState> reachedStates){
		try {
		if (reachedStates.contains(state)){
			return 0;
		}	
		else if (n <= 0){
			return 0;
		}
		else if (n==1){
			return stateMachine.getLegalMoves(state,role).size();	
		}
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		Map<Move, List<MachineState> > nextStates = stateMachine.getNextStates(state, role);
		int totalNumberOfMoves = 0;
		for (Move move : moves){
			List<MachineState> possibleNextStates = nextStates.get(move);
			for (MachineState nextState: possibleNextStates){
				totalNumberOfMoves += getNMobility(stateMachine, nextState, role, n-1, reachedStates);
			}
		}
		return totalNumberOfMoves;
		
		} catch (MoveDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransitionDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	
	/*
	 * Monte Carlo
	 */
	public static double getMonteCarlo(StateMachine stateMachine, MachineState state, Role role, long timeout) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		if (stateMachine.isTerminal(state)) {
			return stateMachine.getGoal(state, role);
		}
		double sum = 0.0;
		double numTerminalStatesVisited = 0.0;
		System.out.println("timeout: " + timeout + " currTime: " + System.currentTimeMillis());
		for (int i = 0; i < 50; i++){
			int[] theDepth = new int[1];
			MachineState terminal = stateMachine.performDepthCharge(state, theDepth);
			sum += stateMachine.getGoal(terminal, role);
			numTerminalStatesVisited += 1;
		}
		double goal = sum / numTerminalStatesVisited;
		System.out.println("est goal: " + goal);
		return sum / numTerminalStatesVisited;	
	}
}
