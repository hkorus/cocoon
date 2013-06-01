package players;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class WinCheck extends Subplayer {

	public WinCheck(StateMachine stateMachine, Role role,
			PlayerResult playerResult, MachineState currentState, Logger log) {
		super(stateMachine, role, playerResult, currentState, log);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		try {
			List<Move> moves = stateMachine.getLegalMoves(currentState, role);

			for (Move move: moves){
				List<List<Move>> legalJointMoves = stateMachine.getLegalJointMoves(currentState, role, move);
				boolean sureMoveFound = true;
				Move sureMove = null;
				for (List<Move> jointMove: legalJointMoves){
					MachineState nextState = stateMachine.getNextState(currentState, jointMove);
					
					if (stateMachine.isTerminal(nextState)){
						System.out.println("Found terminal state: "+ nextState);
						System.out.println("It results in goal of " + stateMachine.getGoal(nextState, role));
						if (stateMachine.getGoal(nextState, role) == 100){
							//found the right move!
							// not returning right away because need to check
							// all of the possible joint moves.
							sureMove = move;
						}else {
							sureMoveFound = false;
						}
					}else{
						sureMoveFound = false;
					}
				}
				if (sureMoveFound){
					playerResult.sureMove = sureMove;
					return;
				}
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


}