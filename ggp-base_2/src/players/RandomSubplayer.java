package players;

import java.util.List;
import java.util.logging.Logger;
import java.util.Random;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

public class RandomSubplayer extends Subplayer {
	public ThreadPlayerResult threadResult;

	public RandomSubplayer(StateMachine stateMachine, Role role,
			PlayerResult playerResult, MachineState currentState, Logger log,
			ThreadPlayerResult threadResult) {
		super(stateMachine, role, playerResult, currentState, log);
		// TODO Auto-generated constructor stub
		this.threadResult = threadResult;
	}

	@Override
	public void run(){
		List<Move> legalMoves;
		try {
			legalMoves = stateMachine.getLegalMoves(currentState, role);
			Random r = new Random();
			threadResult.chosenMove = legalMoves.get(r.nextInt(legalMoves.size()));
			threadResult.completed = true;
		} catch (MoveDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			threadResult.completed = false;
		} catch (Exception e){
			System.err.println("RandomSubplayer with some error!");
			threadResult.completed = false;
			System.err.println(e.getMessage());
		}
	}
}
