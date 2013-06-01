package players;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

public class InfiniteRandomPlayer extends Subplayer{

	public InfiniteRandomPlayer(StateMachine stateMachine, Role role,
			PlayerResult playerResult, MachineState currentState, Logger log) {
		super(stateMachine, role, playerResult, currentState, log);
	}

	public void run() {
		List<Move> moves;
		try {
			moves = stateMachine.getLegalMoves(currentState, role);
			/* Keeps generating random moves to be the "best" move until it is told that it is done */
			while(!Thread.currentThread().isInterrupted()){
				playerResult.setBestMoveSoFar(moves.get(new Random().nextInt(moves.size())));
			}
		} catch (MoveDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}		
}