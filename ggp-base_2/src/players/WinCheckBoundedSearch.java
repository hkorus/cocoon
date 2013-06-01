package players;

import java.util.ArrayList;
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

public class WinCheckBoundedSearch extends Subplayer {
	public static int NOTFOUND = -1000;
	public WinCheckBoundedSearch(StateMachine stateMachine, Role role,
			PlayerResult playerResult, MachineState currentState, Logger log) {
		super(stateMachine, role, playerResult, currentState, log);
		// TODO Auto-generated constructor stub
	}

	private class MoveResult{
		int terminalScore = 0;
		boolean reachedFinal = false;

		MoveResult copy(){
			MoveResult newSummary = new MoveResult();
			newSummary.terminalScore = this.terminalScore;
			newSummary.reachedFinal = this.reachedFinal;
			return newSummary;
		}

		public String toString(){
			String str = "" + terminalScore + " " + reachedFinal + ":";
			return str;
		}
	}

	private boolean gameSolved_temp;

	@Override
	public void run() {
		try {
			List<Move> moves = stateMachine.getLegalMoves(currentState, role);
			double score = -1;
			Move bestMoveSoFar = null;
			int maxDepth = 1;
			playerResult.gameSolved = false;
			while(true){
				gameSolved_temp=true;
				for (Move move : moves) {
					if (Thread.currentThread().isInterrupted()) return;
					double result = guarMinScore(move, currentState, maxDepth, 1);
					if (result > score) {
						score = result;
						System.out.println("best move so far: "+ move+ "(" + score+")");
						bestMoveSoFar = move;
						playerResult.setSureMove(bestMoveSoFar);
						playerResult.setSureScore(score);
					}
				}
				if (gameSolved_temp){
					playerResult.setGameSolved(true);
					return;
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


	/* explores all possible joint moves for current move and returns the worst outcome.
	 * In two player games this is either a player moves or noops. */

	private double guarMinScore(Move move, MachineState state, int maxDepth, int currentDepth) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
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
		return score;
	}

	/* 
	 * Maximize over our potential moves
	 */
	private double maxscore(MachineState state, int maxDepth, int currentDepth) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		//if (playerResult.containsMemoizedState(state)) {
		//	return playerResult.getMemoizedState(state);
		//}
		if (stateMachine.isTerminal(state)){
			return stateMachine.getGoal(state, role);
		}

		if (currentDepth == maxDepth) {
			gameSolved_temp = false;
			return 30;
			//return Heuristic.getPlayerMobility(stateMachine, state, role);
		}
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		double score = -1;
		Collections.shuffle(moves);
		for (Move move : moves) {
			double result = guarMinScore(move, state, maxDepth, currentDepth+1);

			if (result > score) {
				score = result;
			}
		}
		//playerResult.putMemoizedState(state, score);
		return score;
	}

}
