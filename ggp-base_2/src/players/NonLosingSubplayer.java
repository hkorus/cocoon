package players;

import java.util.List;
import java.util.logging.Logger;
import java.util.Random;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class NonLosingSubplayer extends Subplayer {
	public ThreadPlayerResult threadResult;
	private int NUM_PLAYERS;
	public NonLosingSubplayer(StateMachine stateMachine, Role role,
			PlayerResult playerResult, MachineState currentState, Logger log,
			ThreadPlayerResult threadResult) {
		super(stateMachine, role, playerResult, currentState, log);
		// TODO Auto-generated constructor stub
		this.threadResult = threadResult;
	}

	@Override
	public void run(){
		List<Move> allLegalMoves;
		try {
			allLegalMoves = stateMachine.getLegalMoves(currentState, role);
		} catch (MoveDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		NUM_PLAYERS=stateMachine.getRoles().size();
		Move bestMove = allLegalMoves.get(new Random().nextInt(allLegalMoves.size()));
		for(Move move : allLegalMoves){
			boolean moveIsBad = false;
			try {
				for(List<Move> allJointMoves : stateMachine.getLegalJointMoves(currentState,role,move)){
					if(isSuicidal(currentState,move,stateMachine.getNextState(currentState,allJointMoves))){
						moveIsBad= true; break;
					}
				}
			} catch (MoveDefinitionException e) {
				threadResult.completed = false;
				e.printStackTrace();
			} catch (TransitionDefinitionException e) {
				threadResult.completed = false;
				e.printStackTrace();
			} catch (GoalDefinitionException e) {
				threadResult.completed = false;
				e.printStackTrace();
			}
			if(!moveIsBad) bestMove = move;
		}
		threadResult.chosenMove = bestMove;
	}
	
	private boolean isSuicidal(MachineState currentState, Move move, MachineState acquiredState) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if(NUM_PLAYERS==1) return false;
		
			if (!stateMachine.isTerminal(acquiredState)) {
				List<Move> legalMoves = stateMachine.getLegalMoves(acquiredState, role);
				for (Move nextMove : legalMoves) {
					List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(acquiredState, role, nextMove);

					for(List<Move> jointMove : jointMoves){
						MachineState nextState = stateMachine.getNextState(acquiredState, jointMove);

						if ((stateMachine.isTerminal(nextState) && stateMachine.getGoal(nextState,role)==0)) {
							System.out.println("Decided that move " + move + " was SUICIDAL");
							return true;
						}
					}
				}

			}
	

		return false;
	}
}
