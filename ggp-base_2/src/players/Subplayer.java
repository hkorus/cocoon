package players;

import java.util.logging.Logger;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;

public abstract class Subplayer implements Runnable{
	StateMachine stateMachine;
	Role role;
	PlayerResult playerResult;
	MachineState currentState;
	Logger log;
	long timeout;
	
	public Subplayer(StateMachine stateMachine, Role role,
			PlayerResult playerResult, MachineState currentState, Logger log) {
		this.stateMachine = stateMachine;
		this.role = role;
		this.playerResult = playerResult;
		this.currentState = currentState;
		this.log = log;
	}
	
	public Subplayer(StateMachine stateMachine, Role role,
			PlayerResult playerResult, MachineState currentState, long timeout) {
		this.stateMachine = stateMachine;
		this.role = role;
		this.playerResult = playerResult;
		this.currentState = currentState;
		this.timeout = timeout;
	}
}
